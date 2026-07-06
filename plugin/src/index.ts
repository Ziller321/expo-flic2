import {
  type ConfigPlugin,
  withInfoPlist,
  withAndroidManifest,
} from "expo/config-plugins";

type Flic2PluginProps = {
  bluetoothAlwaysPermission?: string;
  bluetoothPeripheralPermission?: string;
};

const withFlic2: ConfigPlugin<Flic2PluginProps | void> = (config, props) => {
  const bluetoothAlwaysPermission =
    (props as Flic2PluginProps)?.bluetoothAlwaysPermission ??
    "This app uses Bluetooth to communicate with Flic buttons";
  const bluetoothPeripheralPermission =
    (props as Flic2PluginProps)?.bluetoothPeripheralPermission ??
    "This app uses Bluetooth to communicate with Flic buttons";

  // iOS: Info.plist permissions and background modes
  config = withInfoPlist(config, (config) => {
    config.modResults.NSBluetoothAlwaysUsageDescription =
      bluetoothAlwaysPermission;
    config.modResults.NSBluetoothPeripheralUsageDescription =
      bluetoothPeripheralPermission;

    // Add bluetooth-central background mode
    const bgModes = (config.modResults.UIBackgroundModes as string[]) ?? [];
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

    // Legacy Bluetooth and location permissions are only needed on Android 11
    // and below. On Android 12+ (API 31), BLUETOOTH_SCAN with neverForLocation
    // plus BLUETOOTH_CONNECT is sufficient and avoids requesting location.
    const permissions = [
      {
        $: {
          "android:name": "android.permission.BLUETOOTH",
          "android:maxSdkVersion": "30",
        },
      },
      {
        $: {
          "android:name": "android.permission.BLUETOOTH_ADMIN",
          "android:maxSdkVersion": "30",
        },
      },
      {
        $: {
          "android:name": "android.permission.BLUETOOTH_SCAN",
          "android:usesPermissionFlags": "neverForLocation",
        },
      },
      {
        $: { "android:name": "android.permission.BLUETOOTH_CONNECT" },
      },
      {
        $: {
          "android:name": "android.permission.ACCESS_FINE_LOCATION",
          "android:maxSdkVersion": "30",
        },
      },
    ];

    for (const permission of permissions) {
      const exists = manifest["uses-permission"].some(
        (p) => p.$?.["android:name"] === permission.$["android:name"],
      );
      if (!exists) {
        manifest["uses-permission"].push(permission);
      }
    }

    return config;
  });

  return config;
};

export default withFlic2;
