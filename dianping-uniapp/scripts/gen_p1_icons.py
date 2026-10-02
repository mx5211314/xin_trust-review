# -*- coding: utf-8 -*-
"""P1 图标：gear.png（设置，灰）、star-red.png（可信点评人盾徽星，品牌红）。
项目规范：UI 图标一律 Pillow PNG，禁 emoji/Unicode 符号。
"""
import math
from PIL import Image, ImageDraw

RED = (255, 36, 66, 255)
GRAY = (154, 160, 172, 255)  # --dp-text4 同系

# ---- gear.png：外环 + 8 根放射齿（中孔天然透明） ----
im = Image.new('RGBA', (96, 96), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.ellipse((20, 20, 76, 76), outline=GRAY, width=11)
for i in range(8):
    a = math.radians(i * 45)
    x1, y1 = 48 + 26 * math.cos(a), 48 + 26 * math.sin(a)
    x2, y2 = 48 + 42 * math.cos(a), 48 + 42 * math.sin(a)
    d.line((x1, y1, x2, y2), fill=GRAY, width=11)
im.save('static/icons/gear.png')

# ---- star-red.png：五角星（盾徽用星） ----
im2 = Image.new('RGBA', (96, 96), (0, 0, 0, 0))
d2 = ImageDraw.Draw(im2)
pts = []
for i in range(10):
    r = 42 if i % 2 == 0 else 18
    a = math.radians(-90 + i * 36)
    pts.append((48 + r * math.cos(a), 48 + r * math.sin(a)))
d2.polygon(pts, fill=RED)
im2.save('static/icons/star-red.png')

print('saved gear.png / star-red.png')
