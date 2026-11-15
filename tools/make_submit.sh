#!/bin/sh
# 메일에 첨부할 파일을 submit/ 폴더에 모은다. 사용: sh tools/make_submit.sh
set -e
cd "$(dirname "$0")/.."
python3 -I tools/fill_form.py docs/기획서-초안.md docs/양식-원본.docx docs/접수양식-작성본.docx
rm -rf submit && mkdir -p submit
cp docs/접수양식-작성본.docx "submit/01_접수양식_기획서_펫드라이브.docx"
cp design/video/pet-drive-demo-real.mp4 "submit/02_시연영상_실제앱_펫드라이브.mp4"
[ -f docs/기획서-읽기용.pdf ] && cp docs/기획서-읽기용.pdf "submit/03_기획서_읽기용_펫드라이브.pdf"
sh tools/build_apk.sh >/dev/null 2>&1 || echo "APK 다시 빌드 못 함 (JDK 17~21, Gradle 필요) → 기존 build/apk 사용"
cp build/apk/petdrive-demo.apk "submit/04_데모앱_펫드라이브.apk"
mkdir -p "submit/앱실행화면" && cp design/png/app/*.png "submit/앱실행화면/"
cp design/logo/logo-sheet.png "submit/앱실행화면/00_로고.png"
echo "submit/ 준비 완료 (접수양식의 01 참가신청서와 03·04 서명은 팀이 직접 작성)"
ls -la submit submit/앱실행화면
