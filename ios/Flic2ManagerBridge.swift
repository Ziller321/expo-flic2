import Foundation
import flic2lib

class Flic2ManagerBridge: NSObject, FLICManagerDelegate, FLICButtonDelegate {

    weak var module: ExpoFlic2Module?

    // MARK: - FLICManagerDelegate

    func managerDidRestoreState(_ manager: FLICManager) {
        for button in manager.buttons() {
            button.delegate = self
        }
    }

    func manager(_ manager: FLICManager, didUpdate state: FLICManagerState) {
        let stateString: String
        switch state {
        case .poweredOn: stateString = "poweredOn"
        case .poweredOff: stateString = "poweredOff"
        case .resetting: stateString = "resetting"
        case .unsupported: stateString = "unsupported"
        case .unauthorized: stateString = "unauthorized"
        default: stateString = "unknown"
        }
        module?.sendEvent("onFlic2ManagerState", ["state": stateString])
    }

    // MARK: - FLICButtonDelegate (connection)

    func buttonDidConnect(_ button: FLICButton) {
        module?.sendEvent("onFlic2Connection", [
            "uuid": button.identifier.uuidString,
            "state": "connected"
        ])
    }

    func buttonIsReady(_ button: FLICButton) {
        module?.sendEvent("onFlic2Connection", [
            "uuid": button.identifier.uuidString,
            "state": "ready"
        ])
    }

    func button(_ button: FLICButton, didDisconnectWithError error: Error?) {
        var payload: [String: Any] = [
            "uuid": button.identifier.uuidString,
            "state": "disconnected"
        ]
        if let error = error {
            payload["error"] = error.localizedDescription
        }
        module?.sendEvent("onFlic2Connection", payload)
    }

    func button(_ button: FLICButton, didFailToConnectWithError error: Error?) {
        var payload: [String: Any] = [
            "uuid": button.identifier.uuidString,
            "state": "disconnected"
        ]
        if let error = error {
            payload["error"] = error.localizedDescription
        }
        module?.sendEvent("onFlic2Connection", payload)
    }

    // MARK: - FLICButtonDelegate (click events via legacy delegate methods)

    func button(_ button: FLICButton, didReceiveButtonDown queued: Bool, age: Int) {
        module?.sendEvent("onFlic2UpOrDown", [
            "uuid": button.identifier.uuidString,
            "isDown": true,
            "queued": queued,
            "age": age
        ])
    }

    func button(_ button: FLICButton, didReceiveButtonUp queued: Bool, age: Int) {
        module?.sendEvent("onFlic2UpOrDown", [
            "uuid": button.identifier.uuidString,
            "isDown": false,
            "queued": queued,
            "age": age
        ])
    }

    func button(_ button: FLICButton, didReceiveButtonClick queued: Bool, age: Int) {
        module?.sendEvent("onFlic2Click", [
            "uuid": button.identifier.uuidString,
            "queued": queued,
            "age": age
        ])
    }

    func button(_ button: FLICButton, didReceiveButtonDoubleClick queued: Bool, age: Int) {
        module?.sendEvent("onFlic2DoubleClick", [
            "uuid": button.identifier.uuidString,
            "queued": queued,
            "age": age
        ])
    }

    func button(_ button: FLICButton, didReceiveButtonHold queued: Bool, age: Int) {
        module?.sendEvent("onFlic2Hold", [
            "uuid": button.identifier.uuidString,
            "queued": queued,
            "age": age
        ])
    }

    // MARK: - FLICButtonDelegate (battery)

    func button(_ button: FLICButton, didUpdateBatteryVoltage voltage: Float) {
        let level = Flic2ManagerBridge.batteryPercentage(from: voltage)
        module?.sendEvent("onFlic2Battery", [
            "uuid": button.identifier.uuidString,
            "level": level
        ])
    }

    // MARK: - Helpers

    /// Normalize CR2032 voltage (2.0V–3.0V) to percentage (0–100). Returns -1 if no sample.
    static func batteryPercentage(from voltage: Float) -> Int {
        guard voltage > 0 else { return -1 }
        return Int(max(0, min(100, (voltage - 2.0) / 1.0 * 100)))
    }

    static func buttonToRecord(_ button: FLICButton) -> [String: Any] {
        let connectionState: String
        switch button.state {
        case .disconnected: connectionState = "disconnected"
        case .connecting: connectionState = "connecting"
        case .connected: connectionState = "connected"
        case .disconnecting: connectionState = "disconnected"
        @unknown default: connectionState = "disconnected"
        }

        let triggerMode: String
        switch button.triggerMode {
        case .click: triggerMode = "click"
        case .clickAndHold: triggerMode = "clickAndHold"
        case .clickAndDoubleClick: triggerMode = "clickAndDoubleClick"
        case .clickAndDoubleClickAndHold: triggerMode = "clickAndDoubleClickAndHold"
        @unknown default: triggerMode = "clickAndDoubleClickAndHold"
        }

        let batteryLevel = batteryPercentage(from: button.batteryVoltage)

        return [
            "uuid": button.identifier.uuidString,
            "bluetoothAddress": button.bluetoothAddress,
            "serialNumber": button.serialNumber,
            "name": button.name ?? "",
            "connectionState": connectionState,
            "firmwareVersion": button.firmwareRevision,
            "batteryLevel": batteryLevel,
            "pressCount": button.pressCount,
            "triggerMode": triggerMode,
            "isReady": button.isReady
        ]
    }
}
