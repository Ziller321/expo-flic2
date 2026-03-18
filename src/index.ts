import { EventSubscription } from "expo-modules-core";
import ExpoFlic2 from "./ExpoFlic2Module";
import {
  Flic2TriggerMode,
  type Flic2Button,
  type Flic2ClickEvent,
  type Flic2DoubleClickEvent,
  type Flic2HoldEvent,
  type Flic2UpOrDownEvent,
  type Flic2ConnectionEvent,
  type Flic2ScanEvent,
  type Flic2BatteryEvent,
  type Flic2ManagerStateEvent,
} from "./ExpoFlic2.types";

export {
  Flic2ConnectionState,
  Flic2TriggerMode,
  type Flic2Button,
  type Flic2ClickEvent,
  type Flic2DoubleClickEvent,
  type Flic2HoldEvent,
  type Flic2UpOrDownEvent,
  type Flic2ConnectionEvent,
  type Flic2ScanEvent,
  type Flic2BatteryEvent,
  type Flic2ManagerStateEvent,
} from "./ExpoFlic2.types";

export function initialize(): void {
  ExpoFlic2.initialize();
}

export function startScan(): void {
  ExpoFlic2.startScan();
}

export function stopScan(): void {
  ExpoFlic2.stopScan();
}

export function getButtons(): Flic2Button[] {
  return ExpoFlic2.getButtons();
}

export function connectButton(uuid: string): void {
  ExpoFlic2.connectButton(uuid);
}

export function disconnectButton(uuid: string): void {
  ExpoFlic2.disconnectButton(uuid);
}

export function forgetButton(uuid: string): void {
  ExpoFlic2.forgetButton(uuid);
}

export function setButtonTriggerMode(
  uuid: string,
  mode: Flic2TriggerMode
): void {
  ExpoFlic2.setButtonTriggerMode(uuid, mode);
}

// Typed event listeners

export function addOnClickListener(
  listener: (event: Flic2ClickEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2Click", listener);
}

export function addOnDoubleClickListener(
  listener: (event: Flic2DoubleClickEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2DoubleClick", listener);
}

export function addOnHoldListener(
  listener: (event: Flic2HoldEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2Hold", listener);
}

export function addOnUpOrDownListener(
  listener: (event: Flic2UpOrDownEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2UpOrDown", listener);
}

export function addOnConnectionListener(
  listener: (event: Flic2ConnectionEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2Connection", listener);
}

export function addOnScanListener(
  listener: (event: Flic2ScanEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2Scan", listener);
}

export function addOnBatteryListener(
  listener: (event: Flic2BatteryEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2Battery", listener);
}

/**
 * iOS only. The Android Flic2 SDK does not provide a Bluetooth manager state
 * callback, so this listener will never fire on Android.
 */
export function addOnManagerStateListener(
  listener: (event: Flic2ManagerStateEvent) => void
): EventSubscription {
  return ExpoFlic2.addListener("onFlic2ManagerState", listener);
}
