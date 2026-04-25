# Qorb AI - Android App (AIDE)

## Setup Steps in AIDE

1. **Open AIDE** → New Project → Android App
2. **Copy files** as shown below into your project
3. **Change URL** in MainActivity.java line 17:
   ```java
   private static final String APP_URL = "https://YOUR_WORKER.workers.dev";
   ```
   Replace with your actual Cloudflare Workers URL.

## File Structure

```
QorbApp/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/qorb/app/
│       │   └── MainActivity.java
│       └── res/
│           ├── layout/
│           │   └── activity_main.xml
│           ├── values/
│           │   ├── strings.xml
│           │   └── styles.xml
│           └── xml/
│               └── file_paths.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Features
- ✅ Full WebView with JavaScript
- ✅ Microphone permission (no more "can't ask permission" popup)
- ✅ Camera access + file upload
- ✅ Gallery/Photos picker
- ✅ Back button navigation
- ✅ Fullscreen (no title bar)
- ✅ Dark background matches app theme
- ✅ DOM Storage (chat history saved)

## Package Name
`com.qorb.app` — change in build.gradle + AndroidManifest.xml if needed.
