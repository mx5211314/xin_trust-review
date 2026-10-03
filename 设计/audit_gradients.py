"""检查令牌迁移是否把「渐变」改成了纯色
   —— 脚本把 #ff2442 一律替换成 var(--dp-brand)，若同一渐变的两个色标
      原本都是品牌红系，就会变成 var(--X), var(--X) 的平色。
"""
import re, pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

# 抓线性渐变的所有色标，只有当「全部色标都是同一个令牌」时才算真·纯色
GRAD = re.compile(r"linear-gradient\((.*?)\)(?!\w)", re.S)


def stops_of(body):
    """拆出渐变的色标（去掉角度、去掉位置百分比），返回令牌列表"""
    parts = [p.strip() for p in body.split(",")]
    toks = []
    for p in parts:
        m = re.match(r"^(var\(--dp-[a-z0-9-]+\))", p)
        if m:
            toks.append(m.group(1))
        elif re.match(r"^[-\d.]+(deg|rad|turn|%)?$", p):
            continue           # 角度
        elif re.match(r"^var\(--dp-[a-z0-9-]+\)\s+[\d.]+%", p):
            toks.append(p.split()[0])
        else:
            toks.append(p)     # 其他颜色（#fff / transparent 等）
    return toks


hits = []
for f in files:
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    st = m.group(1)
    for i, line in enumerate(st.split("\n"), 1):
        for body in GRAD.findall(line):
            toks = [x for x in stops_of(body) if x.startswith("var(")]
            if len(toks) >= 2 and len(set(toks)) == 1:
                hits.append((f.relative_to(ROOT).as_posix(), i, line.strip()))

print(f"扫描 {len(files)} 个文件")
if hits:
    print(f"\n❌ 发现 {len(hits)} 处「同色标渐变」（迁移副作用）：\n")
    for rel, ln, line in hits:
        print(f"  {rel}:{ln}")
        print(f"      {line}")
else:
    print("\n✅ 未发现同色标渐变")
