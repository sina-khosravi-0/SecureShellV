#gradle build
gradle installDebug
adb shell am force-stop come.securelight.secureshellv
adb shell am start -n com.securelight.secureshellv/com.securelight.secureshellv.MainActivity
sleep 1
adb logcat --pid=$(adb shell pidof -s com.securelight.secureshellv) | grep -v "Empty SMPTE"

