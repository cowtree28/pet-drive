# D-ring: 진행률 링 + 가운데 내용
import math


def ring(size, stroke, frac, color, center, track="#EDEEF2", cls="ring"):
    r = (size - stroke) / 2
    c = 2 * math.pi * r
    off = c * (1 - max(0.0, min(1.0, frac)))
    return f'''<div class="{cls}" style="width:{size}px;height:{size}px">
  <svg width="{size}" height="{size}" style="transform:rotate(-90deg)">
    <circle cx="{size/2}" cy="{size/2}" r="{r}" stroke="{track}" stroke-width="{stroke}" fill="none"/>
    <circle cx="{size/2}" cy="{size/2}" r="{r}" stroke="{color}" stroke-width="{stroke}" fill="none" stroke-linecap="round" stroke-dasharray="{c:.1f}" stroke-dashoffset="{off:.1f}"/>
  </svg>
  <div class="rc">{center}</div>
</div>'''
