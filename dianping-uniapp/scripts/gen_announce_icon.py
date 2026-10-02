# -*- coding: utf-8 -*-
"""生成公告喇叭图标 announce.png（品牌红 #ff2442，透明底，96x96 @2x）。
替代 notify 页公告项的 📢 emoji（项目规范：UI 禁 emoji，图标一律 Pillow PNG）。
"""
from PIL import Image, ImageDraw

RED = (255, 36, 66, 255)
SIZE = 96

im = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(im)

# 喇叭柄（圆角矩形）
d.rounded_rectangle((14, 40, 34, 56), radius=5, fill=RED)
# 锥形喇叭口
d.polygon([(30, 36), (30, 60), (56, 74), (56, 22)], fill=RED)
# 口沿竖条
d.rectangle((54, 22, 60, 74), fill=RED)
# 两道声波弧
d.arc((60, 30, 78, 66), start=-52, end=52, fill=RED, width=6)
d.arc((68, 20, 94, 76), start=-52, end=52, fill=RED, width=6)

im.save('static/icons/announce.png')
print('saved static/icons/announce.png', im.size)
