# -*- coding: utf-8 -*-
"""发布页功能行图标：话题#/地点pin/店铺/可见眼睛（4x 超采样，线性简洁风）"""
from PIL import Image, ImageDraw
import os

OUT = r"D:/桌面文件/111/dianping-uniapp/static/icons"
os.makedirs(OUT, exist_ok=True)
SS = 4
SIZE = 48
N = SIZE * SS
BLUE = (74, 144, 217, 255)
RED = (255, 36, 66, 255)
ORANGE = (255, 138, 61, 255)
GRAY = (138, 144, 153, 255)


def canvas():
    return Image.new("RGBA", (N, N), (0, 0, 0, 0))


def save(img, name):
    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    p = os.path.join(OUT, name)
    img.save(p)
    print("saved", name, os.path.getsize(p), "bytes")


def hashtag(color):
    """# 号：两竖两横，圆头笔画"""
    img = canvas()
    d = ImageDraw.Draw(img)
    w = N * 0.10
    x1, x2 = N * 0.34, N * 0.66
    y1, y2 = N * 0.28, N * 0.72
    # 两竖（微斜）
    d.rounded_rectangle([N * 0.30, y1 - w / 2, N * 0.30 + w, y2 + N * 0.10], radius=w / 2, fill=color)
    d.rounded_rectangle([N * 0.62, y1 - w / 2, N * 0.62 + w, y2 + N * 0.10], radius=w / 2, fill=color)
    # 两横
    d.rounded_rectangle([N * 0.16, N * 0.38 - w / 2, N * 0.84, N * 0.38 + w / 2], radius=w / 2, fill=color)
    d.rounded_rectangle([N * 0.16, N * 0.62 - w / 2, N * 0.84, N * 0.62 + w / 2], radius=w / 2, fill=color)
    return img


def pin(color):
    """定位 pin：水滴形 + 白色圆孔"""
    img = canvas()
    d = ImageDraw.Draw(img)
    # 上圆
    d.ellipse([N * 0.24, N * 0.10, N * 0.76, N * 0.62], fill=color)
    # 下尖
    d.polygon([(N * 0.30, N * 0.50), (N * 0.70, N * 0.50), (N * 0.50, N * 0.90)], fill=color)
    # 白孔
    d.ellipse([N * 0.38, N * 0.22, N * 0.62, N * 0.46], fill=(255, 255, 255, 255))
    return img


def shop(color):
    """店铺：屋顶 + 门面 + 门"""
    img = canvas()
    d = ImageDraw.Draw(img)
    # 屋顶（梯形遮阳棚）
    d.polygon([(N * 0.12, N * 0.34), (N * 0.88, N * 0.34), (N * 0.80, N * 0.16), (N * 0.20, N * 0.16)], fill=color)
    # 条纹棚
    for i in range(3):
        x = N * (0.24 + i * 0.20)
        d.polygon([(x, N * 0.16), (x + N * 0.08, N * 0.16), (x + N * 0.06, N * 0.34), (x - N * 0.02, N * 0.34)],
                  fill=(255, 255, 255, 140))
    # 门面
    d.rounded_rectangle([N * 0.20, N * 0.34, N * 0.80, N * 0.84], radius=N * 0.04, fill=color)
    # 门（白）
    d.rounded_rectangle([N * 0.42, N * 0.48, N * 0.58, N * 0.84], radius=N * 0.04, fill=(255, 255, 255, 255))
    # 窗（白）
    d.rounded_rectangle([N * 0.26, N * 0.44, N * 0.36, N * 0.54], radius=2, fill=(255, 255, 255, 255))
    d.rounded_rectangle([N * 0.64, N * 0.44, N * 0.74, N * 0.54], radius=2, fill=(255, 255, 255, 255))
    return img


def eye(color):
    """眼睛：外轮廓 + 瞳孔"""
    img = canvas()
    d = ImageDraw.Draw(img)
    # 外眼形（两段圆弧近似：大椭圆 + 上下切割）
    d.ellipse([N * 0.08, N * 0.26, N * 0.92, N * 0.74], outline=color, width=int(N * 0.075))
    # 瞳孔
    d.ellipse([N * 0.38, N * 0.36, N * 0.62, N * 0.64], fill=color)
    # 高光
    d.ellipse([N * 0.44, N * 0.40, N * 0.52, N * 0.48], fill=(255, 255, 255, 255))
    return img


save(hashtag(BLUE), "topic.png")
save(pin(RED), "location.png")
save(shop(ORANGE), "shop.png")
save(eye(GRAY), "eye.png")
