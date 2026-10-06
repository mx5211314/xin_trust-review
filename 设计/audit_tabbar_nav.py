"""检查是否有 navigateTo 跳向 tabBar 页

tabBar 页只能用 switchTab 打开；用 navigateTo 会静默失败（停在原页面）。
这是 uni-app 的硬限制，也是最容易被忽略的一类"点了没反应"。
"""
import json
import pathlib
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")

raw = re.sub(r"//.*", "", (ROOT / "pages.json").read_text(encoding="utf-8"))
tabs = {p["pagePath"] for p in json.loads(raw).get("tabBar", {}).get("list", [])}

files = (list((ROOT / "pages").rglob("*.vue"))
         + list((ROOT / "pagesAdmin").rglob("*.vue"))
         + list((ROOT / "components").rglob("*.vue"))
         + list((ROOT / "utils").rglob("*.js")))

# 匹配 url 字段，兼容单引号/双引号/反引号
CALL = re.compile(r"navigateTo\(\s*\{\s*url:\s*" + "[\"'`]" + r"([^\"'`]+)")

bad = []
for f in files:
    src = f.read_text(encoding="utf-8")
    src = re.sub(r"<!--.*?-->", "", src, flags=re.S)          # 去 html 注释
    src = re.sub(r"(?m)^\s*(?://|/\*).*$", "", src)           # 去整行注释
    for m in CALL.finditer(src):
        if m.group(1).split("?")[0].lstrip("/") in tabs:
            bad.append((f.relative_to(ROOT).as_posix(), m.group(1)))

print("tabBar 页：", sorted(tabs))
print()
if bad:
    print(f"⚠️  {len(bad)} 处 navigateTo 指向 tabBar 页（会静默失败）：")
    for f, u in bad:
        print(f"   {f}  ->  {u}")
else:
    print("✅ 全站已无 navigateTo 指向 tabBar 页")
