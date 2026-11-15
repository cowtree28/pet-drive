"""화면 PNG와 자막으로 시연 영상(WebM)을 만든다.

사용: python3 make_video.py
결과: pet-drive-demo.webm (1920x1080, 10fps)
필요: Playwright가 받아 둔 chrome-headless-shell, ffmpeg (~/.cache/ms-playwright)
"""
import glob, os, subprocess, html, sys

HERE = os.path.dirname(os.path.abspath(__file__))
PNG = os.path.join(HERE, '..', 'png')
FR = os.path.join(HERE, 'frames')
PURL = 'file://' + os.path.abspath(PNG)
os.makedirs(FR, exist_ok=True)
CH = sorted(glob.glob(os.path.expanduser('~/.cache/ms-playwright/chromium_headless_shell-*/*/chrome-headless-shell')))[-1]
FF = sorted(glob.glob(os.path.expanduser('~/.cache/ms-playwright/ffmpeg-*/ffmpeg-linux')))[-1]
FPS = 10
VERTICAL = '--vertical' in sys.argv
W, H = (1080, 1920) if VERTICAL else (1920, 1080)
FADE = 4  # 전환 프레임 수

CSS = '''@font-face{font-family:"Asta Sans";src:url("FONTURL")}
*{margin:0;padding:0;box-sizing:border-box}body{width:1920px;height:1080px;overflow:hidden;font-family:"Asta Sans";background:#F7F8FA;color:#131417;word-break:keep-all}
.layer{position:absolute;inset:0}
.shot{position:absolute;left:120px;top:36px;width:1680px;height:945px;border-radius:24px;overflow:hidden;box-shadow:0 0 0 1px #E0E1E6}
.shot img{width:100%;height:100%;display:block}
.cap{position:absolute;left:120px;right:120px;top:996px;display:flex;align-items:center;gap:24px}
.cap .tag{flex:none;height:48px;padding:0 20px;border-radius:24px;background:#131417;color:#fff;font-size:24px;font-weight:700;display:flex;align-items:center}
.cap .t{font-size:34px;font-weight:700;letter-spacing:-.5px}
.duo{position:absolute;left:120px;top:36px;display:flex;gap:40px;align-items:center}
.duo .car{width:1240px;height:698px;border-radius:20px;overflow:hidden;box-shadow:0 0 0 1px #E0E1E6}
.duo .car img,.duo .ph img{width:100%;height:100%;display:block}
.duo .ph{width:436px;height:944px;border-radius:44px;overflow:hidden;box-shadow:0 0 0 10px #131417}
.title{position:absolute;inset:0;display:flex;flex-direction:column;justify-content:center;padding:0 160px;background:#fff}
.title h1{font-size:128px;font-weight:800;letter-spacing:-4px}
.title h2{font-size:56px;font-weight:700;margin-top:24px;letter-spacing:-1px}
.title p{font-size:28px;color:#686A72;margin-top:48px}
.title .ring{position:absolute;right:180px;top:50%;margin-top:-230px}
.board{position:absolute;left:120px;top:36px;width:1680px;height:945px;border-radius:24px;overflow:hidden;box-shadow:0 0 0 1px #E0E1E6;background:#fff}
.board img{height:100%;width:auto;display:block;margin:0 auto}'''


def ring_svg(size=460, stroke=36, frac=.3, color='#02C265'):
    import math
    r = (size - stroke) / 2; c = 2 * math.pi * r
    return (f'<svg class="ring" width="{size}" height="{size}" style="transform:rotate(-90deg)">'
            f'<circle cx="{size/2}" cy="{size/2}" r="{r}" stroke="#EDEEF2" stroke-width="{stroke}" fill="none"/>'
            f'<circle cx="{size/2}" cy="{size/2}" r="{r}" stroke="{color}" stroke-width="{stroke}" fill="none" stroke-linecap="round" '
            f'stroke-dasharray="{c:.1f}" stroke-dashoffset="{c*(1-frac):.1f}"/></svg>')


def body(scene):
    kind = scene['kind']
    if kind == 'title':
        return (f'<div class="title"><h1>펫 드라이브</h1><h2>반려견이 타면 차가 먼저 챙깁니다</h2>'
                f'<p>Pleos Connect 차량용 앱 시연 (화면 프로토타입)</p>{ring_svg()}</div>')
    if kind == 'end':
        return (f'<div class="board"><img src="{PURL}/00-board.png"></div>'
                f'<div class="cap"><span class="tag">정리</span><span class="t">{html.escape(scene["cap"])}</span></div>')
    if kind == 'duo':
        return (f'<div class="duo"><div class="car"><img src="{PURL}/{scene["img"]}.png"></div>'
                f'<div class="ph"><img src="{PURL}/{scene.get("phone", "05-phone")}.png"></div></div>'
                f'<div class="cap"><span class="tag">{scene["tag"]}</span><span class="t">{html.escape(scene["cap"])}</span></div>')
    src = scene.get('img_url') or f'{PURL}/{scene["img"]}.png'
    return (f'<div class="shot"><img src="{src}"></div>'
            f'<div class="cap"><span class="tag">{scene["tag"]}</span><span class="t">{html.escape(scene["cap"])}</span></div>')


