# -*- coding: utf-8 -*-
"""生成「收藏夹」文件夹图标（替代 detail.vue 里的 📁 emoji）
   风格对齐 gen_action_icons.py：96px / 4x 超采样 / 灰 (170,175,185)"""
from PIL import Image, ImageDraw
import os

OUT = r"D:\桌面文件\111\dianping-uniapp\static\icons"
os.makedirs(OUT, exist_ok=True)

SS, SIZE = 4, 96
N = SIZE * SS
GRAY = (170, 175, 185, 255)
BRAND = (216, 18, 40, 255)


def folder(color):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    L, R = N * 0.12, N * 0.88
    T, B = N * 0.26, N * 0.80
    r = N * 0.07

    # 标签（左上凸起）
    d.rounded_rectangle([L, T - N * 0.10, L + N * 0.34, T + N * 0.06],
                        radius=r, fill=color)
    # 主体
    d.rounded_rectangle([L, T, R, B], radius=r, fill=color)
    return img


def save(img, name):
    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    p = os.path.join(OUT, name)
    img.save(p)
    print("saved", name, os.path.getsize(p), "bytes")


save(folder(GRAY), "folder.png")
save(folder(BRAND), "folder-on.png")
