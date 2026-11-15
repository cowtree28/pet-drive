#!/bin/sh
# 데모 APK를 Gradle로 빌드한다 (Pleos Connect SDK 포함).
# 필요: JDK 17~21 (JAVA_HOME), Android SDK (ANDROID_HOME, platforms;android-35, build-tools;35.0.0), Gradle 8.x
#   이 저장소는 Gradle wrapper를 넣지 않았다. Android Studio로 열거나, gradle 8.11 이상을 PATH에 둔다.
# 결과: build/apk/petdrive-demo.apk (release 빌드, 코드 축소, 데모용 디버그 키 서명)
set -e
cd "$(dirname "$0")/.."
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Android/Sdk}
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties
${GRADLE:-gradle} -q assembleRelease
mkdir -p build/apk
cp app/build/outputs/apk/release/app-release.apk build/apk/petdrive-demo.apk
echo "ok build/apk/petdrive-demo.apk"
