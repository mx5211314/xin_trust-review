# -*- coding: utf-8 -*-
"""生成操作栏图标：心/星/评论气泡（灰/彩色两态），4x 超采样抗锯齿"""
from PIL import Image, ImageDraw
import os, math

OUT = r"D:\桌面文件\111\dianping-uniapp\static\icons"
os.makedirs(OUT, exist_ok=True)

SS = 4          # 超采样倍数
SIZE = 96       # 输出尺寸
N = SIZE * SS   # 绘制尺寸

GRAY = (170, 175, 185, 255)
RED = (255, 36, 66, 255)
GOLD = (255, 184, 0, 255)
WHITE = (255, 255, 255, 255)


def canvas():
    return Image.new("RGBA", (N, N), (0, 0, 0, 0))


def save(img, name):
    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    p = os.path.join(OUT, name)
    img.save(p)
    print("saved", name, os.path.getsize(p), "bytes")


def heart(color):
    """心形：两圆 + 下三角，比例贴近常见图标"""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = N / 2, N / 2 - N * 0.06
    r = N * 0.235
    d.ellipse([cx - 2 * r * 0.92, cy - r * 1.05, cx - 2 * r * 0.92 + 2 * r, cy - r * 1.05 + 2 * r], fill=color)
    d.ellipse([cx + 2 * r * 0.92 - 2 * r, cy - r * 1.05, cx + 2 * r * 0.92, cy - r * 1.05 + 2 * r], fill=color)
    d.polygon([
        (cx - 2 * r * 0.96, cy + r * 0.35),
        (cx + 2 * r * 0.96, cy + r * 0.35),
        (cx, cy + r * 2.15),
    ], fill=color)
    return img


def star(color):
    """五角星"""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = N / 2, N / 2 + N * 0.03
    R, r = N * 0.44, N * 0.185
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = R if i % 2 == 0 else r
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=color)
    return img


def bubble(color, dot_color=WHITE):
    """评论气泡：圆角矩形 + 左下小尾巴 + 三个点"""
    img = canvas()
    d = ImageDraw.Draw(img)
    pad = N * 0.10
    box = [pad, pad * 1.15, N - pad, N - pad * 1.9]
    d.rounded_rectangle(box, radius=N * 0.20, fill=color)
    # 左下小尾巴
    x0 = pad + N * 0.06
    d.polygon([
        (x0, box[3] - N * 0.08),
        (x0, N - pad * 0.35),
        (x0 + N * 0.16, box[3] - N * 0.02),
    ], fill=color)
    # 三个点
    cy = (box[1] + box[3]) / 2
    r = N * 0.045
    for dx in (-N * 0.155, 0, N * 0.155):
        d.ellipse([N / 2 + dx - r, cy - r, N / 2 + dx + r, cy + r], fill=dot_color)
    return img


save(heart(GRAY), "heart.png")
save(heart(RED), "heart-on.png")
save(star(GRAY), "star.png")
save(star(GOLD), "star-on.png")
save(bubble(GRAY), "bubble.png")
