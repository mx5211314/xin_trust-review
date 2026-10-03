"""修复迁移副作用：头部渐变的两个色标被换成同一令牌
   linear-gradient(165deg, A, A 55%, #ffffff)
   → linear-gradient(165deg, A, var(--dp-card) 58%)
   同时把写死的 #ffffff 收进令牌。
"""
import pathlib

ROOT = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp")
OLD = "linear-gradient(165deg, var(--dp-accent-soft), var(--dp-accent-soft) 55%, #ffffff)"
NEW = "linear-gradient(165deg, var(--dp-accent-soft), var(--dp-card) 58%)"

targets = ["pages/browse/browse.vue", "pages/folders/folders.vue",
           "pages/mine/mine.vue", "pages/topics/topics.vue"]

for rel in targets:
    p = ROOT / rel
    t = p.read_text(encoding="utf-8")
    n = t.count(OLD)
    if n:
        p.write_text(t.replace(OLD, NEW), encoding="utf-8")
    print(f"  {n} 处  {rel}")
