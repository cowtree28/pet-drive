#!/bin/sh
# 메일에 첨부할 파일을 submit/ 폴더에 모은다. 사용: sh tools/make_submit.sh
set -e
cd "$(dirname "$0")/.."
python3 -I tools/fill_form.py docs/기획서-초안.md docs/양식-원본.docx docs/접수양식-작성본.docx
rm -rf submit && mkdir -p submit
# 공식 규칙은 메일 제목뿐이라, 앱 이름을 앞에 두고 번호로 순서를 맞춘다. 보낼 때 앞에 "팀명_"을 붙인다.
cp docs/접수양식-작성본.docx "submit/펫드라이브_01_접수양식(기획서).docx"
cp design/video/pet-drive-demo-v2.mp4 "submit/펫드라이브_02_시연영상.mp4"
[ -f docs/기획서-읽기용.pdf ] && cp docs/기획서-읽기용.pdf "submit/펫드라이브_03_기획서(읽기용).pdf"
sh tools/build_apk.sh >/dev/null 2>&1 || echo "APK 다시 빌드 못 함 (JDK 17~21, Gradle 필요) → 기존 build/apk 사용"
cp build/apk/petdrive-demo.apk "submit/펫드라이브_04_데모앱.apk"
mkdir -p "submit/펫드라이브_05_앱실행화면" && cp design/png/app/*.png "submit/펫드라이브_05_앱실행화면/"
cp design/logo/logo-sheet.png "submit/펫드라이브_05_앱실행화면/00_로고.png"
echo "submit/ 준비 완료 (접수양식의 01 참가신청서와 03·04 서명은 팀이 직접 작성)"
ls -la submit
