import {
  Flic2ConnectionState,
  Flic2TriggerMode,
} from "../ExpoFlic2.types";

describe("Flic2ConnectionState enum", () => {
  it("has all expected values", () => {
    expect(Flic2ConnectionState.Disconnected).toBe("disconnected");
    expect(Flic2ConnectionState.Connecting).toBe("connecting");
    expect(Flic2ConnectionState.Connected).toBe("connected");
    expect(Flic2ConnectionState.Ready).toBe("ready");
  });

  it("has exactly 4 values", () => {
    const values = Object.values(Flic2ConnectionState);
    expect(values).toHaveLength(4);
  });
});

describe("Flic2TriggerMode enum", () => {
  it("has all expected values", () => {
    expect(Flic2TriggerMode.Click).toBe("click");
    expect(Flic2TriggerMode.ClickAndHold).toBe("clickAndHold");
    expect(Flic2TriggerMode.ClickAndDoubleClick).toBe("clickAndDoubleClick");
    expect(Flic2TriggerMode.ClickAndDoubleClickAndHold).toBe(
      "clickAndDoubleClickAndHold"
    );
  });

  it("has exactly 4 values", () => {
    const values = Object.values(Flic2TriggerMode);
    expect(values).toHaveLength(4);
  });
});

describe("public API exports", () => {
  // We can't import the full module (requires native), but we can verify the types compile
  it("Flic2ConnectionState values match what native modules emit", () => {
    // These strings must match what Android/iOS emit in connection events
    const nativeStrings = [
      "disconnected",
      "connecting",
      "connected",
      "ready",
    ];
    const enumValues = Object.values(Flic2ConnectionState);
    expect(enumValues).toEqual(nativeStrings);
  });

  it("Flic2TriggerMode values match what native modules accept", () => {
    // These strings must match what Android/iOS accept in setButtonTriggerMode
    const nativeStrings = [
      "click",
      "clickAndHold",
      "clickAndDoubleClick",
      "clickAndDoubleClickAndHold",
    ];
    const enumValues = Object.values(Flic2TriggerMode);
    expect(enumValues).toEqual(nativeStrings);
  });
});
