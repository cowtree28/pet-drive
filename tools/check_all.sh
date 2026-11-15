#!/bin/sh
# 제출 전 한 번에 확인: 로직 테스트, 주행 기록 재생, 시뮬레이터 일치, 접수양식 생성
set -e
cd "$(dirname "$0")/.."
rm -rf build/test
javac -encoding UTF-8 -d build/test app/src/main/java/ai/petdrive/core/*.java app/src/test/java/ai/petdrive/core/*.java
echo "== 로직 테스트"; java -cp build/test ai.petdrive.core.MotionRiskScorerTest | tail -1
echo "== 주행 기록 재생 (요약)"; java -cp build/test ai.petdrive.core.DriveReplay | tail -4
echo "== 시뮬레이터 일치"; node tools/check_simulator.js | tail -1
echo "== 접수양식"; python3 -I tools/fill_form.py docs/기획서-초안.md docs/양식-원본.docx docs/접수양식-작성본.docx
