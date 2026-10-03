"""全站 emoji 审计：项目约定 UI 禁止 emoji（应用 Pillow 生成的 PNG 图标）"""
import re, pathlib, unicodedata

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

# 常见 emoji 区段
EMOJI = re.compile(
    "[\U0001F300-\U0001F9FF"      # 符号与象形
    "\U0001FA00-\U0001FAFF"
    "\U00002600-\U000027BF"       # 杂项符号与装饰
    "\U0001F1E6-\U0001F1FF]"      # 区域指示符
)


def is_emoji(ch):
    try:
        return unicodedata.category(ch) == "So" or EMOJI.match(ch)
    except Exception:
        return False


hits = []
for f in files:
    t = f.read_text(encoding="utf-8")
    # 只看模板区（样式里不会有 emoji；注释里的说明文字不算）
    m = re.search(r"<template>(.*?)</template>", t, re.S)
    if not m:
        continue
    tpl = m.group(1)
    tpl = re.sub(r"<!--.*?-->", "", tpl, flags=re.S)   # 排除注释
    found = {}
    for ch in tpl:
        if EMOJI.match(ch):
            found[ch] = found.get(ch, 0) + 1
    if found:
        hits.append((f.relative_to(ROOT).as_posix(), found))

print(f"扫描 {len(files)} 个文件\n")
if hits:
    print("❌ 模板中出现 emoji：")
    for rel, found in sorted(hits, key=lambda x: -sum(x[1].values())):
        s = " ".join(f"{c}×{n}" for c, n in sorted(found.items(), key=lambda x: -x[1]))
        print(f"  {rel:<44} {s}")
    print(f"\n共 {len(hits)} 个文件、{sum(sum(v.values()) for _, v in hits)} 处")
else:
    print("✅ 模板中无 emoji")