SCENES = [
    dict(kind='title', sec=4),
    dict(kind='shot', img='00a-boarding', tag='출발', sec=5, cap='콩이와 동승을 켜면 멀미 기록에 맞춰 반려견 놀이터 휴게소를 경유지로 넣어요'),
    dict(kind='shot', img='04a-driving-low', tag='주행', sec=5, cap='주행 중에는 급가감속, 쉬지 않고 달린 시간, 차 안 온도로 멀미 위험을 계산해요'),
    dict(kind='shot', img='04-driving-rest', ramp=(22, 58), tag='주행', sec=6, cap='급정거가 이어지면 멀미 위험 점수가 오르고, 음성으로 부드러운 운전을 권해요'),
    dict(kind='shot', img='04c-driving-high', ramp=(58, 81), tag='주행', sec=6, cap='위험 단계가 되면 가까운 반려견 휴게소에서 쉬어 가도록 안내해요'),
    dict(kind='shot', img='01-guard-start', tag='정차', sec=5, cap='휴게소에 정차하면 차 상태를 보여 주고, 펫 지킴이를 켤 수 있어요'),
    dict(kind='shot', img='02-guard-active', tag='정차', sec=6, cap='차 밖 사람에게는 약속 대신 센서로 확인한 사실을 보여 줘요'),
    dict(kind='duo', img='02-guard-active', tag='보호자', sec=6, cap='보호자는 휴대폰으로 차 안 온도와 기록을 실시간으로 확인해요'),
    dict(kind='shot', img='03-guard-danger', tag='위험', sec=5, cap='에어컨이 꺼지면 창문과 실내등을 켜고, 차 화면은 위험 안내로 바뀌어요'),
    dict(kind='duo', img='03-guard-danger', phone='05b-phone-alert', tag='위험', sec=5, cap='보호자 휴대폰에는 바로 돌아오라는 알림과 차 위치가 떠요'),
    dict(kind='shot', img='06-arrival', tag='도착', sec=5, cap='도착하면 말로 컨디션을 남기고, 다음 주행의 쉬는 간격이 반려견에 맞게 바뀌어요'),
    dict(kind='end', sec=7, cap='대회 제공 SDK 5종을 모두 써서, 주행 중 멀미부터 정차 중 안전까지 챙겨요'),
]


VCSS = '''@font-face{font-family:"Asta Sans";src:url("FONTURL")}
*{margin:0;padding:0;box-sizing:border-box}body{width:1080px;height:1920px;overflow:hidden;font-family:"Asta Sans";background:#F7F8FA;color:#131417;word-break:keep-all}
.layer{position:absolute;inset:0}
.vt{position:absolute;left:64px;right:64px;top:150px}
.vt .tag{display:inline-flex;height:52px;padding:0 22px;border-radius:26px;background:#131417;color:#fff;font-size:26px;font-weight:700;align-items:center}
.vt .t{font-size:58px;font-weight:800;letter-spacing:-1.5px;line-height:1.3;margin-top:28px}
.vs{position:absolute;left:40px;width:1000px;top:560px;height:729px;border-radius:28px;overflow:hidden;box-shadow:0 0 0 1px #E0E1E6;background:#fff}
.vs img{display:block;width:1548px;margin-left:-532px;margin-top:-52px}
.vs.full{height:auto}.vs.full img{width:100%;margin:0}
.vfoot{position:absolute;left:64px;right:64px;bottom:120px;font-size:30px;color:#686A72;font-weight:600}
.vp{position:absolute;left:50%;top:1330px;width:220px;margin-left:-110px;border-radius:32px;overflow:hidden;box-shadow:0 0 0 8px #131417}
.vp img{width:100%;display:block}
.vtitle{position:absolute;inset:0;display:flex;flex-direction:column;justify-content:center;padding:0 80px;background:#fff}
.vtitle h1{font-size:150px;font-weight:800;letter-spacing:-5px}
.vtitle h2{font-size:64px;font-weight:700;margin-top:28px;line-height:1.3;letter-spacing:-1.5px}
.vtitle p{font-size:30px;color:#686A72;margin-top:48px}'''


