"""공식 접수양식 02의 1~8번 칸을 기획서 마크다운으로 채운다.

사용: python3 tools/fill_form.py docs/기획서-초안.md docs/양식-원본.docx docs/접수양식-작성본.docx
"""
import re, sys, zipfile, os
from xml.sax.saxutils import escape
md = open(sys.argv[1], encoding='utf-8').read()
src = zipfile.ZipFile(sys.argv[2])
x = src.read('word/document.xml').decode('utf-8')

# 기획서 본문 1~8 절 나누기
secs = {}
for m in re.finditer(r'^## (\d)\. [^\n]*\n(.*?)(?=^## \d\. |\Z)', md, re.S | re.M):
    secs[int(m.group(1))] = m.group(2).strip()

PPR = '<w:pPr><w:spacing w:after="40" w:line="300" w:lineRule="auto"/></w:pPr>'
PPR_H = '<w:pPr><w:spacing w:before="160" w:after="60" w:line="300" w:lineRule="auto"/></w:pPr>'

def runs(text, bold_all=False):
    text = text.replace('`', '')
    out = []
    for i, part in enumerate(re.split(r'\*\*(.+?)\*\*', text)):
        if not part: continue
        b = bold_all or i % 2 == 1
        rpr = '<w:rPr><w:b/><w:bCs/></w:rPr>' if b else ''
        out.append(f'<w:r>{rpr}<w:t xml:space="preserve">{escape(part)}</w:t></w:r>')
    return ''.join(out)

def para(text, head=False):
    return f'<w:p>{PPR_H if head else PPR}{runs(text, head)}</w:p>'

def convert(body):
    ps = []; incode = False; header_skipped = False
    for line in body.split('\n'):
        s = line.rstrip()
        if s.startswith('```'): incode = not incode; continue
        if incode:
            if s.strip(): ps.append(para(s))
            continue
        if not s.strip(): header_skipped = False; continue
        if s.startswith('### '): ps.append(para(s[4:], head=True)); continue
        if not s.lstrip().startswith('|'):
            header_skipped = False          # 표가 끝나면 다음 표의 머리행을 다시 건너뛴다
        if s.lstrip().startswith('|'):
            cells = [c.strip() for c in s.strip().strip('|').split('|')]
            if all(re.fullmatch(r':?-+:?', c) for c in cells): continue
            if not header_skipped: header_skipped = True; continue   # 표 머리행 생략
            ind = '    ' if line.startswith('  ') else ''
            if len(cells) == 2: t = f'{cells[0]}: {cells[1]}'
            else: t = f'{cells[0]}: {cells[1]} ({", ".join(cells[2:])})'
            ps.append(para(f'{ind}• {t}')); continue
        m = re.match(r'^(\s*)- (.*)', s)
        if m:
            lvl = len(m.group(1)) // 2
            ps.append(para(('    ' * lvl) + ('• ' if lvl == 0 else '– ') + m.group(2))); continue
        s = re.sub(r'^\s*> ?', '', s)
        ps.append(para(s.strip()))
    return ''.join(ps)

# 칸에 넣을 그림: {칸 번호: [(파일, 가로px, 세로px, 캡션)]}
PNG_DIR = os.path.join(os.path.dirname(os.path.abspath(sys.argv[1])), '..', 'design', 'png')
SECTION_IMAGES = {
    # 실제 앱을 Pleos Connect 에뮬레이터에서 실행한 화면 (design/png/app/)
    3: [('app/03_주행_위험_경유지추가.png', 1920, 1080, 'Pleos 에뮬레이터 실행 화면: 급정거가 이어져 위험 단계(80점)가 되자 반려견 휴게소가 내비 경유지에 추가됐습니다'),
        ('app/04_펫지킴이_위험대응.png', 1920, 1080, '펫 지킴이: 에어컨이 꺼져 차 안 온도가 오르자 위험 대응으로 바뀌고 보호자를 부릅니다')],
    7: [('app/02_내비_휴게소경유지.png', 1920, 1080, 'Pleos 내비: 펫 드라이브가 넣은 휴게소 경유지가 포함된 강릉 경로'),
        ('app/05_도착_컨디션기록.png', 1920, 1080, '도착 화면: 컨디션 기록에 따라 다음 주행의 쉬는 간격이 80분에서 70분으로 바뀝니다')],
}
_media = []   # (rId, 파일)


