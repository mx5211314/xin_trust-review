import re, pathlib

root = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(root.glob("pages/**/*.vue"))
               + list(root.glob("pagesAdmin/**/*.vue"))
               + list(root.glob("components/**/*.vue")))

WHITE = re.compile(r"color:\s*#(?:fff|ffffff)\b", re.I)
hits = []

for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    lines = m.group(1).split("\n")
    for i, l in enumerate(lines):
        low = l.lower()
        if "ff2442" not in low:
            continue
        if "background" not in low or "linear-gradient" in low:
            continue
        # 从本行往后 12 行找白色文字声明
        for j in range(i + 1, min(len(lines), i + 12)):
            if WHITE.search(lines[j]):
                hits.append((f.relative_to(root).as_posix(), i + 1, l.strip()[:42]))
                break

print("=== 红底白字（对比度 3.76:1，均不达 WCAG AA 4.5:1）===")
for p, n, s in hits:
    print(f"  {p}:{n}  {s}")
print(f"\n总计 {len(hits)} 处")
