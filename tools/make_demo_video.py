"""시연 영상: 로고 → 왜 필요한가(카드 3장) → 실제 앱 실행 녹화(가운데 두 줄 자막).

사용: python3 tools/make_demo_video.py <ffmpeg 경로(libx264 포함)>
입력: design/logo/title.png, card-a/b/c.png, design/video/app-run-pleos.mp4
결과: design/video/pet-drive-demo-real.mp4
녹화의 예전 자막(왼쪽 아래)은 아래쪽 띠로 덮고, 그 위에 새 자막을 가운데 정렬로 쓴다.
"""
import os, shutil, subprocess, sys, tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
L = os.path.join(ROOT, 'design', 'logo')
FONT_DIR = os.path.expanduser('~/.local/share/fonts')

# (시작, 끝, 지금 무슨 일인지, 앱이 무엇을 왜 하는지) — 녹화 기준 초
CAPS = [
    (0, 15, "보호자가 강아지 '콩이'를 태우고 강릉으로 출발합니다",
     "기어를 D로 바꾸면 펫 드라이브가 Pleos 내비에 목적지 경로를 잡습니다"),
    (15, 52, "운전자가 급정거를 반복합니다. 강아지가 멀미하기 쉬운 운전입니다",
     "앱이 브레이크·속도 신호로 이를 세어 멀미 위험 점수를 올리고, 음성으로 부드러운 운전을 권합니다"),
    (52, 72, "보호자가 '콩이 힘들어해'를 누르자 위험 단계가 됩니다",
     "앱이 반려견 놀이터가 있는 휴게소를 내비 경유지에 바로 넣습니다 (40분 뒤 도착)"),
    (72, 79, "휴게소에 도착해 기어를 P로 바꿉니다",
     "강아지를 차에 잠깐 두고 내릴 때 쓰는 '펫 지킴이' 화면으로 바뀝니다"),
    (79, 96, "보호자가 자리를 비운 사이 차 에어컨이 꺼집니다",
     "앱이 바로 알아채 위험 대응으로 바꾸고 보호자에게 전화합니다 (온도가 오르는 속도는 시연용 계산)"),
    (96, 122, "여행이 끝나면 보호자가 오늘 콩이 컨디션을 말로 남깁니다",
     "'멀미 조금 했어'라고 하면 다음 여행부터 70분마다 쉬도록 바꿔 줍니다"),
]
INTRO = [('title.png', 3.5), ('card-a.png', 6.0), ('card-b.png', 5.0), ('card-c.png', 7.0)]

ff = sys.argv[1]
tmp = tempfile.mkdtemp()
bold = os.path.join(tmp, 'b.otf'); reg = os.path.join(tmp, 'r.otf')
shutil.copy(os.path.join(FONT_DIR, 'PretendardGOV-Bold.otf'), bold)
shutil.copy(os.path.join(FONT_DIR, 'PretendardGOV-Regular.otf'), reg)

args = [ff, '-v', 'error', '-y']
for img, sec in INTRO:
    args += ['-loop', '1', '-t', str(sec), '-i', os.path.join(L, img)]
args += ['-i', os.path.join(ROOT, 'design', 'video', 'app-run-pleos.mp4')]

parts = []
for i, (img, sec) in enumerate(INTRO):
    parts.append(f'[{i}:v]scale=1920:1080,fps=30,format=yuv420p,fade=t=in:st=0:d=0.4,fade=t=out:st={sec - 0.4}:d=0.4[i{i}]')

rec = ['scale=1920:1080', 'fps=30', 'format=yuv420p',
       'drawbox=x=0:y=ih-196:w=iw:h=196:color=0x131417@1.0:t=fill']
for k, (a, b, l1, l2) in enumerate(CAPS):
    f1 = os.path.join(tmp, f'l1_{k}.txt'); f2 = os.path.join(tmp, f'l2_{k}.txt')
    open(f1, 'w', encoding='utf-8').write(l1)
    open(f2, 'w', encoding='utf-8').write(l2)
    on = f"enable='between(t,{a},{b})'"
    rec.append(f"drawtext=fontfile={bold}:textfile={f1}:fontsize=42:fontcolor=white:x=(w-tw)/2:y=h-160:{on}")
    rec.append(f"drawtext=fontfile={reg}:textfile={f2}:fontsize=32:fontcolor=0xD7D9DF:x=(w-tw)/2:y=h-92:{on}")
n = len(INTRO)
parts.append(f'[{n}:v]' + ','.join(rec) + '[rec]')
parts.append(''.join(f'[i{i}]' for i in range(n)) + f'[rec]concat=n={n + 1}:v=1[v]')

out = os.path.join(ROOT, 'design', 'video', 'pet-drive-demo-real.mp4')
args += ['-filter_complex', ';'.join(parts), '-map', '[v]', '-c:v', 'libx264', '-crf', '22',
         '-preset', 'medium', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', out]
subprocess.run(args, check=True)
print('ok', out)
