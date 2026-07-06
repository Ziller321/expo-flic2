package expo.modules.flic2

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import io.flic.flic2libandroid.BatteryLevel
import java.util.concurrent.ConcurrentHashMap

class ExpoFlic2Module : Module() {

  private var manager: Flic2Manager? = null
  private val triggerModes = ConcurrentHashMap<String, String>()
  private val buttonListeners = ConcurrentHashMap<String, Flic2ButtonListener>()
  // Correlation between button clock and Android clock, established at onReady.
  // Maps uuid -> Pair(androidReadyElapsedMs, buttonReadyTimestampMs)
  private val readyCorrelations = ConcurrentHashMap<String, Pair<Long, Long>>()

  override fun definition() = ModuleDefinition {
    Name("ExpoFlic2")

    Events(
      "onFlic2Click",
      "onFlic2DoubleClick",
      "onFlic2Hold",
      "onFlic2UpOrDown",
      "onFlic2Connection",
      "onFlic2Scan",
      "onFlic2Battery",
      "onFlic2ManagerState"
    )

    OnDestroy {
      manager?.getButtons()?.forEach { button ->
        buttonListeners[button.uuid]?.let { button.removeListener(it) }
      }
      buttonListeners.clear()
      triggerModes.clear()
      readyCorrelations.clear()
    }

    Function("initialize") {
      val context = appContext.reactContext ?: return@Function null
      manager = Flic2Manager.initAndGetInstance(context, Handler(Looper.getMainLooper()))
      loadPersistedTriggerModes()
      manager?.getButtons()?.forEach { button ->
        attachListener(button)
      }
      null
    }

    Function("startScan") {
      val currentManager = manager
      if (currentManager == null) {
        sendEvent("onFlic2Scan", mapOf(
          "isScanning" to false,
          "error" to "Flic2 manager is not initialized. Call initialize() first."
        ))
        return@Function
      }
      try {
        startScanInternal(currentManager)
      } catch (e: SecurityException) {
        sendEvent("onFlic2Scan", mapOf(
          "isScanning" to false,
          "error" to "Missing runtime permission: ${e.message}"
        ))
      }
    }

    Function("stopScan") {
      manager?.stopScan()
    }

    Function("getButtons") {
      manager?.getButtons()?.map { it.toRecord(triggerModes[it.uuid] ?: "clickAndDoubleClickAndHold") } ?: emptyList<Map<String, Any?>>()
    }

    Function("connectButton") { uuid: String ->
      findButton(uuid)?.connect()
    }

    Function("disconnectButton") { uuid: String ->
      findButton(uuid)?.disconnectOrAbortPendingConnection()
    }

    Function("forgetButton") { uuid: String ->
      val button = findButton(uuid)
      if (button != null) {
        buttonListeners.remove(uuid)?.let { button.removeListener(it) }
        removeButtonState(uuid)
        manager?.forgetButton(button)
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to uuid,
          "state" to "unpaired"
        ))
      }
    }

    Function("setButtonTriggerMode") { uuid: String, mode: String ->
      triggerModes[uuid] = mode
      triggerModePrefs()?.edit()?.putString(uuid, mode)?.apply()
    }
  }

  private fun startScanInternal(manager: Flic2Manager) {
    manager.startScan(object : Flic2ScanCallback {
      override fun onDiscoveredAlreadyPairedButton(button: Flic2Button) {
        sendEvent("onFlic2Scan", mapOf(
          "isScanning" to true,
          "button" to button.toRecord(triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold")
        ))
      }

      override fun onDiscovered(bdAddr: String) {
        sendEvent("onFlic2Scan", mapOf(
          "isScanning" to true
        ))
      }

      override fun onConnected() {
        sendEvent("onFlic2Scan", mapOf("isScanning" to true))
      }

      override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {
        if (button != null) {
          attachListener(button)
          sendEvent("onFlic2Scan", mapOf(
            "isScanning" to false,
            "button" to button.toRecord(triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold")
          ))
        } else {
          sendEvent("onFlic2Scan", mapOf(
            "isScanning" to false,
            "error" to "Scan failed with result $result (subCode: $subCode)"
          ))
        }
      }
    })
  }

  private fun triggerModePrefs(): SharedPreferences? {
    return appContext.reactContext?.getSharedPreferences("expo-flic2.triggerModes", Context.MODE_PRIVATE)
  }

  private fun loadPersistedTriggerModes() {
    triggerModePrefs()?.all?.forEach { (uuid, mode) ->
      if (mode is String) {
        triggerModes[uuid] = mode
      }
    }
  }

  private fun removeButtonState(uuid: String) {
    triggerModes.remove(uuid)
    readyCorrelations.remove(uuid)
    triggerModePrefs()?.edit()?.remove(uuid)?.apply()
  }

  private fun ageMs(uuid: String, eventTimestamp: Long): Long {
    val (androidReadyMs, buttonReadyMs) = readyCorrelations[uuid] ?: return 0L
    return AgeCalculator.computeAgeMs(SystemClock.elapsedRealtime(), androidReadyMs, buttonReadyMs, eventTimestamp)
  }

  private fun findButton(uuid: String): Flic2Button? {
    return manager?.getButtons()?.find { it.uuid == uuid }
  }

  private fun attachListener(button: Flic2Button) {
    buttonListeners[button.uuid]?.let { button.removeListener(it) }
    val listener = createButtonListener()
    buttonListeners[button.uuid] = listener
    button.addListener(listener)
  }

  private fun createButtonListener(): Flic2ButtonListener {
    return object : Flic2ButtonListener() {
      // Each trigger mode is served by exactly one SDK callback, mirroring the
      // iOS FLICButtonTriggerMode semantics:
      //   click                      -> onButtonUpOrDown (click fires on button down)
      //   clickAndHold               -> onButtonClickOrHold
      //   clickAndDoubleClick        -> onButtonSingleOrDoubleClick
      //   clickAndDoubleClickAndHold -> onButtonSingleOrDoubleClickOrHold
      override fun onButtonClickOrHold(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isClick: Boolean,
        isHold: Boolean
      ) {
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        if (mode != "clickAndHold") return
        val age = ageMs(button.uuid, timestamp)
        if (isClick) {
          sendEvent("onFlic2Click", mapOf("uuid" to button.uuid, "queued" to wasQueued, "age" to age))
        }
        if (isHold) {
          sendEvent("onFlic2Hold", mapOf("uuid" to button.uuid, "queued" to wasQueued, "age" to age))
        }
      }

      override fun onButtonSingleOrDoubleClick(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isSingleClick: Boolean,
        isDoubleClick: Boolean
      ) {
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        if (mode != "clickAndDoubleClick") return
        val age = ageMs(button.uuid, timestamp)
        if (isSingleClick) {
          sendEvent("onFlic2Click", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
        if (isDoubleClick) {
          sendEvent("onFlic2DoubleClick", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
      }

      override fun onButtonSingleOrDoubleClickOrHold(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isSingleClick: Boolean,
        isDoubleClick: Boolean,
        isHold: Boolean
      ) {
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        if (mode != "clickAndDoubleClickAndHold") return
        val age = ageMs(button.uuid, timestamp)
        if (isSingleClick) {
          sendEvent("onFlic2Click", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
        if (isDoubleClick) {
          sendEvent("onFlic2DoubleClick", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
        if (isHold) {
          sendEvent("onFlic2Hold", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
      }

      override fun onButtonUpOrDown(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isUp: Boolean,
        isDown: Boolean
      ) {
        val age = ageMs(button.uuid, timestamp)
        sendEvent("onFlic2UpOrDown", mapOf(
          "uuid" to button.uuid,
          "isDown" to isDown,
          "queued" to wasQueued,
          "age" to age
        ))
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        if (mode == "click" && isDown) {
          sendEvent("onFlic2Click", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
      }

      override fun onConnect(button: Flic2Button) {
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "connected"
        ))
      }

      override fun onReady(button: Flic2Button, timestamp: Long) {
        readyCorrelations[button.uuid] = Pair(SystemClock.elapsedRealtime(), timestamp)
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "ready"
        ))
      }

      override fun onDisconnect(button: Flic2Button) {
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "disconnected"
        ))
      }

      override fun onUnpaired(button: Flic2Button) {
        // The SDK has already removed the button from the manager.
        buttonListeners.remove(button.uuid)
        removeButtonState(button.uuid)
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "unpaired"
        ))
      }

      override fun onFailure(
        button: Flic2Button,
        errorCode: Int,
        subCode: Int
      ) {
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "disconnected",
          "error" to "Connection failed: reason=$errorCode subCode=$subCode"
        ))
      }

      override fun onBatteryLevelUpdated(button: Flic2Button, level: BatteryLevel) {
        sendEvent("onFlic2Battery", mapOf(
          "uuid" to button.uuid,
          "level" to level.estimatedPercentage
        ))
      }
    }
  }
}
