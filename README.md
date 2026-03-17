# expo-flic2

Expo module for integrating [Flic2](https://flic.io/) Bluetooth buttons into your React Native / Expo app.

Supports **iOS** and **Android**. Provides a typed API for scanning, connecting, and reacting to button events (click, double-click, hold, and press/release).

## Installation

```sh
npm install expo-flic2
```

### iOS

Run `npx pod-install` after installing.

Add Bluetooth usage descriptions to your `app.json` (or `app.config.js`) — the config plugin handles this automatically when using Expo managed workflow:

```json
{
  "expo": {
    "plugins": ["expo-flic2"]
  }
}
```

For bare React Native projects, add to `ios/MyApp/Info.plist`:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app uses Bluetooth to connect to Flic2 buttons.</string>
<key>NSBluetoothPeripheralUsageDescription</key>
<string>This app uses Bluetooth to connect to Flic2 buttons.</string>
```

### Android

The module requires the following permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

The config plugin adds these automatically for managed Expo projects.

## Quick Start

```typescript
import { useEffect } from "react";
import {
  initialize,
  startScan,
  stopScan,
  addOnScanListener,
  addOnClickListener,
  addOnConnectionListener,
  connectButton,
} from "expo-flic2";

export default function App() {
  useEffect(() => {
    // 1. Initialize the Flic2 Bluetooth manager
    initialize();

    // 2. Listen for button events
    const clickSub = addOnClickListener(({ uuid, queued, age }) => {
      console.log(`Button ${uuid} clicked (age: ${age}ms)`);
    });

    const connSub = addOnConnectionListener(({ uuid, state, error }) => {
      console.log(`Button ${uuid} connection state: ${state}`);
      if (error) console.error("Connection error:", error);
    });

    // 3. Scan for nearby buttons
    const scanSub = addOnScanListener(({ isScanning, button, error }) => {
      if (error) {
        console.error("Scan error:", error);
        return;
      }
      if (button) {
        console.log("Discovered button:", button.name, button.uuid);
        // Connect as soon as it's found
        connectButton(button.uuid);
      }
    });

    startScan();

    return () => {
      clickSub.remove();
      connSub.remove();
      scanSub.remove();
      stopScan();
    };
  }, []);

  return <YourAppUI />;
}
```

## API

### Initialization

#### `initialize()`

Initialize the Flic2 Bluetooth manager. Call this once before using any other API, typically on app startup.

```typescript
import { initialize } from "expo-flic2";

initialize();
```

### Scanning

#### `startScan()`

Start scanning for nearby Flic2 buttons. Fires `onFlic2Scan` events as buttons are discovered.

#### `stopScan()`

Stop the active scan.

```typescript
import { startScan, stopScan, addOnScanListener } from "expo-flic2";

const sub = addOnScanListener(({ isScanning, button, error, scanEvent }) => {
  if (button) {
    console.log("Found:", button.name, button.serialNumber);
  }
  if (!isScanning) {
    console.log("Scan stopped");
  }
});

startScan();

// Later...
stopScan();
sub.remove();
```

### Button Management

#### `getButtons(): Flic2Button[]`

Returns all known (previously paired or discovered) buttons.

```typescript
import { getButtons } from "expo-flic2";

const buttons = getButtons();
buttons.forEach((btn) => {
  console.log(btn.name, btn.connectionState, btn.batteryLevel);
});
```

#### `connectButton(uuid: string)`

Connect to a button by its UUID.

#### `disconnectButton(uuid: string)`

Disconnect from a button without removing it from the known list.

#### `forgetButton(uuid: string)`

Disconnect and remove a button from the known list.

```typescript
import { connectButton, disconnectButton, forgetButton } from "expo-flic2";

connectButton("some-uuid");
disconnectButton("some-uuid");
forgetButton("some-uuid"); // removes it entirely
```

#### `setButtonTriggerMode(uuid: string, mode: Flic2TriggerMode)`

Configure which gesture events a button fires. Use this to reduce latency — for example, if you only need single clicks, set `Click` mode so the button doesn't wait to rule out a double-click.

```typescript
import { setButtonTriggerMode, Flic2TriggerMode } from "expo-flic2";

// Only fire click events
setButtonTriggerMode(uuid, Flic2TriggerMode.Click);

// Fire click and double-click events
setButtonTriggerMode(uuid, Flic2TriggerMode.ClickAndDoubleClick);

// Fire click and hold events
setButtonTriggerMode(uuid, Flic2TriggerMode.ClickAndHold);

// Fire all event types
setButtonTriggerMode(uuid, Flic2TriggerMode.ClickAndDoubleClickAndHold);
```

### Event Listeners

All listeners return an `EventSubscription` — call `.remove()` to unsubscribe.

#### `addOnClickListener(listener)`

Fires when a button is single-clicked.

```typescript
const sub = addOnClickListener(({ uuid, queued, age }) => {
  // queued: true if the event was stored locally while disconnected
  // age: how many ms ago the event occurred
  console.log(`Clicked: ${uuid}`);
});
```

#### `addOnDoubleClickListener(listener)`

Fires when a button is double-clicked. Requires `Flic2TriggerMode.ClickAndDoubleClick` or `ClickAndDoubleClickAndHold`.

```typescript
const sub = addOnDoubleClickListener(({ uuid, queued, age }) => {
  console.log(`Double-clicked: ${uuid}`);
});
```

#### `addOnHoldListener(listener)`

Fires when a button is held. Requires `Flic2TriggerMode.ClickAndHold` or `ClickAndDoubleClickAndHold`.

```typescript
const sub = addOnHoldListener(({ uuid, queued, age }) => {
  console.log(`Held: ${uuid}`);
});
```

#### `addOnUpOrDownListener(listener)`

Fires on every press and release, regardless of trigger mode.

```typescript
const sub = addOnUpOrDownListener(({ uuid, isDown, queued, age }) => {
  console.log(`Button ${uuid} ${isDown ? "pressed" : "released"}`);
});
```

#### `addOnConnectionListener(listener)`

Fires when a button's connection state changes.

```typescript
import { addOnConnectionListener, Flic2ConnectionState } from "expo-flic2";

const sub = addOnConnectionListener(({ uuid, state, error }) => {
  if (state === Flic2ConnectionState.Ready) {
    console.log(`${uuid} is ready to use`);
  } else if (state === Flic2ConnectionState.Disconnected && error) {
    console.error(`${uuid} disconnected with error: ${error}`);
  }
});
```

#### `addOnScanListener(listener)`

Fires during scanning with discovery updates.

```typescript
const sub = addOnScanListener(({ isScanning, button, error, scanEvent }) => {
  // scanEvent (iOS only): "discovered" | "connected" | "verified" | "verificationFailed"
  if (button) console.log("Discovered:", button.name);
  if (error) console.error("Scan error:", error);
});
```

#### `addOnBatteryListener(listener)`

Fires when a button reports a battery level update.

```typescript
const sub = addOnBatteryListener(({ uuid, level }) => {
  console.log(`Battery for ${uuid}: ${level}%`);
  if (level < 20) {
    alert("Flic2 battery is low!");
  }
});
```

#### `addOnManagerStateListener(listener)`

Fires when the device's Bluetooth state changes.

```typescript
const sub = addOnManagerStateListener(({ state }) => {
  if (state === "poweredOff") {
    alert("Please enable Bluetooth to use Flic2 buttons.");
  }
});
```

## Types

```typescript
type Flic2Button = {
  uuid: string;
  bluetoothAddress: string;
  serialNumber: string;
  name: string;
  connectionState: Flic2ConnectionState;
  firmwareVersion: number;
  batteryLevel: number;
  pressCount: number;
  triggerMode: Flic2TriggerMode;
  isReady: boolean;
};

enum Flic2ConnectionState {
  Disconnected = "disconnected",
  Connecting = "connecting",
  Connected = "connected",
  Ready = "ready",
}

enum Flic2TriggerMode {
  Click = "click",
  ClickAndHold = "clickAndHold",
  ClickAndDoubleClick = "clickAndDoubleClick",
  ClickAndDoubleClickAndHold = "clickAndDoubleClickAndHold",
}
```

## Complete Example: Button Controller Hook

```typescript
import { useEffect, useState } from "react";
import {
  initialize,
  startScan,
  stopScan,
  getButtons,
  connectButton,
  setButtonTriggerMode,
  addOnManagerStateListener,
  addOnScanListener,
  addOnConnectionListener,
  addOnClickListener,
  addOnDoubleClickListener,
  addOnHoldListener,
  Flic2Button,
  Flic2TriggerMode,
  Flic2ConnectionState,
} from "expo-flic2";

export function useFlic2() {
  const [buttons, setButtons] = useState<Flic2Button[]>([]);
  const [isScanning, setIsScanning] = useState(false);
  const [bluetoothReady, setBluetoothReady] = useState(false);

  useEffect(() => {
    initialize();

    const managerSub = addOnManagerStateListener(({ state }) => {
      setBluetoothReady(state === "poweredOn");
    });

    const scanSub = addOnScanListener(({ isScanning, button }) => {
      setIsScanning(isScanning);
      if (button) {
        connectButton(button.uuid);
        setButtonTriggerMode(button.uuid, Flic2TriggerMode.ClickAndDoubleClickAndHold);
      }
    });

    const connSub = addOnConnectionListener(({ state }) => {
      if (state === Flic2ConnectionState.Ready) {
        setButtons(getButtons());
      }
    });

    return () => {
      managerSub.remove();
      scanSub.remove();
      connSub.remove();
    };
  }, []);

  const scan = () => {
    setIsScanning(true);
    startScan();
    // Auto-stop after 10 seconds
    setTimeout(() => stopScan(), 10_000);
  };

  return { buttons, isScanning, bluetoothReady, scan };
}

// Usage in a component:
function FlicController() {
  const { buttons, isScanning, bluetoothReady, scan } = useFlic2();

  useEffect(() => {
    const clickSub = addOnClickListener(({ uuid }) => {
      console.log("Click from", uuid);
    });
    const doubleSub = addOnDoubleClickListener(({ uuid }) => {
      console.log("Double-click from", uuid);
    });
    const holdSub = addOnHoldListener(({ uuid }) => {
      console.log("Hold from", uuid);
    });

    return () => {
      clickSub.remove();
      doubleSub.remove();
      holdSub.remove();
    };
  }, []);

  // ...
}
```

## Contributing

Contributions are welcome! Please refer to the [contributing guide](https://github.com/expo/expo#contributing).
