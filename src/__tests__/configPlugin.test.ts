import { withInfoPlist, withAndroidManifest } from "@expo/config-plugins";
import withFlic2 from "../../plugin/src/index";

// Mock config-plugins
jest.mock("@expo/config-plugins", () => {
  const original = jest.requireActual("@expo/config-plugins");
  return {
    ...original,
    withInfoPlist: jest.fn((config, callback) => {
      callback(config);
      return config;
    }),
    withAndroidManifest: jest.fn((config, callback) => {
      callback(config);
      return config;
    }),
  };
});

function createMockConfig() {
  return {
    name: "test",
    slug: "test",
    modResults: {
      UIBackgroundModes: [] as string[],
      manifest: {
        "uses-permission": [] as Array<{ $: { "android:name": string } }>,
      },
    },
  } as any;
}

describe("withFlic2 config plugin", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("sets default Bluetooth permission strings on iOS", () => {
    const config = createMockConfig();
    withFlic2(config);

    expect(withInfoPlist).toHaveBeenCalled();
    expect(config.modResults.NSBluetoothAlwaysUsageDescription).toBe(
      "This app uses Bluetooth to communicate with Flic buttons"
    );
    expect(config.modResults.NSBluetoothPeripheralUsageDescription).toBe(
      "This app uses Bluetooth to communicate with Flic buttons"
    );
  });

  it("sets custom Bluetooth permission strings on iOS", () => {
    const config = createMockConfig();
    withFlic2(config, {
      bluetoothAlwaysPermission: "Custom always",
      bluetoothPeripheralPermission: "Custom peripheral",
    });

    expect(config.modResults.NSBluetoothAlwaysUsageDescription).toBe(
      "Custom always"
    );
    expect(config.modResults.NSBluetoothPeripheralUsageDescription).toBe(
      "Custom peripheral"
    );
  });

  it("adds bluetooth-central to UIBackgroundModes", () => {
    const config = createMockConfig();
    withFlic2(config);

    expect(config.modResults.UIBackgroundModes).toContain("bluetooth-central");
  });

  it("does not duplicate bluetooth-central if already present", () => {
    const config = createMockConfig();
    config.modResults.UIBackgroundModes = ["bluetooth-central"];
    withFlic2(config);

    const bgModes = config.modResults.UIBackgroundModes as string[];
    expect(bgModes.filter((m: string) => m === "bluetooth-central")).toHaveLength(1);
  });

  it("adds all required Android permissions", () => {
    const config = createMockConfig();
    withFlic2(config);

    expect(withAndroidManifest).toHaveBeenCalled();
    const permNames = config.modResults.manifest["uses-permission"].map(
      (p: any) => p.$["android:name"]
    );
    expect(permNames).toContain("android.permission.BLUETOOTH");
    expect(permNames).toContain("android.permission.BLUETOOTH_ADMIN");
    expect(permNames).toContain("android.permission.BLUETOOTH_SCAN");
    expect(permNames).toContain("android.permission.BLUETOOTH_CONNECT");
    expect(permNames).toContain("android.permission.ACCESS_FINE_LOCATION");
  });

  it("does not duplicate Android permissions if already present", () => {
    const config = createMockConfig();
    config.modResults.manifest["uses-permission"] = [
      { $: { "android:name": "android.permission.BLUETOOTH" } },
    ];
    withFlic2(config);

    const btPerms = config.modResults.manifest["uses-permission"].filter(
      (p: any) => p.$["android:name"] === "android.permission.BLUETOOTH"
    );
    expect(btPerms).toHaveLength(1);
  });
});
