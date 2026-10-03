import re, pathlib

BASE = pathlib.Path(r"D:/桌面文件/111/dianping-uniapp/pages")
for p in ["collection", "chat", "following", "topics", "draftbox"]:
    f = BASE / p / f"{p}.vue"
    t = f.read_text(encoding="utf-8")
    m = re.search(r"<style[^>]*>(.*?)</style>", t, re.S)
    if not m:
        print(f"{p}: no style")
        continue
    st = m.group(1)
    fs = sorted(set(int(v) for v in re.findall(r"font-size:\s*(\d+)rpx", st)))
    off = [v for v in fs if v % 8 != 0]
    print(f"{p:<12} {len(fs)} 种: {' '.join(str(x) for x in fs)}")
    print(f"{'':12} 离格值: {' '.join(str(x) for x in off) if off else '无'}")
    print()
