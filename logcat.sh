adb logcat --pid=$(adb shell pidof -s com.securelight.secureshellv) | grep -v "Empty SMPTE"
