"""접수양식 02 부분만 뽑아 읽기용 PDF를 만든다.

사용: python3 tools/make_pdf.py   (mammoth 필요: pip install mammoth)
결과: docs/기획서-읽기용.pdf
"""
import glob, os, re, subprocess, tempfile
import mammoth

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
CH = sorted(glob.glob(os.path.expanduser('~/.cache/ms-playwright/chromium_headless_shell-*/*/chrome-headless-shell')))[-1]

h = mammoth.convert_to_html(open(os.path.join(ROOT, 'docs', '접수양식-작성본.docx'), 'rb')).value
i = h.rfind('<p', 0, h.find('1. 앱 한 줄 소개'))
j = h.find('03  개인정보'); j = h.rfind('<p', 0, j) if j > 0 else len(h)
body = h[i:j]
body = re.sub(r'<table[^>]*>\s*(<tbody>)?\s*<tr[^>]*>\s*<td[^>]*>', '<div class="box">', body)   # 쪽 나눔이 되도록 표 대신 상자
body = re.sub(r'</td>\s*</tr>\s*(</tbody>)?\s*</table>', '</div>', body)
font = 'file://' + os.path.join(ROOT, 'design', 'screens', 'fonts', 'AstaSans.ttf')
css = ('@font-face{font-family:"Asta Sans";src:url("%s")}@page{size:A4;margin:18mm 16mm}'
       'body{font-family:"Asta Sans";font-size:10.5pt;line-height:1.6;color:#131417;word-break:keep-all}'
       'h1{font-size:22pt;margin:0 0 4pt}.sub{color:#686A72;font-size:10pt;margin-bottom:14pt}p{margin:3pt 0}'
       '.box{border:1px solid #C7C9CE;border-radius:6pt;padding:8pt 10pt;margin:4pt 0 12pt}img{width:100%%;margin-top:6pt;break-inside:avoid}') % font
html = (f'<!doctype html><html lang="ko"><head><meta charset="utf-8"><style>{css}</style></head><body>'
        '<h1>펫 드라이브 (가칭)</h1><div class="sub">2026 현대자동차그룹 차량용 앱 개발 공모전 기획서 (접수양식 02 읽기용)</div>'
        f'{body}</body></html>')
with tempfile.NamedTemporaryFile('w', suffix='.html', delete=False, encoding='utf-8') as f:
    f.write(html); tmp = f.name
out = os.path.join(ROOT, 'docs', '기획서-읽기용.pdf')
subprocess.run([CH, '--headless', '--no-sandbox', '--allow-file-access-from-files', '--virtual-time-budget=4000',
                '--no-pdf-header-footer', f'--print-to-pdf={out}', 'file://' + tmp],
               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
os.remove(tmp)
print('ok', out)
