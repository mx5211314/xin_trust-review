"""
红底白字「真按钮」改色（白字 3.76:1 -> 5.20:1，达 WCAG AA）
白名单：仅改这 6 个确认为按钮容器的选择器。
角标/红点/下划线/数字胶囊等装饰件一律不动。
"""
import re, pathlib

root = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")

# 真按钮白名单：选择器名 -> 说明
BUTTONS = {
    "op-pass":    "审核台-通过",
    "done-btn":   "审核台-刷新",
    "nav-publish":"导航-发布",
    "follow-btn": "关注/取关",
    "step-num":   "步骤序号",
    "edit-btn":   "编辑资料",
}

files = sorted(list(root.glob("pages/**/*.vue"))
               + list(root.glob("pagesAdmin/**/*.vue"))
               + list(root.glob("components/**/*.vue")))

done = []

for f in files:
    txt = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", txt, re.S)
    if not m:
        continue
    style = m.group(1)
    lines = style.split("\n")
    changed = 0

    for i, line in enumerate(lines):
        if not re.search(r"background:\s*#ff2442", line, re.I):
            continue
        if "linear-gradient" in line.lower():
            continue
        # 找本行所属选择器（向上找最近的以 { 结尾的行）
        sel = ""
        for j in range(i, -1, -1):
            s = lines[j].strip()
            if s.endswith("{"):
                sel = s[:-1].strip()
                break
            if s == "}" or s == "};":
                break
        mcls = re.findall(r"\.([a-zA-Z][\w-]*)", sel)
        if not mcls:
            continue
        c = mcls[-1]
        if c in BUTTONS:
            lines[i] = re.sub(r"#ff2442", "var(--dp-brand-deep)", line, flags=re.I)
            changed += 1

    if changed:
        newstyle = "\n".join(lines)
        txt = txt[:m.start(1)] + newstyle + txt[m.end(1):]
        f.write_text(txt, encoding="utf-8")
        done.append((f.relative_to(root).as_posix(), changed))

print(f"=== 完成：{len(done)} 文件 / {sum(c for _, c in done)} 处真按钮改色 ===\n")
for p, c in done:
    print(f"  {c} 处  {p}")
print("\n处理的按钮：")
for k, v in BUTTONS.items():
    print(f"  .{k:<14} {v}")
