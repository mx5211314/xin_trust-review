"""精确诊断：红底容器 + 其内部文字元素的实际颜色"""
import re, pathlib

root = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(root.glob("pages/**/*.vue"))
               + list(root.glob("pagesAdmin/**/*.vue"))
               + list(root.glob("components/**/*.vue")))

# 装饰性选择器：改色会破坏视觉，必须排除
DECOR = re.compile(r"(dot|badge|tab-line|tab-dot|on::after|line$|guide-logo|"
                   r"chat-badge|sum-badge|bubble|\.face|\.eye|chat-bubble)", re.I)

for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    style = m.group(1)

    # 找所有含 background:#ff2442 的规则块
    for mm in re.finditer(r"([^{}\n]+)\{([^{}]*background:\s*#ff2442[^}]*)\}", style):
        sel_full, decls = mm.group(1), mm.group(2)
        if "linear-gradient" in decls.lower():
            continue
        for sel in sel_full.split(","):
            sel = sel.strip().split("\n")[-1].strip()
            mcls = re.search(r"\.([a-zA-Z][\w-]*)", sel)
            if not mcls:
                continue
            c = mcls.group(1)
            decor = bool(DECOR.search(c))
            # 找该容器内子元素的 color（含 -text/-num 后缀或后代选择器）
            kids = re.findall(r"\.([a-zA-Z][\w-]*)[\s\S]{0,60}?\{[^}]*?color:\s*(#[0-9a-fA-F]{3,6}|white)", style)
            # 更稳：直接找形如 .xxx-text/.xxx-num 的颜色
            kid_color = None
            kc = re.search(r"\." + re.escape(c) + r"-?(?:text|num|label)?\s*\{[^}]*?color:\s*(#[0-9a-fA-F]{3,6}|white)", style, re.I)
            if kc:
                kid_color = kc.group(1)
            flag = "装饰/勿改" if decor else "★可能按钮"
            print(f"{flag:<12} .{c:<20} 子元素色={kid_color or '未在CSS定义'}")
