# 공통 차량 화면 틀(상태바, 주행 정보, GNB)을 만들어 각 화면 HTML을 생성한다.
PAW = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="{c}">'
       '<ellipse cx="6" cy="9" rx="2.2" ry="2.8"/><ellipse cx="10" cy="5.5" rx="2.2" ry="2.8"/>'
       '<ellipse cx="14.5" cy="5.5" rx="2.2" ry="2.8"/><ellipse cx="18.5" cy="9" rx="2.2" ry="2.8"/>'
       '<path d="M12.2 11c-3.2 0-6.2 3.6-6.2 6.4 0 1.9 1.4 2.9 3.1 2.9 1.3 0 2-.6 3.1-.6s1.8.6 3.1.6c1.7 0 3.1-1 3.1-2.9 0-2.8-3-6.4-6.2-6.4z"/></svg>')


def paw(s=28, c="#fff"):
    return PAW.format(s=s, c=c)


# 원근감 있는 도로와 뒤에서 본 내 차. 가이드의 Driving view 구성을 단순화했다.
def scene(moving):
    dash = 'stroke-dasharray="26 30"' if moving else ''
    return f'''<svg class="scene" width="640" height="660" viewBox="0 0 640 660">
  <polygon points="250,0 390,0 600,660 40,660" fill="#F7F8FA"/>
  <line x1="297" y1="0" x2="215" y2="660" stroke="#E0E1E6" stroke-width="6" {dash}/>
  <line x1="343" y1="0" x2="425" y2="660" stroke="#E0E1E6" stroke-width="6" {dash}/>
  <line x1="250" y1="0" x2="40" y2="660" stroke="#C7C9CE" stroke-width="4"/>
  <line x1="390" y1="0" x2="600" y2="660" stroke="#C7C9CE" stroke-width="4"/>
  <g transform="translate(320 520)">
    <rect x="-74" y="-46" width="148" height="92" rx="26" fill="#44464E"/>
    <rect x="-56" y="-70" width="112" height="44" rx="16" fill="#686A72"/>
    <rect x="-64" y="2" width="30" height="10" rx="5" fill="#A4A8AE"/>
    <rect x="34" y="2" width="30" height="10" rx="5" fill="#A4A8AE"/>
  </g>
</svg>'''


ICONS = ('<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#313236" stroke-width="2" stroke-linecap="round">'
         '<path d="M4 18h2M9 15v3M14 11v7M19 7v11"/></svg>'
         '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#313236" stroke-width="2" stroke-linecap="round">'
         '<path d="M12 4a7 7 0 0 0-7 7v4l-2 3h18l-2-3v-4a7 7 0 0 0-7-7zM10 21h4"/></svg>')


def page(body, speed="0", gear="P", time="14:02", cl="22.0", cr="22.0", css="screens.css"):
    gears = "".join(f"<b>{g}</b>" if g == gear else g for g in "PRND")
    return f'''<!doctype html><html lang="ko"><head><meta charset="utf-8"><link rel="stylesheet" href="base.css"><link rel="stylesheet" href="{css}"></head><body>
<div class="ivi">
  <div class="statusbar">{ICONS}<span>{time}</span></div>
  <div class="driving">
    <div class="speed">{speed}</div><div class="unit">km/h</div>
    <div class="gear">{gears}</div>
    {scene(speed != "0")}
  </div>
  {body}
  <div class="gnb">
    <div class="climate"><span>‹</span><div>{cl}<small>AUTO</small></div><span>›</span></div>
    <div class="apps"><div class="app"></div><div class="app"></div><div class="app"></div><div class="app me">{paw(32)}</div><div class="app"></div><div class="app"></div><div class="app"></div></div>
    <div class="climate"><span>‹</span><div>{cr}<small>AUTO</small></div><span>›</span></div>
  </div>
</div></body></html>'''
