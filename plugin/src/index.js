import { withInfoPlist, withAndroidManifest, } from "@expo/config-plugins";
const withFlic2 = (config, props) => {
    const bluetoothAlwaysPermission = props?.bluetoothAlwaysPermission ??
        "This app uses Bluetooth to communicate with Flic buttons";
    const bluetoothPeripheralPermission = props?.bluetoothPeripheralPermission ??
        "This app uses Bluetooth to communicate with Flic buttons";
    // iOS: Info.plist permissions and background modes
    config = withInfoPlist(config, (config) => {
        config.modResults.NSBluetoothAlwaysUsageDescription =
            bluetoothAlwaysPermission;
        config.modResults.NSBluetoothPeripheralUsageDescription =
            bluetoothPeripheralPermission;
        // Add bluetooth-central background mode
        const bgModes = config.modResults.UIBackgroundModes ?? [];
        if (!bgModes.includes("bluetooth-central")) {
            bgModes.push("bluetooth-central");
        }
        config.modResults.UIBackgroundModes = bgModes;
        return config;
    });
    // Android: Bluetooth permissions
    config = withAndroidManifest(config, (config) => {
        const manifest = config.modResults.manifest;
        if (!manifest["uses-permission"]) {
            manifest["uses-permission"] = [];
        }
        const permissions = [
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.ACCESS_FINE_LOCATION",
        ];
        for (const permission of permissions) {
            const exists = manifest["uses-permission"].some((p) => p.$?.["android:name"] === permission);
            if (!exists) {
                manifest["uses-permission"].push({
                    $: { "android:name": permission },
                });
            }
        }
        return config;
    });
    return config;
};
export default withFlic2;
//# sourceMappingURL=index.js.map