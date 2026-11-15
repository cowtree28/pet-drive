# ② 펫 지킴이 작동 중 화면 — 서로 다른 레퍼런스 10개 시안
import sys, os
sys.path.insert(0, '.')
from _chrome import page, paw

os.makedirs('variants', exist_ok=True)

V = {}

# A. 고속도로 녹색 안내표지판
V['A-highway-sign'] = ('''
.win.full{background:#00704A;box-shadow:none}
.v{position:absolute;inset:18px;border:6px solid #fff;border-radius:18px;color:#fff;display:flex;flex-direction:column;padding:48px 64px}
.v .top{font-size:34px;font-weight:700;display:flex;align-items:center;gap:14px}
.v .num{font-size:330px;font-weight:800;line-height:.9;letter-spacing:-14px;margin-top:auto;font-feature-settings:normal}
.v .num sup{font-size:110px;vertical-align:150px;letter-spacing:0;margin-left:10px}
.v .msg{font-size:72px;font-weight:800;margin-top:20px}
.v .row{margin-top:auto;display:flex;border-top:4px solid #fff;padding-top:26px}
.v .row div{flex:1;font-size:36px;font-weight:700}
.v .row div+div{border-left:4px solid #fff;padding-left:32px}
''', f'''<div class="v"><div class="top">{paw(40)}펫 지킴이</div>
<div class="num">22<sup>℃</sup></div><div class="msg">보호자가 곧 돌아옵니다</div>
<div class="row"><div>에어컨 켜짐</div><div>문 잠김</div><div>6분 / 20분</div></div></div>''')

# B. 공항 출발 안내판
V['B-airport-board'] = ('''
.win.full{background:#0E1013;box-shadow:none}
.v{padding:56px 64px;color:#F2F2F2;height:100%;display:flex;flex-direction:column}
.v .h{font-size:28px;color:#8A8F98;font-weight:600}
.v .msg{font-size:84px;font-weight:800;margin-top:14px;letter-spacing:-1px}
.v table{width:100%;border-collapse:collapse;margin-top:auto;font-size:40px}
.v th{font-size:22px;color:#8A8F98;text-align:left;font-weight:600;padding-bottom:12px}
.v td{padding:20px 0;border-top:2px solid #23262B;font-weight:700}
.v td.y{color:#FFCC00}
.v td.ok{color:#2BD67B;text-align:right}
''', '''<div class="v"><div class="h">펫 지킴이 상황판 · 14:08</div><div class="msg">보호자가 곧 돌아옵니다</div>
<table><tr><th>항목</th><th>값</th><th style="text-align:right">상태</th></tr>
<tr><td>차 안 온도</td><td class="y">22.1℃</td><td class="ok">정상</td></tr>
<tr><td>에어컨</td><td class="y">22℃ 유지</td><td class="ok">작동</td></tr>
<tr><td>문</td><td class="y">잠김</td><td class="ok">정상</td></tr>
<tr><td>보호자가 떠난 지</td><td class="y">6분</td><td class="ok">최대 20분</td></tr></table></div>''')

