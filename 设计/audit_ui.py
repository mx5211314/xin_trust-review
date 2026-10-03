"""UI 量化审查：对照 9 条设计原则扫描 dianping-uniapp 全量样式"""
import re, pathlib
from collections import Counter

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

font_sizes = Counter()
spacing_vals = Counter()   # margin/padding/gap
skeleton_files, empty_files, backdrop_files = [], [], []
fixed_files = []
position_vals = Counter()

SPACING_PROP = re.compile(
    r"(?:^|[\s;{])(margin|margin-top|margin-bottom|margin-left|margin-right|"
    r"padding|padding-top|padding-bottom|padding-left|padding-right|"
    r"margin-inline|padding-inline|gap)\s*:\s*([^;}]+)", re.I)

for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    style = m.group(1)
    rel = f.relative_to(ROOT).as_posix()

    for v in re.findall(r"font-size\s*:\s*(\d+)rpx", style):
        font_sizes[int(v)] += 1

    for prop, val in SPACING_PROP.findall(style):
        if "var(" in val:
            continue
        for n in re.findall(r"(-?\d+)rpx", val):
            spacing_vals[int(n)] += 1

    for v in re.findall(r"position\s*:\s*(fixed|sticky|absolute)", style, re.I):
        position_vals[v.lower()] += 1

    if "skeleton" in t or "骨架" in t:
        skeleton_files.append(rel)
    if "empty" in t.lower() or "暂无" in t or "还没有" in t:
        empty_files.append(rel)
    if "backdrop-filter" in t:
        backdrop_files.append(rel)
    if re.search(r"position\s*:\s*fixed", style, re.I) or "safe-area" in style:
        fixed_files.append(rel)

print("=" * 62)
print("原则07 字阶层级：全站 font-size 取值分布")
print("=" * 62)
tot = sum(font_sizes.values())
for size in sorted(font_sizes):
    bar = "#" * font_sizes[size]
    print(f"  {size:>3}rpx  ×{font_sizes[size]:>3}  {bar}")
print(f"  → 共 {len(font_sizes)} 种字号 / {tot} 处使用")

print()
print("=" * 62)
print("原则08 间距系统：8 的倍数检查")
print("=" * 62)
on8 = {v: c for v, c in spacing_vals.items() if v % 8 == 0 and v >= 0}
off8 = {v: c for v, c in spacing_vals.items() if v % 8 != 0}
tot_sp = sum(spacing_vals.values())
on_cnt = sum(on8.values())
off_cnt = sum(off8.values())
print(f"  落在 8 倍数上 : {on_cnt} 处 ({on_cnt*100//max(tot_sp,1)}%)")
print(f"  不在 8 倍数上 : {off_cnt} 处 ({off_cnt*100//max(tot_sp,1)}%)")
print(f"  → 离格取值 Top15：", end="")
for v, c in sorted(off8.items(), key=lambda x: -x[1])[:15]:
    print(f"{v}rpx×{c}", end="  ")
print()

print()
print("=" * 62)
print("原则03/09 骨架屏 & 空状态覆盖")
print("=" * 62)
print(f"  含骨架屏 skeleton 的文件: {len(skeleton_files)} / {len(files)}")
for s in skeleton_files:
    print(f"      {s}")
print(f"  含空状态 empty/暂无 的文件: {len(empty_files)} / {len(files)}")
for s in empty_files:
    print(f"      {s}")

print()
print("=" * 62)
print("原则05/06 固定底栏 / 毛玻璃 / 安全区")
print("=" * 62)
print(f"  position 用法统计: {dict(position_vals)}")
print(f"  含 fixed 或 safe-area 的文件: {len(fixed_files)}")
for s in fixed_files:
    print(f"      {s}")
print(f"  含 backdrop-filter(毛玻璃) 的文件: {len(backdrop_files)}")
for s in backdrop_files:
    print(f"      {s}")
