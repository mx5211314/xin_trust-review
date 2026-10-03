"""逐页统计：字号种类数、间距离格数（作为前后对比依据）"""
import re, pathlib
from collections import Counter

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

SPACING_PROP = re.compile(
    r"(?:^|[\s;{])(margin|margin-top|margin-bottom|margin-left|margin-right|"
    r"padding|padding-top|padding-bottom|padding-left|padding-right|gap)\s*:\s*([^;}]+)", re.I)

rows = []
for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    style = m.group(1)
    sizes = [int(v) for v in re.findall(r"font-size\s*:\s*(\d+)rpx", style)]
    sp = []
    for prop, val in SPACING_PROP.findall(style):
        if "var(" in val:
            continue
        sp += [int(n) for n in re.findall(r"(-?\d+)rpx", val)]
    bad = [v for v in sp if v % 8 != 0]
    uniq = sorted(set(sizes))
    rows.append((
        f.relative_to(ROOT).as_posix(),
        len(uniq), len(sizes),
        len(sp), len(bad),
        (len(bad) * 100 // len(sp)) if sp else 0,
        uniq
    ))

rows.sort(key=lambda r: (-r[1], -r[3]))

print(f"{'页面':<40}{'字号种':>6}{'字号处':>7}{'间距处':>7}{'离格':>6}{'离格%':>7}")
print("-" * 76)
for p, us, ts, tsp, bad, pct, uniq in rows:
    print(f"{p:<40}{us:>6}{ts:>7}{tsp:>7}{bad:>6}{pct:>6}%")

print()
print("=" * 76)
print("字号种类最多的 8 个页面（整改收益最大）")
print("=" * 76)
for p, us, ts, tsp, bad, pct, uniq in rows[:8]:
    print(f"\n{p}  → {us} 种字号")
    print(f"   取值: {' '.join(str(u) for u in uniq)}")

tot_u = sum(r[1] for r in rows)
tot_sp = sum(r[3] for r in rows)
tot_bad = sum(r[4] for r in rows)
print()
print("=" * 76)
print(f"合计：{len(rows)} 个文件 / 字号种类累计 {tot_u} / 间距 {tot_sp} 处（离格 {tot_bad} = {tot_bad*100//max(tot_sp,1)}%）")