# C. 병원 환자 모니터
V['C-vital-monitor'] = ('''
.win.full{background:#06080A;box-shadow:none}
.v{height:100%;display:grid;grid-template-columns:1fr 440px;color:#fff}
.v .wave{padding:48px 0 48px 56px;display:flex;flex-direction:column}
.v .lab{font-size:26px;color:#2BD67B;font-weight:700}
.v svg{margin-top:24px}
.v .msg{font-size:60px;font-weight:800;margin-top:auto}
.v .side{border-left:2px solid #1B1F24;padding:48px 44px;display:flex;flex-direction:column;gap:30px}
.v .big{color:#2BD67B;font-size:150px;font-weight:700;line-height:.9;letter-spacing:-6px;font-feature-settings:normal}
.v .big small{font-size:50px;letter-spacing:0}
.v .k{font-size:22px;color:#7C838C}
.v .s{font-size:40px;font-weight:700}
''', '''<div class="v"><div class="wave"><div class="lab">차 안 온도 · 최근 6분</div>
<svg width="720" height="420" viewBox="0 0 780 420"><g stroke="#13201A" stroke-width="2">
<line x1="0" y1="60" x2="780" y2="60"/><line x1="0" y1="180" x2="780" y2="180"/><line x1="0" y1="300" x2="780" y2="300"/></g>
<text x="0" y="50" fill="#FE3D16" font-size="20" font-family="Asta Sans">27℃ 알림</text>
<line x1="0" y1="60" x2="780" y2="60" stroke="#FE3D16" stroke-dasharray="6 8"/>
<polyline points="0,150 130,215 260,262 390,286 520,298 650,292 760,292" fill="none" stroke="#2BD67B" stroke-width="5" stroke-linejoin="round"/>
<circle cx="760" cy="292" r="9" fill="#2BD67B"/></svg>
<div class="msg">보호자가 곧 돌아옵니다</div></div>
<div class="side"><div><div class="k">차 안</div><div class="big">22.1<small>℃</small></div></div>
<div><div class="k">에어컨</div><div class="s">켜짐</div></div><div><div class="k">문</div><div class="s">잠김</div></div>
<div><div class="k">떠난 지</div><div class="s">6분 / 20분</div></div></div></div>''')

# D. iOS Live Activity 링
V['D-ring'] = ('''
.v{height:100%;display:flex;align-items:center;gap:72px;padding:0 80px}
.v .ring{position:relative;width:560px;height:560px;flex:none}
.v .ring svg{position:absolute;inset:0;transform:rotate(-90deg)}
.v .ring .c{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center}
.v .ring .n{font-size:200px;font-weight:700;letter-spacing:-8px;line-height:.9;font-feature-settings:normal}
.v .ring .n sup{font-size:60px;letter-spacing:0;color:var(--basic-400);vertical-align:90px}
.v .ring .t{font-size:30px;color:var(--basic-500);margin-top:10px}
.v .msg{font-size:72px;font-weight:800;line-height:1.2;letter-spacing:-1px}
.v ul{list-style:none;margin-top:40px}
.v li{font-size:34px;font-weight:600;padding:14px 0;color:var(--basic-700);display:flex;gap:16px;align-items:center}
.v li i{width:14px;height:14px;border-radius:50%;background:var(--active)}
''', '''<div class="v"><div class="ring"><svg width="560" height="560"><circle cx="280" cy="280" r="250" stroke="#EDEEF2" stroke-width="40" fill="none"/>
<circle cx="280" cy="280" r="250" stroke="#02C265" stroke-width="40" fill="none" stroke-linecap="round" stroke-dasharray="1571" stroke-dashoffset="1100"/></svg>
<div class="c"><div class="n">22<sup>℃</sup></div><div class="t">20분 중 6분 지남</div></div></div>
<div><div class="msg">보호자가<br>곧 돌아옵니다</div><ul><li><i></i>에어컨 켜짐</li><li><i></i>문 잠김</li><li><i></i>보호자가 휴대폰으로 확인 중</li></ul></div></div>''')

# E. LED 도트 전광판
V['E-led-dot'] = ('''
.win.full{background:#141414;box-shadow:none}
.v{height:100%;display:flex;flex-direction:column;justify-content:center;padding:0 72px;gap:40px}
.v .dot{color:#FFB000;-webkit-mask-image:radial-gradient(circle,#000 52%,transparent 58%);-webkit-mask-size:9px 9px;font-weight:800}
.v .l1{font-size:120px;letter-spacing:4px}
.v .l2{font-size:330px;line-height:.9;letter-spacing:0;font-feature-settings:normal}
.v .l3{font-size:64px;color:#3BE07A}
.v .frame{border:3px solid #2A2A2A;border-radius:12px;padding:48px 56px}
''', '''<div class="v"><div class="frame"><div class="dot l1">보호자 곧 옵니다</div><div class="dot l2">22℃</div><div class="dot l3">에어컨 켜짐 · 문 잠김 · 6분째</div></div></div>''')

