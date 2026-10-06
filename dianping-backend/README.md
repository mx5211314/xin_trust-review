# 点评平台后端 MVP（SpringBoot 3 单体）

对应《接口契约骨架_v0.1.md》的可运行实现。技术栈：SpringBoot 3.2 / Java 17 / MyBatis-Plus / MySQL / Redis / JWT。

## 快速开始

```bash
# 1. 环境要求：JDK 17+、MySQL 8、Redis、Maven 3.8+
#    （IDEA 里装 Lombok 插件，2023 版以后自带）

# 2. 建库建表
mysql -uroot -p < sql/init.sql

# 3. 改配置 application.yml：数据库密码、Redis 地址、阿里云 OSS 的 AK/SK/Bucket

# 4. 跑起来
mvn spring-boot:run
```

启动后先获取验证码再登录：

```bash
# 1. 下发验证码（dev profile 下 data.devCode 会带回验证码，前端直接展示）
curl -X POST http://localhost:18080/auth/sms/send \
  -H "Content-Type: application/json" -d '{"phone":"13800000000"}'
# → {"code":0,"data":{"devCode":"123456","devMode":true}}

# 2. 用验证码登录（管理员账号由 init.sql 预置）
curl -X POST http://localhost:18080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000000","code":"123456"}'
```

> 本机开发请用 `--spring.profiles.active=dev` 启动（该 profile 会打开
> `dianping.sms.echo-code`，把验证码回显出来，无需真实短信即可走通登录）。

## 目录结构

```
src/main/java/com/dianping/
├── DianpingApplication.java
├── common/     统一返回体/错误码/全局异常/JWT/登录拦截器/分页插件
├── entity/     User / Content / UserAction
├── mapper/     MyBatis-Plus BaseMapper
├── dto/        请求/响应对象（Java record）
├── service/    Auth / Content / Like / Oss / Audit / Admin
├── controller/ auth /user /content /oss /admin 五组接口
└── task/       点赞数 Redis→MySQL 定时同步（5 分钟）
```

## 已实现能力

| 模块 | 接口 | 说明 |
|---|---|---|
| 认证 | POST /auth/sms/send | 下发验证码：6 位随机、5 分钟有效、一次性、有错误次数与发送频率限制 |
| 认证 | POST /auth/login | 手机号+验证码，首登自动注册，返回 JWT(7天) |
| 用户 | GET /user/me、GET /user/{userId} | 我的信息 / 用户主页+TA的点评 |
| 上传 | GET /oss/upload-params | OSS PostObject 表单直传签名，客户端直传不经后端 |
| 内容 | POST /content | 发布点评，仅 REVIEWER；发布后机审（mock 可配） |
| 内容 | GET /content/feed | 信息流，仅 APPROVED，地区可选过滤，分页 |
| 内容 | GET /content/{id} | 详情；作者可见自己的 REJECTED+原因 |
| 点赞 | POST/DELETE /content/{id}/like | 幂等，Redis 计数 + 关系表，重复操作返回 2003 |
| 后台 | /admin/content/* /admin/user/* /admin/reviewer/* | 审核/驳回/下架/封禁/点评人授权，仅 ADMIN |

## 与契约的对应关系

- 统一返回体 `{"code":0,"msg":"ok","data":{}}`，错误码表见 `common/ResultCode`
- **Long 已全局序列化为字符串**（WebConfig），雪花 ID 不会超前端精度
- 内容状态机 PENDING→APPROVED/REJECTED→TAKEN_DOWN，见 `entity/Content`
- 全接口强制登录（含浏览）。若要开放匿名浏览，把 `/content/feed`、`GET /content/{id}` 加入 WebConfig 的 excludePathPatterns

## 待办（按优先级）

1. **OSS 配置**：填上 AK/SK/Bucket 才能真传文件（不填时 upload-params 会返回假签名，直传会 403）
2. **短信发送渠道**：验证码**机制已完成**（随机码 + Redis TTL + 一次性消费 +
   错误次数限制 + 发送频率限制，见 `SmsCodeService`），但**发送渠道只有一个
   打日志的 `LogSmsSender`**。拿到短信资质后新增一个 `SmsSender` 实现类即可，
   机制层不用改。当前开发/演示靠 `dianping.sms.echo-code=true` 把验证码回显给前端。
3. **内容审核**：`AuditService` 是 mock，接阿里云内容安全（步骤已写在类注释里）
4. **视频**：表结构和发布接口已预留 videoKey/coverKey/duration，转码暂未做
5. **Admin 后台页面**：接口已全，页面可以用简单模板或 uniapp 顺手包一个

## 上线前必做

- [ ] 换 JWT secret（application.yml 里标了 TODO）
- [ ] 删掉 MyBatis 的 SQL 打印（application.yml 的 log-impl）
- [ ] OSS AK 收进环境变量，别提交到代码仓库
- [ ] 域名 ICP 备案（要 2~4 周，提前办）+ HTTPS
- [ ] Redis 和 MySQL 别裸奔在公网
