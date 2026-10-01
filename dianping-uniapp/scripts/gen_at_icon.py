# -*- coding: utf-8 -*-
"""@ 提及图标：蓝色 @ 字形（系统字体），4x 超采样，线性风格与 topic 一致。"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = r"D:/桌面文件/111/dianping-uniapp/static/icons"
os.makedirs(OUT, exist_ok=True)
SS = 4
N = 48 * SS
BLUE = (74, 144, 217, 255)

# 尝试加载系统字体
font = None
cands = [
    r"C:/Windows/Fonts/msyh.ttc",
    r"C:/Windows/Fonts/arial.ttf",
    r"C:/Windows/Fonts/segoeui.ttf",
    r"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]
for c in cands:
    if os.path.exists(c):
        try:
            font = ImageFont.truetype(c, int(N * 0.82))
            break
        except Exception:
            continue
if font is None:
    font = ImageFont.load_default()

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
bbox = d.textbbox((0, 0), "@", font=font)
w = bbox[2] - bbox[0]
h = bbox[3] - bbox[1]
x = (N - w) / 2 - bbox[0]
y = (N - h) / 2 - bbox[1]
d.text((x, y), "@", font=font, fill=BLUE)

img = img.resize((48, 48), Image.LANCZOS)
p = os.path.join(OUT, "at.png")
img.save(p)
print("saved", "at.png", os.path.getsize(p), "bytes; font=", font is not None)
