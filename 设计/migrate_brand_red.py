"""
品牌双色红迁移（可判定规则，避开正则陷阱）
规则：
  color: #ff2442  且同规则块 font-size < 30rpx  ->  var(--dp-brand-deep)
  其余（background / border / gradient / 大字）  ->  保留原色
仅改 <style> 段内、仅改单行 color 声明。
"""
import re, pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
targets = sorted(list(ROOT.glob("pages/**/*.vue"))
                 + list(ROOT.glob("pagesAdmin/**/*.vue"))
                 + list(ROOT.glob("components/**/*.vue")))

SMALL = 30
report = []

for f in targets:
    txt = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", txt, re.S)
    if not m:
        continue
    style = m.group(1)
    lines = style.split("\n")
    changed = 0

    for i, line in enumerate(lines):
        low = line.lower()
        if "ff2442" not in low or "color" not in low:
            continue
        # 必须是纯 color 声明（不是 background-color / border-color）
        decl = line.split(":", 1)
        if len(decl) < 2:
            continue
        prop = decl[0].strip().lower()
        if prop != "color":
            continue

        # 向上找本规则块的 font-size
        fs = None
        for j in range(i, -1, -1):
            s = lines[j].strip()
            fm = re.search(r"font-size:\s*(\d+)\s*rpx", s)
            if fm:
                fs = int(fm.group(1))
                break
            if s == "}" or s == "};":
                break

        if fs is not None and fs < SMALL:
            lines[i] = line.replace("#ff2442", "var(--dp-brand-deep)").replace("#FF2442", "var(--dp-brand-deep)")
            changed += 1

    if changed:
        newstyle = "\n".join(lines)
        txt = txt[:m.start(1)] + newstyle + txt[m.end(1):]
        f.write_text(txt, encoding="utf-8")
        report.append((f.relative_to(ROOT).as_posix(), changed))

total = sum(c for _, c in report)
print(f"=== 完成：{len(report)} 文件 / {total} 处小字 -> var(--dp-brand-deep) ===\n")
for p, c in sorted(report, key=lambda x: -x[1]):
    print(f"  {c:>3}  {p}")