def img_para(fname, w, h, caption):
    rid = f'rIdPet{len(_media) + 1}'
    _media.append((rid, fname))
    cx = int(6.6 * 914400); cy = int(cx * h / w); n = 9000 + len(_media)
    pic = (f'<w:p><w:pPr><w:spacing w:before="120" w:after="0"/><w:jc w:val="center"/></w:pPr><w:r><w:drawing>'
        f'<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/><wp:docPr id="{n}" name="{fname}"/>'
        '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        f'<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="{n}" name="{fname}"/><pic:cNvPicPr/></pic:nvPicPr>'
        f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>'
        '</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>')
    if caption:
        pic += f'<w:p><w:pPr><w:spacing w:after="80"/><w:jc w:val="center"/></w:pPr><w:r><w:rPr><w:color w:val="686A72"/><w:sz w:val="18"/></w:rPr><w:t xml:space="preserve">{escape(caption)}</w:t></w:r></w:p>'
    return pic


# sdt가 든 문단을 순서대로 교체 (칸 k = 기획서 k절)
pat = re.compile(r'<w:p [^>]*>(?:(?!</w:p>).)*?<w:sdt>.*?</w:sdt></w:p>', re.S)
boxes = list(pat.finditer(x))
assert len(boxes) == 8, len(boxes)
for k in range(8, 0, -1):
    m = boxes[k - 1]
    body = secs.get(k, '')
    if k == 7:
        body = ('- 02_설명보드_펫드라이브.png: 화면 시안과 사용한 Pleos SDK를 한 장에 정리 (아래 그림과 같음)\n'
                '- 화면시안 폴더 PNG 10장: 출발(동승 모드), 멀미 케어 주행(편안·주의·위험), 펫 지킴이 켜기·작동·위험 대응, 보호자 휴대폰(실시간 확인, 위험 알림), 도착 후 컨디션 기록\n'
                '- 03_시연영상_펫드라이브.webm: 시연 영상 65초 (화면 프로토타입)\n'
                '- 04_기획서_읽기용_펫드라이브.pdf: 접수양식 02(기획서)를 읽기 쉽게 정리한 PDF\n'
                '- GitHub 저장소: 멀미 위험 점수 로직(Java)과 테스트 23개, 주행 기록 재생 데모, 멀미 케어 시뮬레이터, Pleos 권한 선언 매니페스트\n'
                '- 저장소 주소와 최종 커밋 해시: 메일 본문에 기재')
    if k == 8:
        body = '없음'
    extra = ''.join(img_para(*im) for im in SECTION_IMAGES.get(k, []))
    x = x[:m.start()] + convert(body) + extra + x[m.end():]

# 앱명 칸 채우기 (01 참가신청서, 02 작품 소개, 04 서명란). 개인정보가 아니라서 미리 채운다.
def fill_app_name(x):
    out, pos = [], 0
    for m in re.finditer(r'<w:p[ >].*?</w:p>', x, re.S):
        para = m.group()
        text = re.sub(r'<[^>]+>', '', para)
        if '프로젝트명' in text and '___' in text:
            done = [False]
            def rep(t):
                if not done[0]:
                    done[0] = True
                    return '<w:t xml:space="preserve"> 펫 드라이브(가칭)</w:t>'
                return '<w:t></w:t>'
            para = re.sub(r'<w:t(?: [^>]*)?>_+</w:t>', lambda mm: rep(mm), para)
            para = re.sub(r'(<w:t(?: [^>]*)?>[^<]*?프로젝트명:\s*)_+(</w:t>)', lambda mm: (done.__setitem__(0, True) or mm.group(1) + '펫 드라이브(가칭)' + mm.group(2)) if not done[0] else mm.group(1) + mm.group(2), para)
        out.append(x[pos:m.start()]); out.append(para); pos = m.end()
    out.append(x[pos:])
    return ''.join(out)

x = fill_app_name(x)
with zipfile.ZipFile(sys.argv[3], 'w', zipfile.ZIP_DEFLATED) as z:
    for info in src.infolist():
        if info.is_dir(): continue
        data = x.encode('utf-8') if info.filename == 'word/document.xml' else src.read(info.filename)
        if info.filename == 'word/_rels/document.xml.rels':
            rels = ''.join(f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/pet-{rid}.png"/>' for rid, _ in _media)
            data = data.decode('utf-8').replace('</Relationships>', rels + '</Relationships>').encode('utf-8')
        if info.filename == '[Content_Types].xml' and b'Extension="png"' not in data:
            data = data.replace(b'<Default Extension="xml"', b'<Default Extension="png" ContentType="image/png"/><Default Extension="xml"')
        z.writestr(info, data)
    for rid, fname in _media:
        z.write(os.path.join(PNG_DIR, fname), f'word/media/pet-{rid}.png')
print('ok', {k: len(v) for k, v in secs.items()})
