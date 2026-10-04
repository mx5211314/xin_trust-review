"""扫描 v-if / v-else-if / v-else 链被「兄弟 v-if」截断的坑

v-else-if / v-else 只认**紧邻的上一个兄弟元素**。
如果在一条链中间插进一个带 v-if 的兄弟，后面的 else-if/else 就会
挂到那个兄弟身上，而不是原本的链头。

真实案例（detail.vue，已修）：
  <swiper v-if="有图"> … </swiper>
  <view v-if="图>1" class="page-badge">…</view>   ← 截断点
  <video v-else-if="videoUrl" />                  ← 实际挂在 page-badge 上
  <view v-else>视频加载失败</view>                 ← 所以单图笔记会误显示

判定：在同一段「连续的兄弟条件元素」里，若出现 ≥2 个 v-if
      且其后还有 v-else-if / v-else，即视为可疑。
"""
import re, pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

# 注意：属性值里可能有 '>'（如 v-if="list.length > 1"），
# 用 (?:  "[^"]*" | '[^']*' | [^>"'] )* 跳过带引号的部分，
# 否则会在引号内的 '>' 处提前截断 → v-if 识别不到（本脚本曾因此漏报）
TAG = re.compile(r"""^\s*<([a-zA-Z][\w-]*)\b((?:"[^"]*"|'[^']*'|[^>"'])*)/?>""")
VIF = re.compile(r'\bv-if\s*=\s*"([^"]*)"')
VELIF = re.compile(r'\bv-else-if\s*=\s*"([^"]*)"')
VELSE = re.compile(r"\bv-else(?!-if)\b")

issues = []

for f in files:
    rel = f.relative_to(ROOT).as_posix()
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<template>(.*?)</template>", t, re.S)
    if not m:
        continue
    tpl = re.sub(r"<!--.*?-->", "", m.group(1), flags=re.S)

    run = []          # 连续的条件兄弟

    def flush():
        if not run:
            return
        kinds = [k for k, _, _ in run]
        ifs = [i for i, k in enumerate(kinds) if k == "if"]
        # 精确判据：只有当「最后一个 v-if」之后还出现 else-if / else 时，
        # 才说明链条被中途插入的 v-if 截断了。
        # （若 else 链已完整结束、后面的 v-if 另起一条链，属正常，不报）
        if len(ifs) >= 2:
            last_if = ifs[-1]
            if any(k in ("else-if", "else") for k in kinds[last_if + 1:]):
                issues.append((rel, list(run), last_if))
        run.clear()

    for line in tpl.split("\n"):
        s = line.strip()
        mm = TAG.match(line)
        if not mm:
            continue
        tag, attrs = mm.group(1), mm.group(2)
        if VELIF.search(attrs):
            run.append(("else-if", f"<{tag}> {VELIF.search(attrs).group(1)}", s[:88]))
        elif VELSE.search(attrs):
            run.append(("else", f"<{tag}> (else)", s[:88]))
        elif VIF.search(attrs):
            run.append(("if", f"<{tag}> {VIF.search(attrs).group(1)}", s[:88]))
        elif tag in ("view", "text", "image", "video", "swiper", "block",
                     "scroll-view", "button", "input", "textarea"):
            flush()        # 无条件的兄弟 → 链结束
    flush()

print(f"扫描 {len(files)} 个文件\n")
if issues:
    print(f"⚠️  发现 {len(issues)} 处 v-if 链被截断：\n")
    for rel, run, last_if in issues:
        print(f"  {rel}")
        for i, (k, cond, raw) in enumerate(run):
            mark = "   ← 截断点（此处新起 v-if，下面的 else 挂到了它身上）" if i == last_if else ""
            print(f"      [{k:>7}] {cond}{mark}")
        print()
else:
    print("✅ 全站未发现 v-if 链被截断的情况")
