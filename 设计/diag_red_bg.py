"""
诊断：红底规则的选择器 -> template 里是否配白字
用途：区分「真按钮(需改色)」与「图标底/装饰(保持原色)」
"""
import re, pathlib

root = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(root.glob("pages/**/*.vue"))
               + list(root.glob("pagesAdmin/**/*.vue"))
               + list(root.glob("components/**/*.vue")))

WHITE_INLINE = re.compile(r"color:\s*(#fff\b|#ffffff\b|white\b)", re.I)
rows = []

for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    style = m.group(1)
    tpl = t[:m.start(1)]          # template + script
    template = re.search(r"<template>(.*?)</template>", tpl, re.S)
    tpl = template.group(1) if template else tpl

    for mm in re.finditer(r"([^{}\n]+)\{([^{}]*)\}", style):
        sel_full, decls = mm.group(1), mm.group(2)
        if not re.search(r"background:\s*#ff2442", decls, re.I):
            continue
        if "linear-gradient" in decls.lower():
            kind = "渐变"
        else:
            kind = "纯红底"
        for sel in sel_full.split(","):
            sel = sel.strip().split("\n")[-1].strip()
            if not sel:
                continue
            classes = re.findall(r"\.([a-zA-Z][\w-]*)", sel)
            if not classes:
                continue
            c = classes[-1]
            # template 中该 class 的元素上是否出现白色文字
            uses = re.findall(r'<[^>]*class="[^"]*\b' + re.escape(c) + r'\b[^"]*"[^>]*>', tpl)
            white = any(WHITE_INLINE.search(u) for u in uses)
            rows.append((f.relative_to(root).as_posix(), kind, sel, len(uses), white))

print("=== 红底规则清单 ===")
print(f"{'文件':<38}{'类型':<8}{'选择器':<26}{'用量':<6}白字")
for p, kind, sel, n, white in rows:
    print(f"{p:<38}{kind:<8}{sel:<26}{n:<6}{'是' if white else '否'}")

need = [r for r in rows if r[4] and r[1] == "纯红底"]
print(f"\n需改色（纯红底+白字）: {len(need)}")
print(f"应保留（渐变/图标底/无白字）: {len(rows) - len(need)}")
