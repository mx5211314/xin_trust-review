"""全站硬编码颜色调查：找出所有未走令牌的颜色，按文件统计"""
import re, pathlib
from collections import Counter

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

targets = ["feed"]  # 已改过的跳过

rows = []
allcolors = Counter()

for f in files:
    rel = f.relative_to(ROOT).as_posix()
    if any(f"/{t}/" in rel for t in targets):
        continue
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    st = m.group(1)
    cols = re.findall(r"#[0-9a-fA-F]{3,8}\b", st)
    # 排除已在 var(--dp-*) 里 / transition / 透明
    brand = [c for c in cols if c.lower() in ("#ff2442", "#d81228", "#ffe8ea", "#fff1f2")]
    other = [c for c in cols if c.lower() not in ("#ff2442", "#ffffff", "#fff", "#d81228", "#ffe8ea", "#fff1f2", "#000000")]
    if cols:
        rows.append((rel, len(cols), len(brand), len(other)))
        allcolors.update(c.lower() for c in cols)

rows.sort(key=lambda r: -r[1])

print(f"{'页面':<42}{'颜色总数':>8}{'品牌红':>8}{'其他':>6}")
print("-" * 68)
for rel, tot, b, o in rows:
    flag = " ←" if b else ""
    print(f"{rel:<42}{tot:>8}{b:>8}{o:>6}{flag}")

print()
print("=" * 68)
print("全站最常出现的颜色 Top20")
print("=" * 68)
for c, n in allcolors.most_common(20):
    print(f"  {c:<10} × {n}")
print()
print(f"合计涉及 {len(rows)} 个文件")
