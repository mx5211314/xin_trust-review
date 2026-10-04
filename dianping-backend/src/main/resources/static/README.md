# static/ —— H5 前端构建产物（不入库）

这个目录存放**本地点评 App 的 H5 构建产物**，由后端在同源下一并提供：

```
浏览器 → :18080/            index.html + assets/*
        → :18080/content/**  后端 API
        → :18080/upload/**   图片/视频
```

## 目录被 .gitignore 忽略

这里的文件是**构建输出**（每次构建文件名都带新 hash），入库只会让仓库无限膨胀，
因此 `.gitignore` 忽略了本目录下除本 README 外的所有内容。

**克隆仓库后这个目录是空的，属正常。** 需要按下面步骤自行生成。

## 怎么生成

详见仓库根目录的 `部署说明_让别人访问.md`，三步：

1. **HBuilderX 构建**：打开 `dianping-uniapp` → 发行 → 网站-PC Web或手机H5
   - 前提：`manifest.json` 的 `appid` 不能为空；工程根目录要有 `index.html`
   - 产物落在 `dianping-uniapp/unpackage/dist/build/web/`（注意是 `web` 不是 `h5`）
2. **复制进来**：
   ```bash
   cp -r dianping-uniapp/unpackage/dist/build/web/. \
         dianping-backend/src/main/resources/static/
   ```
3. **重新打包**（打包前务必先停掉正在运行的后端，Windows 会锁住 jar）：
   ```bash
   mvn -o -q package -DskipTests
   ```

## 需要放行哪些路径

`common/WebConfig.java` 的登录拦截器已放行静态资源：
`/`、`/index.html`、`/favicon.ico`、`/assets/**`、`/static/**`。
新增静态资源目录时记得同步补上，否则会被拦成 1002「请先登录」。
