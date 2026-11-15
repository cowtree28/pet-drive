#!/bin/sh
# 화면 HTML을 PNG로 렌더링한다. 사용: ./render.sh [파일이름(확장자 없이)...]
CH=$(ls -d ~/.cache/ms-playwright/chromium_headless_shell-*/*/chrome-headless-shell | tail -1)
cd "$(dirname "$0")"
for n in "$@"; do
  case "$n" in
    05-phone|05b-phone-alert) timeout 60 "$CH" --headless --no-sandbox --hide-scrollbars --force-device-scale-factor=2 --window-size=390,844 --virtual-time-budget=3000 --screenshot=../png/$n.png "file://$PWD/$n.html" 2>/dev/null ;;
    board) timeout 60 "$CH" --headless --no-sandbox --hide-scrollbars --allow-file-access-from-files --window-size=2560,1600 --virtual-time-budget=4000 --screenshot=../png/00-board.png "file://$PWD/board.html" 2>/dev/null ;;
    *) timeout 60 "$CH" --headless --no-sandbox --hide-scrollbars --window-size=1920,1080 --virtual-time-budget=3000 --screenshot=../png/$n.png "file://$PWD/$n.html" 2>/dev/null ;;
  esac
done
