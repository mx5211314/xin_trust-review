"""把 Controller 里所有分页调用接上 PageParam 归一化。

替换形态固定：`page, pageSize` → `PageParam.page(page), PageParam.size(pageSize)`
（已核对 16 处调用点全部是这种连写形态，无歧义）

用 FQN 而不是加 import，跟随本仓库 Controller 既有的写法
（同文件里 UserContext 也是全限定调用）。
"""
import pathlib
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-backend/src/main/java")
FQN = "com.dianping.common.PageParam"
OLD = "page, pageSize"
NEW = f"{FQN}.page(page), {FQN}.size(pageSize)"

total = 0
for f in sorted(ROOT.rglob("*Controller.java")):
    src = f.read_text(encoding="utf-8")
    if OLD not in src:
        continue
    n = src.count(OLD)
    # 幂等：已处理过的文件跳过
    if NEW in src:
        print(f"  跳过（已处理） {f.name}")
        continue
    out = src.replace(OLD, NEW)
    f.write_text(out, encoding="utf-8")
    total += n
    print(f"  {f.name}: 替换 {n} 处")

print(f"\n共替换 {total} 处")
