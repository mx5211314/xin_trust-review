# -*- coding: utf-8 -*-
"""生成 tabBar 图标 6 张：81x81 透明底 PNG"""
from PIL import Image, ImageDraw
import os

SIZE = 81
GRAY = (153, 153, 153, 255)      # 未选中
DARK = (31, 36, 48, 255)         # 选中（近黑）
RED = (255, 36, 66, 255)         # 品牌红
WHITE = (255, 255, 255, 255)
OUT = r"D:\桌面文件\111\dianping-uniapp\static\tabbar"
os.makedirs(OUT, exist_ok=True)


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def home(color):
    """房子剪影：三角屋顶 + 方身"""
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(40, 10), (8, 40), (73, 40)], fill=color)
    d.rectangle([16, 38, 65, 71], fill=color)
    return img


def plus(color, bg=None):
    """发布：圆底 + 白加号；未选中为纯灰加号"""
    img = canvas()
    d = ImageDraw.Draw(img)
    if bg:
        d.ellipse([6, 6, 75, 75], fill=bg)
        d.rectangle([37, 24, 44, 56], fill=WHITE)
        d.rectangle([24, 37, 57, 44], fill=WHITE)
    else:
        d.rectangle([36, 22, 45, 58], fill=color)
        d.rectangle([22, 37, 59, 44], fill=color)
    return img


def user(color):
    """人形剪影：圆头 + 半圆身"""
    img = canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([27, 10, 54, 37], fill=color)
    d.pieslice([16, 42, 65, 91], start=180, end=360, fill=color)
    return img


def msg(color):
    """消息气泡：圆角矩形 + 左下小尾巴"""
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([8, 12, 73, 60], radius=14, fill=color)
    d.polygon([(20, 56), (20, 72), (38, 58)], fill=color)
    return img


specs = {
    "home.png": home(GRAY),
    "home-on.png": home(DARK),
    "plus.png": plus(GRAY),
    "plus-on.png": plus(WHITE, RED),
    "msg.png": msg(GRAY),
    "msg-on.png": msg(DARK),
    "my.png": user(GRAY),
    "my-on.png": user(DARK),
}
for name, img in specs.items():
    path = os.path.join(OUT, name)
    img.save(path)
    print("saved", path, os.path.getsize(path), "bytes")
