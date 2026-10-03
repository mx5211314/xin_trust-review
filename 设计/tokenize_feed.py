"""feed.vue 硬编码颜色 → 设计令牌"""
import pathlib

f = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp/pages/feed/feed.vue")
t = f.read_text(encoding="utf-8")
orig = t

REPL = [
    # 装饰指示条：品牌红 → 加深版令牌
    ("background: #ff2442;", "background: var(--dp-brand-deep);"),
    # 头像占位渐变：暖橙 → 品牌红（用令牌）
    ("linear-gradient(135deg, #ffb199, #ff2442)",
     "linear-gradient(135deg, var(--dp-orange), var(--dp-brand-deep))"),
    # 阴影转暖调（原来是纯黑 rgba）
    ("box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);",
     "box-shadow: var(--sh-card);"),
    ("box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);",
     "box-shadow: var(--sh-card);"),
]

counts = []
for a, b in REPL:
    n = t.count(a)
    if n:
        t = t.replace(a, b)
    counts.append((a[:46], n))

f.write_text(t, encoding="utf-8")

print("=== 替换结果 ===")
for a, n in counts:
    print(f"  {n:>2} 处  {a}")
print(f"\n改动: {'有' if t != orig else '无'}")
print("剩余硬编码品牌红:", t.count("#ff2442") + t.count("#FF2442"))
