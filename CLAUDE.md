# expo-flic2

## Versioning

When releasing a new version, update the version string in **three places**:

### 1. `package.json` (source of truth)
```json
"version": "X.Y.Z"
```
This also drives the **iOS** version — `ios/ExpoFlic2.podspec` reads `package['version']` directly, so no separate iOS change is needed.

### 2. `android/build.gradle` (two places)
```groovy
version = 'X.Y.Z'          // top-level, used by Gradle/Maven
...
defaultConfig {
    versionName "X.Y.Z"    // required by expo-module-gradle-plugin
}
```

### Summary table

| File | Field | Notes |
|------|-------|-------|
| `package.json` | `"version"` | Drives npm publish and iOS podspec |
| `android/build.gradle` | `version = '...'` | Gradle/Maven artifact version |
| `android/build.gradle` | `defaultConfig.versionName` | Required by expo-module-gradle-plugin |
| `ios/ExpoFlic2.podspec` | `s.version` | **Auto-read from package.json — do not edit** |
