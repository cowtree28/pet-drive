import sys
sys.path.insert(0, '.')
from _chrome import page, paw
from _icons import icon
from _ring import ring

G = "#686A72"
GREEN, RED, DARK, AMBER = "#02C265", "#FE3D16", "#313236", "#F59E0B"
SPEAKER = ('<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#313236" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
           '<path d="M4 9v6h4l5 4V5L8 9H4z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/></svg>')


def act(ic, title, sub=""):
    s = f'<span class="sub">{sub}</span>' if sub else ''
    return f'<li>{icon(ic, 34, G)}<div><b>{title}</b>{s}</div></li>'


def temp(n):
    return f'<div class="rt">{n}<sup>℃</sup></div>'


# ① 정차 직후
s1 = f'''<div class="win full"><div class="r1">
  <div class="left">
    <div class="who"><b>콩이</b><span>중형견, 멀미 있음</span></div>
    {ring(460, 34, 0, GREEN, temp("24.5") + '<div class="rl">지금 차 안</div>')}
    <div class="facts">
      <div>{icon("sun", 30, G)}<span>바깥</span><b>33℃</b></div>
      <div>{icon("fuel", 30, G)}<span>연료</span><b>48%</b></div>
      <div>{icon("seat", 30, G)}<span>뒷좌석</span><b>탑승</b></div>
    </div>
  </div>
  <div class="right">
    <h2>펫 지킴이</h2>
    <ul class="acts">
      {act("ac", "에어컨 22℃ 유지", "20분마다 연장 확인")}
      {act("lock", "문 잠금")}
      {act("light", "무드등 초록")}
      {act("screen", "바깥 안내 화면")}
    </ul>
    <div class="btn filled start">펫 지킴이 켜기</div>
  </div>
</div></div>'''

# ② 작동 중 — 바깥 사람에게 사실만
s2 = f'''<div class="win full"><div class="r2">
  <div class="main">
    <div class="state"><i></i>반려견 탑승 중 · 에어컨 켜짐</div>
    <div class="hero">
      {ring(460, 34, 0.7, GREEN, temp("22.1") + '<div class="rl">반려견이 있는 차 안</div><div class="rn">다음 확인까지 14분</div>')}
      <div class="aside">
        <div class="out"><span>바깥</span><b>33<sup>℃</sup></b></div>
        <div class="kv"><span>보호자가 떠난 지</span><b>6분</b></div>
        <div class="kv"><span>보호자</span><b>휴대폰 확인 중</b></div>
      </div>
    </div>
  </div>
  <div class="side">
    <div class="side-h">보호자 연락처</div>
    <div class="tel">0504-0000-2831</div>
    <div class="side-h rec-h">차 안 온도 기록</div>
    <ol class="rec">
      <li><span>13:28</span><b>22.1℃</b></li><li><span>13:27</span><b>22.1℃</b></li>
      <li><span>13:26</span><b>22.0℃</b></li><li><span>13:25</span><b>22.2℃</b></li>
    </ol>
  </div>
</div></div>'''

# ③ 위험
s3 = f'''<div class="win full"><div class="r2 danger">
  <div class="main">
    <div class="state"><i></i>에어컨 멈춤 · 차 안 온도 상승</div>
    <div class="hero">
      {ring(460, 34, 1, RED, temp("31.0") + '<div class="rl">반려견이 있는 차 안</div><div class="rn">보호자를 부르는 중</div>', track="#FFE1DA")}
      <div class="aside">
        <div class="out"><span>바깥</span><b>33<sup>℃</sup></b></div>
        <div class="kv"><span>보호자가 떠난 지</span><b>14분</b></div>
        <div class="kv"><span>보호자 전화</span><b>3번</b></div>
      </div>
    </div>
  </div>
  <div class="side">
    <div class="side-h">자동 대응</div>
    <ul class="acts">
      {act("window", "창문 3cm 열림")}
      {act("light", "실내등 켜짐")}
      {act("call", "보호자 호출 중")}
    </ul>
    <div class="callbox"><span>보호자 연락처</span><b>0504-0000-2831</b><span class="sm">받지 않으면 119에 신고해 주세요</span></div>
  </div>
</div></div>'''

# ④ 주행 중 — 멀미 케어 (단계별)
def care(score, level, color, did, nxt_place, nxt_time, voice, km, rem):
    lis = "".join(f'<li>{icon(i, 30, G)}<b>{t}</b></li>' for i, t in did)
    return f'''<div class="win a"><div class="map">
  <div class="side" style="left:-40px;top:420px;width:760px;transform:rotate(-14deg)"></div>
  <div class="side" style="left:-60px;top:760px;width:500px;transform:rotate(22deg)"></div>
  <div class="road"><div class="route"></div><div class="pin"></div></div>
  <div class="pinlabel">덕평자연휴게소</div>
  <div class="tbt"><svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#131417" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M8 20V9h9M13 4l5 5-5 5"/></svg><div><div class="d">{km}km</div><div class="r">휴게소 진입</div></div></div>
  <div class="eta"><span>도착 16:20</span><span>남은 거리 {rem}km</span></div>
</div></div>
<div class="win b"><div class="r4 lv-{level}">
  <div class="who">{paw(28, "#131417")}<b>콩이 멀미 위험</b></div>
  {ring(330, 30, score/100, color, f'<div class="lv">{level}</div><div class="rl">100점 중 {score}</div>')}
  <ul class="did">{lis}</ul>
  <div class="next"><span>다음 휴식</span><b>{nxt_place}</b><em>{nxt_time}</em></div>
  <div class="voice">{SPEAKER}“{voice}”</div>
</div></div>'''

