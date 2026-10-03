# -*- coding: utf-8 -*-
"""生成「分享」图标（箭头出框），与 gen_action_icons.py 保持同风格：
   96px 输出 / 4x 超采样 / 默认灰 (170,175,185)"""
from PIL import Image, ImageDraw
import os

OUT = r"D:\桌面文件\111\dianping-uniapp\static\icons"
os.makedirs(OUT, exist_ok=True)

SS = 4
SIZE = 96
N = SIZE * SS

GRAY = (170, 175, 185, 255)
RED = (255, 36, 66, 255)


def share(color):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(N * 0.075)          # 线宽
    cx = N * 0.5

    # 箭头竖线
    d.line([(cx, N * 0.22), (cx, N * 0.60)], fill=color, width=w)
    # 箭头两撇
    d.line([(cx - N * 0.17, N * 0.38), (cx, N * 0.21)], fill=color, width=w)
    d.line([(cx + N * 0.17, N * 0.38), (cx, N * 0.21)], fill=color, width=w)

    # 收纳框（顶部留口给箭头穿过）
    L, R = N * 0.18, N * 0.82
    T, B = N * 0.47, N * 0.86
    d.line([(L, T), (L, B)], fill=color, width=w)   # 左竖
    d.line([(R, T), (R, B)], fill=color, width=w)   # 右竖
    d.line([(L, B), (R, B)], fill=color, width=w)   # 底横
    d.line([(L, T), (L + N * 0.16, T)], fill=color, width=w)  # 左上短横
    d.line([(R - N * 0.16, T), (R, T)], fill=color, width=w)  # 右上短横
    return img


def save(img, name):
    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    p = os.path.join(OUT, name)
    img.save(p)
    print("saved", name, os.path.getsize(p), "bytes")


save(share(GRAY), "share.png")
save(share(RED), "share-on.png")
