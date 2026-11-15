#!/bin/sh
# 메일에 첨부할 파일을 submit/ 폴더에 모은다. 사용: sh tools/make_submit.sh
set -e
cd "$(dirname "$0")/.."
python3 -I tools/fill_form.py docs/기획서-초안.md docs/양식-원본.docx docs/접수양식-작성본.docx
rm -rf submit && mkdir -p submit/화면시안
cp docs/접수양식-작성본.docx "submit/01_접수양식_기획서_펫드라이브.docx"
cp design/png/00-board.png "submit/02_설명보드_펫드라이브.png"
cp design/video/pet-drive-demo.mp4 "submit/03_시연영상_펫드라이브.mp4"
[ -f docs/기획서-읽기용.pdf ] && cp docs/기획서-읽기용.pdf "submit/04_기획서_읽기용_펫드라이브.pdf"
i=1
for f in 00a-boarding 04a-driving-low 04-driving-rest 04c-driving-high 01-guard-start 02-guard-active 03-guard-danger 05-phone 05b-phone-alert 06-arrival; do
  cp "design/png/$f.png" "submit/화면시안/$(printf %02d $i)_$f.png"; i=$((i+1))
done
sh tools/build_apk.sh >/dev/null 2>&1 || echo "APK 다시 빌드 못 함 (JDK 17~21, Gradle 필요) → 기존 build/apk 사용"
cp build/apk/petdrive-demo.apk "submit/05_데모앱_펫드라이브.apk"
cp design/video/app-run-pleos.mp4 "submit/06_앱실행영상_Pleos에뮬레이터_펫드라이브.mp4"
mkdir -p "submit/앱실행화면_Pleos에뮬레이터" && cp design/png/app/*.png "submit/앱실행화면_Pleos에뮬레이터/"
echo "submit/ 준비 완료 (접수양식의 01 참가신청서와 03·04 서명은 팀이 직접 작성)"
ls -la submit submit/화면시안
