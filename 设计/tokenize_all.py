"""全站硬编码颜色 → 设计令牌（21 个文件）

规则（保守，只动语义明确的）：
  #ff2442  渐变中 → var(--dp-brand)；其余 → var(--dp-brand-deep)
  暖色浅底   → var(--dp-accent-soft)
  冷墨 #1f2430 → var(--dp-text)
  危险/提示色 → var(--dp-danger*)/var(--dp-warn*)
其余中性色（#fff/#eee/#bbb 等）一律不动，避免误伤。
"""
import re, pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

# 简单替换映射（不含品牌红，品牌红单独处理）
SIMPLE = {
    "#ffb199": "var(--dp-orange)",
    "#ff8a3d": "var(--dp-orange)",
    "#ff7a8e": "var(--dp-brand)",
    "#4a90d9": "var(--dp-brand-deep)",
    "#1f2430": "var(--dp-text)",
    "#fcebeb": "var(--dp-danger-soft)",
    "#a32d2d": "var(--dp-danger)",
    "#fff7e8": "var(--dp-warn-soft)",
}
WASH = ["#ffe4e9", "#fff1f3", "#ffe0e4", "#fff5f6",
        "#ffeef1", "#ffd7df", "#ffe8ea", "#fff1f2"]

report = []

for f in files:
    if f.name == "App.vue":
        continue
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    st = m.group(1)
    orig = st
    n = 0

    # 品牌红：按是否在渐变里决定用原色还是加深版
    out = []
    for line in st.split("\n"):
        low = line.lower()
        if "#ff2442" in low:
            repl = "var(--dp-brand)" if "gradient" in low else "var(--dp-brand-deep)"
            c = low.count("#ff2442")
            line = re.sub("#ff2442", repl, line, flags=re.I)
            n += c
        out.append(line)
    st = "\n".join(out)

    # 暖色浅底
    for w in WASH:
        c = st.lower().count(w)
        if c:
            st = re.sub(w, "var(--dp-accent-soft)", st, flags=re.I)
            n += c

    # 其余简单映射
    for a, b in SIMPLE.items():
        c = st.lower().count(a)
        if c:
            st = re.sub(a, b, st, flags=re.I)
            n += c

    if st != orig:
        t = t[:m.start(1)] + st + t[m.end(1):]
        f.write_text(t, encoding="utf-8")
        report.append((f.relative_to(ROOT).as_posix(), n))

print(f"=== 迁移完成：{len(report)} 个文件 / {sum(n for _, n in report)} 处 ===\n")
for p, c in sorted(report, key=lambda x: -x[1]):
    print(f"  {c:>3} 处  {p}")
