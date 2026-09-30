# -*- coding: utf-8 -*-
"""生成 10 张演示配图（存放在本地上传目录，由后端托管，手机可访问）"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = r"D:\dianping-upload\demo"
os.makedirs(OUT, exist_ok=True)
W, H = 600, 450

# 微软雅黑（加粗）
FONT_BIG = ImageFont.truetype(r"C:\Windows\Fonts\msyhbd.ttc", 56)
FONT_SM = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 26)
FONT_TAG = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 22)

# (标题, 标签, 渐变色1, 渐变色2, 装饰圆色)
specs = [
    ("老味烧烤", "唐山 · 探店", (255, 138, 90), (214, 60, 40), (255, 200, 160)),
    ("炭火自助", "人均30吃到撑", (255, 170, 110), (190, 80, 40), (255, 220, 180)),
    ("棋盘山", "周末遛娃", (110, 190, 150), (30, 110, 90), (180, 230, 200)),
    ("山有咖啡", "社区小店", (200, 160, 120), (110, 80, 55), (235, 210, 180)),
    ("唐山宴", "一次吃遍", (250, 190, 90), (200, 120, 30), (255, 230, 170)),
    ("郝家羊汤", "三十年老店", (240, 200, 150), (170, 110, 60), (255, 235, 200)),
    ("秦皇小巷", "夜市", (150, 120, 220), (70, 50, 140), (210, 195, 245)),
    ("南湖灯光秀", "免费出片", (120, 160, 230), (40, 70, 150), (190, 215, 250)),
    ("探店日记", "本地点评", (255, 120, 150), (170, 50, 90), (255, 190, 210)),
    ("周末好去处", "本地精选", (130, 200, 210), (30, 110, 130), (190, 235, 240)),
]

for idx, (title, tag, c1, c2, deco) in enumerate(specs, start=1):
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    # 对角线渐变
    for y in range(H):
        t = y / H
        r = int(c1[0] + (c2[0] - c1[0]) * t)
        g = int(c1[1] + (c2[1] - c1[1]) * t)
        b = int(c1[2] + (c2[2] - c1[2]) * t)
        d.line([(0, y), (W, y)], fill=(r, g, b))
    # 装饰圆
    d.ellipse([W - 230, -110, W + 90, 210], fill=deco)
    d.ellipse([-70, H - 120, 90, H + 40], fill=deco)
    # 文案
    bbox = d.textbbox((0, 0), title, font=FONT_BIG)
    tw = bbox[2] - bbox[0]
    d.text(((W - tw) / 2, H / 2 - 80), title, font=FONT_BIG, fill=(255, 255, 255))
    bb2 = d.textbbox((0, 0), tag, font=FONT_SM)
    tw2 = bb2[2] - bb2[0]
    d.text(((W - tw2) / 2, H / 2 + 10), tag, font=FONT_SM, fill=(255, 244, 240))
    # 品牌角标
    d.rounded_rectangle([22, 22, 152, 62], radius=20, fill=(255, 255, 255))
    d.text((42, 30), "本地点评", font=FONT_TAG, fill=c2)
    path = os.path.join(OUT, f"dp{idx}.jpg")
    img.save(path, quality=88)
    print("saved", path, os.path.getsize(path), "bytes")
