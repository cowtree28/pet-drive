"""시연 영상 V2: 아이콘 모션 인트로 → 왜 필요한가(카드) → 실제 앱 실행(가리지 않는 자막) → 아웃트로.

사용: python3 tools/make_demo_video_v2.py <ffmpeg 경로(libx264 포함)>
입력: design/logo/m-icon.png, m-word.png, m-tag.png, m-out.png, card-a/b/c.png, design/video/app-run-pleos.mp4
결과: design/video/pet-drive-demo-v2.mp4
실제 앱 화면은 줄여서 위에 두고, 자막은 화면 밖 아래 여백에 쓴다(앱 화면을 가리지 않음).
녹화 아래쪽의 예전 자막은 잘라 낸다.
"""
import os, shutil, subprocess, sys, tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
L = os.path.join(ROOT, 'design', 'logo')
FONT_DIR = os.path.expanduser('~/.local/share/fonts')
BG = '0x0E0F12'

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

ff = sys.argv[1]
tmp = tempfile.mkdtemp()
bold = os.path.join(tmp, 'b.otf'); reg = os.path.join(tmp, 'r.otf')
shutil.copy(os.path.join(FONT_DIR, 'PretendardGOV-Bold.otf'), bold)
shutil.copy(os.path.join(FONT_DIR, 'PretendardGOV-Regular.otf'), reg)
P = lambda n: os.path.join(L, n)


def run(args):
    subprocess.run([ff, '-v', 'error', '-y'] + args, check=True)


# 0~1 사이로 부드럽게 (smoothstep). a~b 초 구간
def ease(a, b):
    x = f'min(1,max(0,(t-{a})/{b - a}))'
    return f'({x}*{x}*(3-2*{x}))'


def intro(out, dur, tag=True, outro=False):
    """아이콘이 튀어나와 위로 올라가고, 이름과 소개가 떠오른다."""
    pop = 'if(lt(t,0.55),t/0.55*1.12,if(lt(t,0.85),1.12-(t-0.55)/0.3*0.12,1))'
    size = f'max(2,trunc(620*{pop}/2)*2)'
    ring = f"max(2,trunc((260+1300*{ease(0.15, 1.4)})/2)*2)"
    rise = ease(1.9, 2.7)
    icon_y = f'(H-h)/2-30-{rise}*190'
    word_y = f'600+(1-{ease(2.5, 3.2)})*50'
    tag_y = f'800+(1-{ease(3.3, 4.0)})*40'
    second = 'm-out.png' if outro else 'm-tag.png'
    f = (f"color=c={BG}:s=1920x1080:d={dur}:r=30[bg0];"
         f"[0:v]format=rgba,scale=w='{ring}':h='{ring}':eval=frame,fade=t=out:st=0.5:d=0.9:alpha=1[rg];"
         f"[bg0][rg]overlay=x='(W-w)/2':y='(H-h)/2-30':eval=frame[bg];"
         f"[1:v]format=rgba,scale=w='{size}':h='{size}':eval=frame[ic];"
         f"[2:v]format=rgba,fade=t=in:st=2.5:d=0.6:alpha=1[wd];"
         f"[3:v]format=rgba,fade=t=in:st=3.3:d=0.6:alpha=1[tg];"
         f"[bg][ic]overlay=x='(W-w)/2':y='{icon_y}':eval=frame[a];"
         f"[a][wd]overlay=x='(W-w)/2':y='{word_y}':eval=frame[b];"
         f"[b][tg]overlay=x='(W-w)/2':y='{tag_y}':eval=frame,format=yuv420p,"
         f"fade=t=out:st={dur - 0.5}:d=0.5[v]")
    run(['-loop', '1', '-t', str(dur), '-i', P('m-ring.png'),
         '-loop', '1', '-t', str(dur), '-i', P('m-icon.png'),
         '-loop', '1', '-t', str(dur), '-i', P('m-word.png'),
         '-loop', '1', '-t', str(dur), '-i', P(second),
         '-filter_complex', f, '-map', '[v]', '-t', str(dur), '-r', '30',
         '-c:v', 'libx264', '-crf', '20', '-pix_fmt', 'yuv420p', out])


def card(out, img, dur):
    """카드가 천천히 확대된다 (Ken Burns)."""
    frames = int(dur * 30)
    f = (f"[0:v]scale=3840:-1,zoompan=z='1+0.05*on/{frames}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
         f":d={frames}:s=1920x1080:fps=30,format=yuv420p,fade=t=in:st=0:d=0.5,fade=t=out:st={dur - 0.5}:d=0.5[v]")
    run(['-loop', '1', '-t', str(dur), '-i', P(img), '-filter_complex', f, '-map', '[v]',
         '-frames:v', str(frames), '-c:v', 'libx264', '-crf', '20', '-pix_fmt', 'yuv420p', out])


