"""空状态审计：找出仍是「一行灰字」的空状态，以及缺失的引导按钮

项目约定（原则 09）：空状态要「说清楚 + 给下一步」——
  圆底图标 .empty-ic + 标题 .empty-t + 说明 .empty-d + 主按钮 .empty-btn
"""
import re, pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

guided, plain, none = [], [], []

for f in files:
    rel = f.relative_to(ROOT).as_posix()
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<template>(.*?)</template>", t, re.S)
    if not m:
        continue
    tpl = m.group(1)
    # 找出所有空的空状态块
    blocks = re.findall(r'<view[^>]*class="[^"]*\bempty\b[^"]*"[^>]*>(.*?)</view>\s*(?=<|$)',
                        tpl, re.S)
    if not blocks:
        continue
    has_guided = "empty-t" in tpl
    has_btn = "empty-btn" in tpl
    n_blocks = len(blocks)
    if has_guided and has_btn:
        guided.append((rel, n_blocks))
    elif has_guided:
        plain.append((rel, n_blocks, "缺引导按钮"))
    else:
        plain.append((rel, n_blocks, "仍是灰字"))

print(f"扫描 {len(files)} 个文件\n")
print(f"✅ 已引导式（{len(guided)}）")
for rel, n in guided:
    print(f"     {rel}")

print(f"\n❌ 待改（{len(plain)}）")
for rel, n, why in plain:
    print(f"     {rel:<40} {n} 处空状态 · {why}")
