"""前后端接口对账：找出「前端在调、后端没提供」的断点

前端调用形如 request({ url: '/user/me' })，动态段写作 ${id}。
后端端点从 @GetMapping("/xxx") 等注解提取，方法级前缀取 @RequestMapping。
"""
import re, pathlib, sys, collections

sys.stdout.reconfigure(encoding="utf-8")
ROOT = pathlib.Path(r"D:/桌面文件/111")
FE = ROOT / "dianping-uniapp"
BE = ROOT / "dianping-backend/src/main/java"

# ---------- 1. 前端调用 ----------
URL = re.compile(r"url:\s*[`'\"]([^`'\"]+)")
calls = collections.Counter()
where = {}
for f in (list(FE.glob("pages/**/*.vue")) + list(FE.glob("pagesAdmin/**/*.vue"))
          + list(FE.glob("utils/*.js")) + list(FE.glob("components/**/*.vue"))):
    t = f.read_text(encoding="utf-8")
    for m in URL.finditer(t):
        u = m.group(1).split("?")[0]
        u = re.sub(r"\$\{[^}]*\}", "{}", u)      # ${id} -> {}
        u = re.sub(r"\{[^}]*\}", "{}", u)
        if not u.startswith("/"):
            continue
        if not re.match(r"^/(auth|user|content|chat|admin|oss|notify|report|wechat|upload)", u):
            continue
        calls[u] += 1
        where.setdefault(u, f.relative_to(FE).as_posix())

# ---------- 2. 后端端点 ----------
# 注意两点：
# 1) 字符类不能排除 '}'，否则 /{id}/approve 会被截断成 /{id
# 2) 必须兼容「全限定注解」写法 @org.springframework...PutMapping("/x")，否则漏端点
MAPPING = re.compile(r'@(?:[A-Za-z][\w.]*\.)?(Get|Post|Put|Delete)Mapping\(\s*(?:value\s*=\s*)?[{"\']*([^"\')\s]*)')
CLASS_MAP = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?[{"\']*([^"\')\s}]*)')
endpoints = set()
for f in BE.rglob("*Controller.java"):
    t = f.read_text(encoding="utf-8")
    cm = CLASS_MAP.search(t)
    prefix = cm.group(1) if cm else ""
    if prefix and not prefix.startswith("/"):
        prefix = "/" + prefix          # 正则吃掉了引号，这里补回前导斜杠
    prefix = prefix.rstrip("/")
    for m in MAPPING.finditer(t):
        p = m.group(2)
        full = (prefix + p) if p.startswith("/") else (prefix + "/" + p if p else prefix)
        endpoints.add(re.sub(r"\{[^}]*\}", "{}", full).rstrip("/") or "/")

# ---------- 3. 对账 ----------
miss = []
for u, n in sorted(calls.items()):
    norm = u.rstrip("/") or "/"
    if norm in endpoints:
        continue
    # 允许前缀匹配（如 /user/{} 匹配 /user/{}/follow）
    if any(e.startswith(norm) or norm.startswith(e) for e in endpoints):
        continue
    miss.append((u, n, where[u]))

print(f"前端调用 {len(calls)} 个路径 · 后端提供 {len(endpoints)} 个端点\n")
if miss:
    print(f"⚠️  {len(miss)} 个前端调用在后端找不到对应端点：")
    for u, n, w in miss:
        print(f"   {n}x  {u:<42} {w}")
else:
    print("✅ 前端调用的接口后端都有对应端点")

print(f"\n后端端点（共 {len(endpoints)}）示例：")
for e in sorted(endpoints)[:0]:
    print("  ", e)