CARE_ARGS = {
    "04a-driving-low": dict(score=22, level="편안", color=GREEN, did=[("ac", "뒷좌석 23℃ · 약풍")], nxt_place="덕평자연휴게소", nxt_time="40분 뒤", voice="40분 뒤 반려견 놀이터가 있는 휴게소에서 쉬어요", km=64, rem=220),
    "04-driving-rest": dict(score=58, level="주의", color=AMBER, did=[("ac", "뒷좌석 23℃ · 약풍"), ("window", "뒷창문 2cm 열림")], nxt_place="덕평자연휴게소", nxt_time="15분 뒤", voice="급정거가 잦았어요. 부드럽게 운전해 주세요", km=16, rem=172),
    "04c-driving-high": dict(score=81, level="위험", color=RED, did=[("ac", "뒷좌석 23 → 22℃"), ("window", "뒷창문 2cm 열림")], nxt_place="덕평자연휴게소", nxt_time="8분 뒤", voice="콩이가 힘들어해요. 8분 뒤 휴게소에서 쉬어 가요", km=12, rem=168),
}
CARE_KW = {"04a-driving-low": dict(speed="100", gear="D", time="12:40", cl="23.0", cr="23.0"),
           "04-driving-rest": dict(speed="62", gear="D", time="13:05", cl="23.0", cr="23.0"),
           "04c-driving-high": dict(speed="88", gear="D", time="13:12", cl="23.0", cr="22.0")}


def level_of(score):
    return ("편안", GREEN) if score < 40 else ("주의", AMBER) if score < 70 else ("위험", RED)


def care_frame(name, score):
    """영상용: name 화면의 내용을 쓰되 링 점수만 바꾼 HTML."""
    a = dict(CARE_ARGS[name]); a["score"] = score; a["level"], a["color"] = level_of(score)
    return page(care(**a), css="ring.css", **CARE_KW[name])


s4a = care(**CARE_ARGS["04a-driving-low"])
s4 = care(**CARE_ARGS["04-driving-rest"])
s4c = care(**CARE_ARGS["04c-driving-high"])

# ⓪ 출발 — 동승 모드 켜기
s0 = f'''<div class="win full"><div class="r0">
  <div class="left">
    <div class="who"><b>콩이와 동승</b><span>중형견, 멀미 있음</span></div>
    {ring(460, 34, 1, GREEN, '<div class="rt" style="font-size:140px">80<small>분</small></div><div class="rl">쉬는 간격</div>')}
    <p class="why">멀미 있음 · 기본보다 30분 짧게</p>
  </div>
  <div class="right">
    <h2>강릉까지 휴식 경로</h2>
    <ol class="plan">
      <li class="done"><span>11:58</span><b>지금 출발</b></li>
      <li><span>13:20</span><b>덕평자연휴게소</b><em>{paw(22, "#131417")}반려견 놀이터</em></li>
      <li><span>14:50</span><b>평창휴게소</b><em>15분 휴식</em></li>
      <li><span>16:20</span><b>강릉 도착</b></li>
    </ol>
    <div class="cab">{icon("ac", 30, G)}<span>뒷좌석 23℃ · 약풍</span></div>
    <div class="btn filled start">펫 드라이브로 출발</div>
  </div>
</div></div>'''

# ⑥ 도착 후 — 말로 컨디션 기록, 다음 주행 조정
MIC = ('<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
       '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/></svg>')
s6 = f'''<div class="win full"><div class="r6">
  <div class="left">
    <div class="who"><b>콩이와 강릉 도착</b><span>4시간 22분, 휴식 2번</span></div>
    {ring(460, 34, 0.81, RED, '<div class="rt" style="font-size:140px">81</div><div class="rl">오늘 최고 멀미 위험</div><div class="rn" style="color:#FE3D16">13:12 영동고속도로 정체 구간</div>', track="#EDEEF2")}
  </div>
  <div class="right">
    <h2>콩이 오늘 어땠나요?</h2>
    <div class="ask">{MIC}<span>“콩이 오늘 멀미 조금 했어”</span></div>
    <div class="heard"><span>기록</span><b>멀미 조금</b></div>
    <div class="adj">
      <div class="k">다음 주행부터</div>
      <div class="row"><span>쉬는 간격</span><b>80분 → 70분</b></div>
      <div class="row"><span>휴게소 경유지</span><b>70분마다</b></div>
    </div>
    <div class="btn filled start">확인</div>
  </div>
</div></div>'''

if __name__ == "__main__":
  for name, body, kw in [
    ("00a-boarding", s0, dict(time="11:57")),
    ("06-arrival", s6, dict(time="16:20")),
    ("01-guard-start", s1, dict(time="13:21")),
    ("02-guard-active", s2, dict(time="13:28")),
    ("03-guard-danger", s3, dict(time="13:36", cl="OFF", cr="OFF")),
    ("04-driving-rest", s4, dict(speed="62", gear="D", time="13:05", cl="23.0", cr="23.0")),
    ("04a-driving-low", s4a, dict(speed="100", gear="D", time="12:40", cl="23.0", cr="23.0")),
    ("04c-driving-high", s4c, dict(speed="88", gear="D", time="13:12", cl="23.0", cr="22.0")),
]:
    open(f"{name}.html", "w", encoding="utf-8").write(page(body, css="ring.css", **kw))
  print("ok")
