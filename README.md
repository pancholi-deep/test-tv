# Glass Home Redirector (Fire OS 6 / Android 7.1)

A small helper for the Onida AFTTIFF55 / `rancho` TV. It watches Android UsageEvents and, when `com.amazon.tv.launcher` moves to the foreground, brings `com.fares.glass` back to the foreground.

It does **not** replace, disable, or modify Amazon's launcher, HomeStarter, key handler, recovery, or OTA components. It requires no root. This is a workaround, not a true Home-key remap; a brief Amazon launcher flash is possible, and Fire OS may still kill or restrict the service.

## Build without Android Studio (recommended)

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`. You do not need Android Studio or the Android SDK installed locally.

1. Create a new empty GitHub repository.
2. From this folder, initialize and push the project:

   ```bash
   git init
   git add .
   git commit -m "Add Glass Home Redirector"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git push -u origin main
   ```

3. On GitHub, open the repository's **Actions** tab. Select **Build Glass Home Redirector APK** and wait for the workflow to finish.
4. Open the completed workflow run and download the `glass-home-redirector-debug` artifact. Extract it; it contains `app-debug.apk`.

The workflow builds with JDK 17, Android SDK Platform 35, and Gradle 8.9 on GitHub's hosted runner.

## Install and enable on the TV

Connect ADB to the TV, then from the directory containing `app-debug.apk`:

```bash
adb install -r app-debug.apk
adb shell appops set com.deep.glassredirector GET_USAGE_STATS allow
adb shell appops get com.deep.glassredirector GET_USAGE_STATS
adb shell monkey -p com.deep.glassredirector 1
```

The `appops` command grants Usage Access to this helper only. If Fire OS's app-ops command rejects the operation, open the helper on the TV and try its **Open Usage Access settings** button. Enable Usage Access for **Glass Home Redirector**, return to the app, and tap **Start redirect monitoring**.

Once the app reports Usage Access granted, leave it running and press the remote's Home button. The helper should detect Amazon Home and bring Glass forward.

## Logs / diagnostics

```bash
adb logcat -s GlassRedirector:I
adb shell dumpsys activity activities | grep mResumedActivity
adb shell appops get com.deep.glassredirector GET_USAGE_STATS
```

## Stop / undo

Open Glass Home Redirector and tap **Stop redirect monitoring**, or run:

```bash
adb shell am force-stop com.deep.glassredirector
```

To revoke Usage Access:

```bash
adb shell appops set com.deep.glassredirector GET_USAGE_STATS deny
```

To uninstall the helper:

```bash
adb uninstall com.deep.glassredirector
```

No Amazon system package needs to be disabled for this experiment.
