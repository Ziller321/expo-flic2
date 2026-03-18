export enum Flic2ConnectionState {
  Disconnected = "disconnected",
  Connecting = "connecting",
  Connected = "connected",
  Ready = "ready",
  Unpaired = "unpaired",
}

export enum Flic2TriggerMode {
  Click = "click",
  ClickAndHold = "clickAndHold",
  ClickAndDoubleClick = "clickAndDoubleClick",
  ClickAndDoubleClickAndHold = "clickAndDoubleClickAndHold",
}

export type Flic2Button = {
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

export type Flic2ClickEvent = {
  uuid: string;
  queued: boolean;
  age: number;
};

export type Flic2DoubleClickEvent = {
  uuid: string;
  queued: boolean;
  age: number;
};

export type Flic2HoldEvent = {
  uuid: string;
  queued: boolean;
  age: number;
};

export type Flic2UpOrDownEvent = {
  uuid: string;
  isDown: boolean;
  queued: boolean;
  age: number;
};

export type Flic2ConnectionEvent = {
  uuid: string;
  state: Flic2ConnectionState;
  error?: string;
};

export type Flic2ScanEvent = {
  isScanning: boolean;
  button?: Flic2Button;
  error?: string;
  /** iOS only: scanner status during scan (discovered, connected, verified, verificationFailed) */
  scanEvent?: string;
};

export type Flic2BatteryEvent = {
  uuid: string;
  level: number;
};

export type Flic2ManagerStateEvent = {
  state: "poweredOn" | "poweredOff" | "resetting" | "unsupported" | "unauthorized" | "unknown";
};
