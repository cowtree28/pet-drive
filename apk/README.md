# APK

`대방어_펫드라이브_release.apk`: 펫 드라이브 release 빌드 (코드 축소, 데모용 디버그 키 서명).

- 설치: `adb install -r -g 대방어_펫드라이브_release.apk`
- Pleos Connect 에뮬레이터 v2.1.0에서 실행을 확인했습니다. 차량 값은 Virtual Console(`adb forward tcp:8088 tcp:8088` → http://127.0.0.1:8088/)로 바꿀 수 있습니다.
- 다시 빌드: `sh tools/build_apk.sh` (JDK 17~21, Gradle 8.11)
