<script>
export default {
  onLaunch() {
    console.log('App launched')
  }
}
</script>

<style>
/* ---------- 主题令牌：浅色为默认，.theme-dark 覆盖（根节点由全局 mixin 注入） ---------- */
page {
  --dp-bg: #f7f8fa;        /* 页面背景 */
  --dp-card: #ffffff;      /* 卡片/面板 */
  --dp-soft: #f6f7f9;      /* 输入框/浅色块 */
  --dp-text: #1f2430;      /* 主文字 */
  --dp-text2: #666666;     /* 次文字 */
  --dp-text3: #999999;     /* 弱文字 */
  --dp-text4: #c2c8d0;     /* 极弱/占位 */
  --dp-line: #f1f3f5;      /* 分割线 */
  --dp-accent-soft: #ffe8ea; /* 品牌红浅底 */
  /* ---- 品牌双色分工（设计系统 v3）----
     --dp-brand-deep  正文/交互用；浅色纸底 AA 4.87:1
     --dp-brand       品牌原色；仅限大字号(≥18px)/暗底/图形，浅色纸底仅 3.52:1
     ⚠ 禁止用 --dp-brand 写小于 18px 的正文（小字不达 WCAG AA） */
  --dp-brand-deep: #d81228;
  --dp-brand: #ff2442;
  --dp-jade: #1f6f5c;      /* 可信认证 / 上升趋势 */
  background-color: #f7f8fa;
  font-size: 28rpx;
  color: #1f2430;
}
.theme-dark {
  --dp-bg: #111214;
  --dp-card: #1e2023;
  --dp-soft: #26282c;
  --dp-text: #f2f3f5;
  --dp-text2: #b0b6bf;
  --dp-text3: #7a8087;
  --dp-text4: #565c63;
  --dp-line: #2a2d31;
  --dp-accent-soft: #3a2226;
  --dp-brand-deep: #ff5a6e;  /* 暗底提亮，6.22:1 */
  --dp-jade: #4fbf9e;
}
/* 品牌红主题（小红书式） */
.card {
  background: var(--dp-card);
  border-radius: 16rpx;
  margin: 16rpx 24rpx;
  padding: 24rpx;
}
.btn-primary {
  /* 白字按钮：底色必须用加深版，原色 #ff2442 配白字仅 3.76:1 不达 AA（加深版 5.20:1） */
  background: var(--dp-brand-deep);
  color: #ffffff;
  border-radius: 44rpx;
  font-size: 30rpx;
}
.btn-primary.disabled {
  opacity: 0.5;
}
.tag {
  display: inline-block;
  padding: 2rpx 16rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}
.tag-reviewer {
  background: var(--dp-accent-soft);
  color: #ff2442;
  display: inline-flex;
  align-items: center;
}
.tag-star {
  width: 22rpx;
  height: 22rpx;
  margin-right: 6rpx;
  flex-shrink: 0;
}
.tag-admin {
  background: #1f2430;
  color: #ffffff;
}
.tag-banned {
  background: #fcebeb;
  color: #a32d2d;
}
.muted {
  color: var(--dp-text3);
  font-size: 24rpx;
}
/* 骨架屏闪烁 */
.skeleton {
  background: linear-gradient(90deg, #f1f2f4 25%, #e8eaed 37%, #f1f2f4 63%);
  background-size: 400% 100%;
  animation: sk 1.2s ease infinite;
}
.theme-dark .skeleton {
  background: linear-gradient(90deg, #26282c 25%, #2e3136 37%, #26282c 63%);
  background-size: 400% 100%;
}
@keyframes sk {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
/* 点赞红心 */
.like-icon {
  font-size: 34rpx;
  color: var(--dp-text3);
}
.like-icon.liked {
  color: #ff2442;
}
</style>
