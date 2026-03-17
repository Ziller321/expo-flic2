package expo.modules.flic2

import android.os.Handler
import android.os.Looper
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import io.flic.flic2libandroid.BatteryLevel

class ExpoFlic2Module : Module() {

  private var manager: Flic2Manager? = null
  private val triggerModes = mutableMapOf<String, String>()

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

    Function("initialize") {
      val context = appContext.reactContext ?: return@Function null
      manager = Flic2Manager.initAndGetInstance(context, Handler(Looper.getMainLooper()))
      manager?.getButtons()?.forEach { button ->
        button.addListener(createButtonListener())
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
            button.addListener(createButtonListener())
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
        manager?.forgetButton(button)
      }
    }

    Function("setButtonTriggerMode") { uuid: String, mode: String ->
      triggerModes[uuid] = mode
    }
  }

  private fun findButton(uuid: String): Flic2Button? {
    return manager?.getButtons()?.find { it.uuid == uuid }
  }

  private fun createButtonListener(): Flic2ButtonListener {
    return object : Flic2ButtonListener() {
      override fun onButtonSingleOrDoubleClickOrHold(
        button: Flic2Button,
        wasQueued: Boolean,
        lastQueued: Boolean,
        timestamp: Long,
        isSingleClick: Boolean,
        isDoubleClick: Boolean,
        isHold: Boolean
      ) {
        val ageSeconds = (System.currentTimeMillis() - timestamp) / 1000
        val mode = triggerModes[button.uuid] ?: "clickAndDoubleClickAndHold"
        val emitClick = isSingleClick && mode != "clickAndDoubleClick"
        val emitDoubleClick = isDoubleClick && mode != "click" && mode != "clickAndHold"
        val emitHold = isHold && mode != "click" && mode != "clickAndDoubleClick"
        if (emitClick) {
          sendEvent("onFlic2Click", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to ageSeconds
          ))
        }
        if (emitDoubleClick) {
          sendEvent("onFlic2DoubleClick", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to ageSeconds
          ))
        }
        if (emitHold) {
          sendEvent("onFlic2Hold", mapOf(
            "uuid" to button.uuid,
            "queued" to wasQueued,
            "age" to ageSeconds
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
          "age" to ((System.currentTimeMillis() - timestamp) / 1000)
        ))
      }

      override fun onConnect(button: Flic2Button) {
        sendEvent("onFlic2Connection", mapOf(
          "uuid" to button.uuid,
          "state" to "connected"
        ))
      }

      override fun onReady(button: Flic2Button, timestamp: Long) {
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