def vbody(scene):
    kind = scene['kind']
    if kind == 'title':
        return ('<div class="vtitle"><h1>펫 드라이브</h1><h2>반려견이 타면<br>차가 먼저 챙깁니다</h2>'
                '<p>Pleos Connect 차량용 앱 (화면 프로토타입)</p></div>')
    img = '00-board' if kind == 'end' else scene['img']
    tag = '정리' if kind == 'end' else scene['tag']
    out = (f'<div class="vt"><span class="tag">{tag}</span><div class="t">{html.escape(scene["cap"])}</div></div>'
           f'<div class="vs{" full" if kind == "end" else ""}"><img src="{scene.get("img_url") or f"{PURL}/{img}.png"}"></div>')
    if kind != 'duo':
        out += '<div class="vfoot">2026 현대자동차그룹 차량용 앱 개발 공모전 출품작</div>'
    if kind == 'duo':
        out += f'<div class="vp"><img src="{PURL}/{scene.get("phone", "05-phone")}.png"></div>'
    return out


SCREENS = os.path.abspath(os.path.join(HERE, '..', 'screens'))
RAMP_STEPS = 10


def ramp_bodies(scene):
    """링 점수가 a에서 b로 차오르는 화면들을 렌더링해 장면 body 목록으로 돌려준다."""
    sys.path.insert(0, SCREENS)
    cwd = os.getcwd(); os.chdir(SCREENS)
    try:
        import build
    finally:
        os.chdir(cwd)
    a, b = scene['ramp']
    out = []
    for k in range(RAMP_STEPS):
        sc = round(a + (b - a) * (k + 1) / RAMP_STEPS)
        html_path = os.path.join(SCREENS, f'_ramp_{scene["img"]}_{k}.html')
        png = os.path.join(FR, f'ramp_{scene["img"]}_{k}.png')
        open(html_path, 'w', encoding='utf-8').write(build.care_frame(scene['img'], sc))
        subprocess.run([CH, '--headless', '--no-sandbox', '--hide-scrollbars', '--window-size=1920,1080',
                        '--virtual-time-budget=3000', f'--screenshot={png}', 'file://' + html_path],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=90)
        os.remove(html_path)
        sc2 = dict(scene); sc2['img_url'] = 'file://' + png
        out.append((vbody if VERTICAL else body)(sc2))
    return out


def page(layers):
    css = (VCSS if VERTICAL else CSS).replace('FONTURL', 'file://' + os.path.abspath(os.path.join(HERE, '..', 'screens', 'fonts', 'AstaSans.ttf')))
    inner = ''.join(f'<div class="layer" style="opacity:{op}">{b}</div>' for b, op in layers)
    return f'<!doctype html><html lang="ko"><head><meta charset="utf-8"><style>{css}</style></head><body>{inner}</body></html>'


def shoot(name, layers):
    path = os.path.join(FR, name + '.html'); out = os.path.join(FR, name + '.jpg')
    open(path, 'w', encoding='utf-8').write(page(layers))
    subprocess.run([CH, '--headless', '--no-sandbox', '--hide-scrollbars', '--allow-file-access-from-files',
                    f'--window-size={W},{H}', '--virtual-time-budget=3000', f'--screenshot={out}', 'file://' + path],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=90)
    return out


def main():
    timeline = []  # (jpg 경로, 반복 횟수)
    bodies = [(vbody if VERTICAL else body)(s) for s in SCENES]
    ramps = [ramp_bodies(s) if s.get('ramp') else None for s in SCENES]
    for i, s in enumerate(SCENES):
        used = 0
        if ramps[i]:
            for k, rb in enumerate(ramps[i][1:], 1):
                timeline.append((shoot(f's{i:02d}_r{k}', [(rb, 1)]), 1)); used += 1
        still = shoot(f's{i:02d}', [(bodies[i], 1)])
        hold = s['sec'] * FPS - used - (FADE if i + 1 < len(SCENES) else 0)
        timeline.append((still, hold))
        if i + 1 < len(SCENES):
            nxt = ramps[i + 1][0] if ramps[i + 1] else bodies[i + 1]
            for k in range(1, FADE + 1):
                a = k / (FADE + 1)
                timeline.append((shoot(f's{i:02d}_t{k}', [(bodies[i], round(1 - a, 3)), (nxt, round(a, 3))]), 1))
    out = os.path.join(HERE, 'pet-drive-shortform.webm' if VERTICAL else 'pet-drive-demo.webm')
    stream = os.path.join(FR, 'all.mjpeg')   # 이 ffmpeg 빌드는 파이프 입력을 못 받아 파일로 넘긴다
    with open(stream, 'wb') as f:
        for jpg, n in timeline:
            data = open(jpg, 'rb').read()
            for _ in range(n):
                f.write(data)
    r = subprocess.run([FF, '-y', '-f', 'image2pipe', '-framerate', str(FPS), '-c:v', 'mjpeg', '-i', stream,
                        '-c:v', 'libvpx-vp9', '-b:v', '0', '-crf', '32', '-pix_fmt', 'yuv420p', out],
                       stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    os.remove(stream)
    if r.returncode != 0:
        print(r.stderr.decode(errors='ignore')[-2000:]); raise SystemExit(1)
    total = sum(n for _, n in timeline) / FPS
    print(f'ok {out} {total:.1f}s')


if __name__ == '__main__':
    main()
