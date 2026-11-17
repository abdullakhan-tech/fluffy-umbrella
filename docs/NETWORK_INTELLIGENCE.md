# Network Intelligence

Zaythar's Network Intelligence screen reports the active Android network while the screen is open.

## Live signals

- Active transport: Wi-Fi, mobile network, Ethernet, VPN, or offline.
- Android's validated-internet capability (not merely a connected radio).
- Metered connection state.
- Mobile data generation (2G / 3G / 4G / 5G) when Android exposes it and the user grants the optional phone-state permission. Android 12+ display callbacks also detect 5G NSA/advanced display overrides.
- Android's estimated upstream and downstream bandwidth (not a speed-test result).
- Roaming state when that permission is granted.

The monitor subscribes to Android's default-network callbacks and refreshes metadata every five seconds while the screen is active. It unregisters the callback when the screen stops collecting.

## Permissions

- `INTERNET` and `ACCESS_NETWORK_STATE` support connectivity and internet-state reporting.
- `READ_PHONE_STATE` is requested only when the user opens Network Intelligence and chooses to enable cellular-generation details. It is used for the current data-network type and roaming flag; it is not used to read calls, SMS, or contacts.

If permission is declined, connectivity still works and the screen reports a generic mobile-network state.

## Important limitations

Zaythar does not create a 5G/4G/3G connection, switch carrier plans, or force the modem to use a radio generation. The modem, SIM/eSIM, carrier coverage, Android device firmware, and user-selected system network mode determine the available connection. Some 5G NSA deployments are exposed by Android through a display override while the underlying data-network type remains LTE; on Android 12+ this implementation listens for that display callback. Older Android releases may only expose the underlying LTE type, so some 5G NSA sessions can still appear as 4G.

A validated network capability is Android's assessment of internet reachability; it is not a guarantee that every website or AI provider is reachable.

## Verification

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The unit test covers the common 2G, 3G, 4G, and 5G telephony constants. Device verification is still required across carriers and Android versions.
