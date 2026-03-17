package expo.modules.flic2

import io.flic.flic2libandroid.Flic2Button

fun Flic2Button.toRecord(triggerMode: String = "clickAndDoubleClickAndHold"): Map<String, Any?> {
  return mapOf(
    "uuid" to uuid,
    "bluetoothAddress" to bdAddr,
    "serialNumber" to serialNumber,
    "name" to name,
    "connectionState" to when (connectionState) {
      Flic2Button.CONNECTION_STATE_DISCONNECTED -> "disconnected"
      Flic2Button.CONNECTION_STATE_CONNECTING -> "connecting"
      Flic2Button.CONNECTION_STATE_CONNECTED_STARTING -> "connected"
      Flic2Button.CONNECTION_STATE_CONNECTED_READY -> "ready"
      else -> "disconnected"
    },
    "firmwareVersion" to firmwareVersion,
    "batteryLevel" to (lastKnownBatteryLevel?.estimatedPercentage ?: -1),
    "pressCount" to pressCount,
    "triggerMode" to triggerMode,
    "isReady" to (connectionState == Flic2Button.CONNECTION_STATE_CONNECTED_READY)
  )
}