def demo(out):
    """실제 앱 녹화: 위쪽 930px만 쓰고(예전 자막 잘라 냄) 줄여서 올리고, 자막은 아래 여백에."""
    parts = [f"color=c={BG}:s=1920x1080:r=30[bg]",
             "[0:v]crop=1920:930:0:0,scale=1716:832,fps=30[rec]",
             "[bg][rec]overlay=x=(W-w)/2:y=34:shortest=1"]
    chain = []
    for k, (a, b, l1, l2) in enumerate(CAPS):
        f1 = os.path.join(tmp, f'l1_{k}.txt'); f2 = os.path.join(tmp, f'l2_{k}.txt')
        open(f1, 'w', encoding='utf-8').write(l1)
        open(f2, 'w', encoding='utf-8').write(l2)
        on = f"enable='between(t,{a},{b})'"
        chain.append(f"drawtext=fontfile={bold}:textfile={f1}:fontsize=40:fontcolor=white:x=(w-tw)/2:y=900:{on}")
        chain.append(f"drawtext=fontfile={reg}:textfile={f2}:fontsize=29:fontcolor=0xB9BCC4:x=(w-tw)/2:y=966:{on}")
    f = ';'.join(parts[:2]) + ';' + parts[2] + ',' + ','.join(chain) + ',format=yuv420p,fade=t=in:st=0:d=0.5[v]'
    run(['-i', os.path.join(ROOT, 'design', 'video', 'app-run-pleos.mp4'), '-filter_complex', f,
         '-map', '[v]', '-c:v', 'libx264', '-crf', '21', '-pix_fmt', 'yuv420p', out])


def roll(out):
    """'7/10'의 7이 숫자판처럼 0에서 7까지 굴러 내려와 멈춘다 (design/logo/roll/ 36장, 1.2초)."""
    run(['-framerate', '30', '-i', os.path.join(L, 'roll', 'f%03d.png'),
         '-vf', 'scale=1920:1080,format=yuv420p,fade=t=in:st=0:d=0.3', '-c:v', 'libx264', '-crf', '20',
         '-pix_fmt', 'yuv420p', out])


def build(out, card_id, holds, fade_in=True):
    """읽는 순서대로 내용이 하나씩 나타난다 (b-<카드>-<단계>.png를 차례로 겹쳐 넘김)."""
    xf = 0.35
    args, chain = [], []
    for i, h in enumerate(holds):
        args += ['-loop', '1', '-t', str(h + xf), '-i', P(f'b-{card_id}-{i}.png')]
        chain.append(f'[{i}:v]scale=1920:1080,fps=30,format=yuv420p[s{i}]')
    last, off = 's0', holds[0]
    for i in range(1, len(holds)):
        chain.append(f'[{last}][s{i}]xfade=transition=fade:duration={xf}:offset={off - xf}[x{i}]')
        last, off = f'x{i}', off + holds[i]
    total = sum(holds)
    fin = 'fade=t=in:st=0:d=0.4,' if fade_in else ''
    chain.append(f'[{last}]{fin}fade=t=out:st={total - 0.4}:d=0.4,trim=duration={total}[v]')
    run(args + ['-filter_complex', ';'.join(chain), '-map', '[v]', '-c:v', 'libx264', '-crf', '20',
                '-pix_fmt', 'yuv420p', out])


segs = []
for name, fn in [('s1', lambda o: intro(o, 6.5)),
                 ('s2r', roll),
                 ('s2', lambda o: build(o, 'a', [2.2, 3.0, 3.8], fade_in=False)),
                 ('s3', lambda o: build(o, 'b', [3.0, 2.8, 3.8])),
                 ('s4', lambda o: build(o, 'c', [3.0, 2.6, 2.6, 4.0])),
                 ('s5', demo),
                 ('s6', lambda o: intro(o, 6.0, outro=True))]:
    o = os.path.join(tmp, name + '.mp4')
    fn(o)
    segs.append(o)
lst = os.path.join(tmp, 'list.txt')
open(lst, 'w').write(''.join(f"file '{s}'\n" for s in segs))
out = os.path.join(ROOT, 'design', 'video', 'pet-drive-demo-v2.mp4')
run(['-f', 'concat', '-safe', '0', '-i', lst, '-c:v', 'libx264', '-crf', '21', '-pix_fmt', 'yuv420p',
     '-movflags', '+faststart', out])
print('ok', out)
