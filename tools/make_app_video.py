"""에뮬레이터 화면 녹화(run.mp4)에 구간 자막을 넣어 design/video/app-run.mp4로 만든다.

사용: python3 tools/make_app_video.py <녹화.mp4> <ffmpeg 경로(libx264 포함)> [pleos]
  pleos를 붙이면 Pleos 에뮬레이터 녹화용 자막을 쓰고 design/video/app-run-pleos.mp4로 만든다.
녹화는 adb shell screenrecord로 받는다 (docs/제출-체크리스트.md 6번 참고).
"""
import os, shutil, subprocess, sys, tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
CAPS = [
    (0, 23, '실제 앱 실행 · 가상 주행 기록 60배속 · 급가감속, 쉰 시간, 차 안 온도로 멀미 위험 계산'),
    (23, 30, '급정거 버튼을 누르면 최근 10분 급가감속 횟수에 더해져요'),
    (30, 77, '정체 구간에서 급정거가 이어지면 주의 단계, 음성으로 부드러운 운전 안내'),
    (77, 84, '위험 단계 · 가까운 휴게소에서 쉬어 가도록 안내'),
    (84, 100, '휴게소에서 펫 지킴이 · 에어컨이 멈춰 27℃를 넘으면 위험 대응'),
    (100, 111, '도착 후 말한 컨디션으로 다음 주행의 쉬는 간격 조정 (80분 → 70분)'),
]

PLEOS_CAPS = [
    (0, 15, 'Pleos Connect 에뮬레이터 · 기어 D → NaviHelper로 강릉 경로 요청 (영동고속도로)'),
    (15, 52, '급정거를 반복하면 브레이크·속도 신호로 급가감속을 세고 점수가 올라가요 · Pleos TTS 음성 안내'),
    (52, 72, '"콩이 힘들어해" → 위험 단계 · 반려견 휴게소가 내비 경유지로 들어가 40분 뒤로 바뀌어요'),
    (72, 79, '기어 P → 휴식으로 기록하고 펫 지킴이 화면으로'),
    (79, 96, '차량 에어컨이 꺼지면 위험 대응 (꺼짐은 Vehicle SDK 신호, 온도 상승은 데모 계산)'),
    (96, 121, '도착 후 컨디션 기록 (Pleos STT, 시연은 버튼) → 다음 쉬는 간격 80분 → 70분'),
]

src, ff = sys.argv[1], sys.argv[2]
pleos = len(sys.argv) > 3 and sys.argv[3] == 'pleos'
if pleos:
    CAPS = PLEOS_CAPS
tmp = tempfile.mkdtemp()
font = os.path.join(tmp, 'font.ttf')
shutil.copy(os.path.join(ROOT, 'app/src/main/assets/fonts/AstaSans.ttf'), font)
parts = ['fps=30']
for i, (a, b, text) in enumerate(CAPS):
    tf = os.path.join(tmp, f'c{i}.txt')
    open(tf, 'w', encoding='utf-8').write(text)
    parts.append(f"drawtext=fontfile={font}:textfile={tf}:fontsize=34:fontcolor=white:box=1:"
                 f"boxcolor=0x131417@0.85:boxborderw=22:x=48:y=h-th-60:enable='between(t,{a},{b})'")
out = os.path.join(ROOT, 'design/video/app-run-pleos.mp4' if pleos else 'design/video/app-run.mp4')
subprocess.run([ff, '-v', 'error', '-y', '-i', src, '-map', '0:v', '-vf', ','.join(parts),
                '-c:v', 'libx264', '-preset', 'medium', '-crf', '23', '-pix_fmt', 'yuv420p',
                '-movflags', '+faststart', out], check=True)
print('ok', out)