# F. Braun 온도계 다이얼
V['F-dial'] = ('''
.win.full{background:var(--basic-50)}
.v{height:100%;display:grid;grid-template-columns:640px 1fr;align-items:center;padding:0 64px;gap:40px}
.v .msg{font-size:68px;font-weight:800;line-height:1.2;letter-spacing:-1px}
.v .sub{font-size:30px;color:var(--basic-500);margin-top:28px;line-height:1.6}
''', '''<div class="v"><svg width="640" height="640" viewBox="0 0 640 640">
<circle cx="320" cy="320" r="310" fill="#fff" stroke="#E0E1E6" stroke-width="2"/>
<path d="M 129.1 510.9 A 270 270 0 0 1 403.4 63.2" fill="none" stroke="#02C265" stroke-width="30"/>
<path d="M 419.4 69.0 A 270 270 0 0 1 510.9 510.9" fill="none" stroke="#FE3D16" stroke-width="30"/>
<g font-family="Asta Sans" font-size="30" fill="#686A72" text-anchor="middle"><text x="156" y="494">10</text><text x="156" y="166">20</text><text x="484" y="166">30</text><text x="484" y="494">40</text></g>
<line x1="320" y1="320" x2="224.7" y2="132.9" stroke="#131417" stroke-width="10" stroke-linecap="round"/>
<circle cx="320" cy="320" r="24" fill="#131417"/>
<text x="320" y="500" text-anchor="middle" font-family="Asta Sans" font-size="110" font-weight="700" fill="#131417">22℃</text>
<text x="320" y="560" text-anchor="middle" font-family="Asta Sans" font-size="26" fill="#686A72">27℃부터 위험</text></svg>
<div><div class="msg">보호자가<br>곧 돌아옵니다</div><div class="sub">에어컨 켜짐, 문 잠김<br>보호자가 떠난 지 6분</div></div></div>''')

# G. 스위스 타이포 포스터
V['G-swiss'] = ('''
.v{height:100%;position:relative;overflow:hidden;padding:56px 64px}
.v .grid{display:grid;grid-template-columns:repeat(3,1fr);gap:32px;border-top:6px solid var(--basic-900);padding-top:20px}
.v .grid div{font-size:26px;line-height:1.4;color:var(--basic-700)}
.v .grid b{display:block;font-size:30px;color:var(--basic-900)}
.v .msg{font-size:84px;font-weight:800;letter-spacing:-2px;line-height:1.1;margin-top:48px;width:520px}
.v .huge{position:absolute;right:40px;bottom:-120px;font-size:640px;font-weight:800;letter-spacing:-50px;line-height:1;color:var(--basic-900);font-feature-settings:normal}
.v .huge span{color:var(--negative);font-size:200px;letter-spacing:0;vertical-align:360px;margin-left:20px}
''', '''<div class="v"><div class="grid"><div><b>펫 지킴이</b>작동 중</div><div><b>에어컨</b>22℃ 유지</div><div><b>문</b>잠김 · 6분째</div></div>
<div class="msg">보호자가 곧 돌아옵니다</div><div class="huge">22<span>℃</span></div></div>''')

# H. 신호등
V['H-traffic-light'] = ('''
.v{height:100%;display:flex;align-items:center;gap:80px;padding:0 90px}
.v .lamp{width:260px;flex:none;background:#1D1E21;border-radius:130px;padding:28px;display:flex;flex-direction:column;gap:26px}
.v .lamp i{display:block;width:204px;height:204px;border-radius:50%;background:#2E3035}
.v .lamp i.on{background:#02C265;box-shadow:0 0 0 10px rgba(2,194,101,.18)}
.v .t{font-size:40px;font-weight:700;color:var(--active)}
.v .msg{font-size:84px;font-weight:800;letter-spacing:-1.5px;line-height:1.15;margin-top:16px}
.v .n{font-size:200px;font-weight:700;letter-spacing:-8px;line-height:1;margin-top:30px;font-feature-settings:normal}
.v .n small{font-size:56px;letter-spacing:0;color:var(--basic-400)}
.v .s{font-size:30px;color:var(--basic-500);margin-top:16px}
''', '''<div class="v"><div class="lamp"><i></i><i></i><i class="on"></i></div>
<div><div class="t">안전해요</div><div class="msg">보호자가 곧 돌아옵니다</div><div class="n">22<small>℃</small></div><div class="s">에어컨 켜짐, 문 잠김, 떠난 지 6분</div></div></div>''')

