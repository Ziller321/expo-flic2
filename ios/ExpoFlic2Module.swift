import ExpoModulesCore
import flic2lib

public class ExpoFlic2Module: Module {

    private let bridge = Flic2ManagerBridge()

    public func definition() -> ModuleDefinition {
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

        OnCreate {
            bridge.module = self
        }

        Function("initialize") {
            FLICManager.configure(with: bridge, buttonDelegate: bridge, background: true)
        }

        Function("startScan") {
            guard let manager = FLICManager.shared() else { return }
            manager.scanForButtons(stateChangeHandler: { [weak self] event in
                let eventName: String
                switch event {
                case .discovered: eventName = "discovered"
                case .connected: eventName = "connected"
                case .verified: eventName = "verified"
                case .verificationFailed: eventName = "verificationFailed"
                @unknown default: eventName = "unknown"
                }
                self?.sendEvent("onFlic2Scan", [
                    "isScanning": true,
                    "scanEvent": eventName
                ])
            }, completion: { [weak self] button, error in
                if let button = button {
                    button.delegate = self?.bridge
                    self?.sendEvent("onFlic2Scan", [
                        "isScanning": false,
                        "button": Flic2ManagerBridge.buttonToRecord(button)
                    ])
                } else {
                    self?.sendEvent("onFlic2Scan", [
                        "isScanning": false,
                        "error": error?.localizedDescription ?? "Unknown scan error"
                    ])
                }
            })
        }

        Function("stopScan") {
            FLICManager.shared()?.stopScan()
        }

        Function("getButtons") { () -> [[String: Any]] in
            guard let manager = FLICManager.shared() else { return [] }
            return manager.buttons().map { Flic2ManagerBridge.buttonToRecord($0) }
        }

        Function("connectButton") { (uuid: String) in
            guard let button = findButton(uuid) else { return }
            button.connect()
        }

        Function("disconnectButton") { (uuid: String) in
            guard let button = findButton(uuid) else { return }
            button.disconnect()
        }

        Function("forgetButton") { (uuid: String) in
            guard let manager = FLICManager.shared(),
                  let button = findButton(uuid) else { return }
            manager.forgetButton(button) { _, _ in }
        }

        Function("setButtonTriggerMode") { (uuid: String, mode: String) in
            guard let button = findButton(uuid) else { return }
            switch mode {
            case "click":
                button.triggerMode = .click
            case "clickAndHold":
                button.triggerMode = .clickAndHold
            case "clickAndDoubleClick":
                button.triggerMode = .clickAndDoubleClick
            case "clickAndDoubleClickAndHold":
                button.triggerMode = .clickAndDoubleClickAndHold
            default:
                break
            }
        }
    }

    private func findButton(_ uuid: String) -> FLICButton? {
        guard let manager = FLICManager.shared(),
              let targetUUID = UUID(uuidString: uuid) else { return nil }
        return manager.buttons().first { $0.identifier == targetUUID }
    }
}
