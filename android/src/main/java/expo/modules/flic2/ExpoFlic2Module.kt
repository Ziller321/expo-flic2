package expo.modules.flic2

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
  private val buttonListeners = mutableMapOf<String, Flic2ButtonListener>()
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
      manager?.getButtons()?.forEach { button ->
        attachListener(button)
      }
      null
    }

    Function("startScan") {
      manager?.startScan(object : Flic2ScanCallback {
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
        triggerModes.remove(uuid)
        manager?.forgetButton(button)
      }
    }

    Function("setButtonTriggerMode") { uuid: String, mode: String ->
      triggerModes[uuid] = mode
    }
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
      override fun onButtonClickOrHold(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isClick: Boolean,
        isHold: Boolean
      ) {
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        if (mode != "click" && mode != "clickAndHold") return
        val age = ageMs(button.uuid, timestamp)
        if (isClick) {
          sendEvent("onFlic2Click", mapOf("uuid" to button.uuid, "queued" to wasQueued, "age" to age))
        }
        if (isHold && mode == "clickAndHold") {
          sendEvent("onFlic2Hold", mapOf("uuid" to button.uuid, "queued" to wasQueued, "age" to age))
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
        if (mode == "click" || mode == "clickAndHold") return
        val age = ageMs(button.uuid, timestamp)
        val emitClick = isSingleClick && mode != "clickAndDoubleClick"
        val emitDoubleClick = isDoubleClick
        val emitHold = isHold && mode != "clickAndDoubleClick"
        if (emitClick) {
          sendEvent("onFlic2Click", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
        if (emitDoubleClick) {
          sendEvent("onFlic2DoubleClick", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to age
          ))
        }
        if (emitHold) {
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
        sendEvent("onFlic2UpOrDown", mapOf(
          "uuid" to button.uuid,
          "isDown" to isDown,
          "queued" to wasQueued,
          "age" to ageMs(button.uuid, timestamp)
        ))
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