# I. 감열지 영수증
V['I-receipt'] = ('''
.win.full{background:#E9EAEE;box-shadow:none}
.v{height:100%;display:grid;grid-template-columns:1fr 470px;gap:56px;padding:56px 64px;align-items:start}
.v .msg{font-size:84px;font-weight:800;letter-spacing:-1.5px;line-height:1.15}
.v .n{font-size:230px;font-weight:700;letter-spacing:-10px;line-height:1;margin-top:40px;font-feature-settings:normal}
.v .n small{font-size:64px;letter-spacing:0;color:var(--basic-500)}
.v .s{font-size:30px;color:var(--basic-500);margin-top:12px}
.v .rc{background:#fff;padding:40px 40px 56px;font-size:26px;line-height:1.75;color:#222;
 -webkit-mask:linear-gradient(#000,#000) top/100% calc(100% - 16px) no-repeat, conic-gradient(from -45deg at bottom,#0000,#000 1deg 89deg,#0000 90deg) bottom/32px 16px repeat-x}
.v .rc h4{font-size:28px;text-align:center;border-bottom:2px dashed #999;padding-bottom:14px;margin-bottom:12px}
.v .rc div{display:flex;justify-content:space-between}
.v .rc .f{border-top:2px dashed #999;margin-top:12px;padding-top:12px;font-weight:700}
''', '''<div class="v"><div><div class="msg">보호자가<br>곧 돌아옵니다</div><div class="n">22.1<small>℃</small></div><div class="s">차 안 온도를 1분마다 기록하고 있어요</div></div>
<div class="rc"><h4>펫 지킴이 온도 기록</h4><div><span>14:02</span><span>24.5℃</span></div><div><span>14:03</span><span>23.4℃</span></div><div><span>14:04</span><span>22.6℃</span></div><div><span>14:05</span><span>22.2℃</span></div><div><span>14:06</span><span>22.0℃</span></div><div><span>14:07</span><span>22.1℃</span></div><div><span>14:08</span><span>22.1℃</span></div>
<div class="f"><span>에어컨 · 문</span><span>켜짐 · 잠김</span></div></div></div>''')

# J. 날씨 앱 비교
V['J-weather-compare'] = ('''
.v{height:100%;display:flex;flex-direction:column;padding:56px 72px}
.v .msg{font-size:76px;font-weight:800;letter-spacing:-1.5px}
.v .cmp{display:grid;grid-template-columns:1fr 1fr;gap:28px;margin-top:auto}
.v .c{border-radius:28px;padding:40px 48px;height:560px;display:flex;flex-direction:column}
.v .c.out{background:#FFF1EA}
.v .c.in{background:#EAF3FF}
.v .c .k{font-size:32px;font-weight:700}
.v .c.out .k{color:#C2410C}.v .c.in .k{color:#0064FF}
.v .c .n{font-size:240px;font-weight:700;letter-spacing:-10px;line-height:1;margin-top:auto;font-feature-settings:normal}
.v .c .n small{font-size:64px;letter-spacing:0;color:var(--basic-500)}
.v .c .s{font-size:28px;color:var(--basic-500);margin-top:8px}
''', '''<div class="v"><div class="msg">보호자가 곧 돌아옵니다</div><div class="cmp">
<div class="c out"><div class="k">바깥</div><div class="n">33<small>℃</small></div><div class="s">휴게소 주차장</div></div>
<div class="c in"><div class="k">콩이가 있는 차 안</div><div class="n">22<small>℃</small></div><div class="s">에어컨 켜짐, 문 잠김, 6분째</div></div></div></div>''')

for name, (css, body) in V.items():
    html = page(f'<div class="win full">{body}</div>', time="14:08").replace(
        '<link rel="stylesheet" href="screens.css">', f'<style>{css}</style>').replace('href="base.css"', 'href="../base.css"')
    open(f'variants/{name}.html', 'w', encoding='utf-8').write(html)
print(len(V))
