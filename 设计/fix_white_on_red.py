"""
红底白字按钮改色（白字 3.76:1 -> 5.20:1，达 WCAG AA）
判定规则（严格，避免误伤）：
  1. 声明必须是 background: #ff2442;（纯色底，排除 linear-gradient）
  2. 从该行往后 12 行内、且仍在同一规则块内，必须存在白色文字声明
     color: #fff / #ffffff / white
  3. 命中才替换为 var(--dp-brand-deep)
未命中的红底（图标底/装饰/渐变）一律保持原样。
"""
import re, pathlib

root = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(root.glob("pages/**/*.vue"))
               + list(root.glob("pagesAdmin/**/*.vue"))
               + list(root.glob("components/**/*.vue")))

WHITE = re.compile(r"color:\s*#(?:fff|ffffff)\b|color:\s*white\b", re.I)
BG = re.compile(r"background:\s*#ff2442\s*;", re.I)

changed_files = []
total = 0

for f in files:
    txt = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", txt, re.S)
    if not m:
        continue
    style = m.group(1)
    lines = style.split("\n")
    changed = 0

    for i, line in enumerate(lines):
        if not BG.search(line) or "linear-gradient" in line.lower():
            continue
        # 往后找白字，但不得跨越规则块结束
        has_white = False
        for j in range(i + 1, min(len(lines), i + 12)):
            s = lines[j].strip()
            if WHITE.search(s):
                has_white = True
                break
            if s in ("}", "};"):
                break
        if not has_white:
            continue
        lines[i] = BG.sub("background: var(--dp-brand-deep);", line)
        changed += 1

    if changed:
        newstyle = "\n".join(lines)
        txt = txt[:m.start(1)] + newstyle + txt[m.end(1):]
        f.write_text(txt, encoding="utf-8")
        changed_files.append((f.relative_to(root).as_posix(), changed))
        total += changed

print(f"=== 完成：{len(changed_files)} 文件 / {total} 处红底白字按钮改为 var(--dp-brand-deep) ===\n")
for p, c in changed_files:
    print(f"  {c:>2} 处  {p}")
