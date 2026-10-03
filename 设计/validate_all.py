"""校验全站改动：花括号配平 + 令牌是否都有定义 + 残留硬编码"""
import re, pathlib, sys

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
files = sorted(list(ROOT.glob("pages/**/*.vue"))
               + list(ROOT.glob("pagesAdmin/**/*.vue"))
               + list(ROOT.glob("components/**/*.vue")))

# 收集 App.vue 里定义的令牌
app = (ROOT / "App.vue").read_text(encoding="utf-8")
defined = set(re.findall(r"(--dp-[a-z0-9-]+)\s*:", app))

bad = []
for f in files:
    rel = f.relative_to(ROOT).as_posix()
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        continue
    st = m.group(1)
    issues = []
    if st.count("{") != st.count("}"):
        issues.append(f"花括号 {st.count('{')}/{st.count('}')} 不配平")
    undef = set(re.findall(r"var\((--dp-[a-z0-9-]+)", st)) - defined
    if undef:
        issues.append(f"未定义令牌 {sorted(undef)}")
    n_brand = len(re.findall(r"#ff2442", st, re.I))
    if n_brand:
        issues.append(f"残留 #ff2442 × {n_brand}")
    if issues:
        bad.append((rel, issues))

print(f"检查 {len(files)} 个文件")
print(f"App.vue 定义令牌 {len(defined)} 个")
print()
if bad:
    print("❌ 发现问题：")
    for rel, iss in bad:
        print(f"  {rel}")
        for i in iss:
            print(f"      - {i}")
else:
    print("✅ 全部通过：花括号配平、令牌均有定义、无残留硬编码品牌红")

# 额外：统计改用令牌的总处数
total_var = sum(len(re.findall(r"var\(--dp-", f.read_text(encoding='utf-8')))
                for f in files)
print(f"\n全站 var(--dp-*) 使用总处数：{total_var}")
