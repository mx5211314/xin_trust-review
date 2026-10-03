<script>
export default {
  onLaunch() {
    console.log('App launched')
  }
}
</script>

<style>
/* ============================================================
   本地点评 · 全局设计系统 v4
   —— 落地自《本地生活点评-UI原型-v4》
   业务：本地生活点评（吃 + 玩 + 景区 + 亲子），非美食 App
   ------------------------------------------------------------
   约定
   1. 颜色一律用 var(--dp-*)，深浅色自动切换，不要写死 hex
   2. 字号只取 8 档（--fs-*），间距只取 8 档（--sp-*）
   3. 品牌红双色分工：--dp-brand-deep 用于文字/交互，
      --dp-brand 仅用于大字号(≥18px)/暗底/图形
   ============================================================ */

/* ---------- 1. 令牌 · 浅色 ---------- */
page {
  /* 中性色：暖调（原为冷灰 #f7f8fa，与"城市行走"主题不搭） */
  --dp-bg: #faf8f5;          /* 页面背景 · 暖纸 */
  --dp-card: #ffffff;        /* 卡片/面板 */
  --dp-soft: #f4f1ec;        /* 输入框/浅色块 */
  --dp-text: #1c1917;        /* 主文字 · 暖墨 16.8:1 */
  --dp-text2: #4d4842;       /* 次文字 7.8:1 */
  --dp-text3: #726a63;       /* 弱文字/元信息 4.7:1 达 AA */
  --dp-text4: #a89f96;       /* 占位符 */
  --dp-line: #ece7e0;        /* 分割线 */
  --dp-accent-soft: #fff1f2; /* 品牌红浅底 */

  /* 品牌：双色分工，勿混用 */
  --dp-brand-deep: #d81228;  /* 正文/交互/白字按钮；浅底 4.87:1 */
  --dp-brand: #ff2442;       /* 原色：仅大字号/暗底/图形 3.52:1 */
  --dp-orange: #ff7a45;      /* 暖橙辅色：渐变、榜单 */
  --dp-jade: #0f9d76;        /* 可信认证 / 上升趋势 */

  /* 字阶：8 档（替代原有 26 种，来源《UI 审查与对标报告》） */
  --fs-caption: 20rpx;   /* 10px 角标、极小注释 */
  --fs-sm:      24rpx;   /* 12px 辅助信息、时间戳 */
  --fs-base:    28rpx;   /* 14px 正文主力 */
  --fs-md:      32rpx;   /* 16px 强调正文、列表标题 */
  --fs-lg:      40rpx;   /* 20px 小标题 */
  --fs-xl:      48rpx;   /* 24px 区块标题 */
  --fs-2xl:     64rpx;   /* 32px 页面大标题 */
  --fs-display: 88rpx;   /* 44px 英雄数字 */

  /* 间距：8rpx 刻度（4pt 基准），全站只取这 8 个值 */
  --sp-1: 8rpx;    --sp-2: 16rpx;   --sp-3: 24rpx;   --sp-4: 32rpx;
  --sp-5: 48rpx;   --sp-6: 64rpx;   --sp-7: 96rpx;   --sp-8: 128rpx;

  /* 圆角 */
  --r-sm: 8rpx; --r-md: 16rpx; --r-lg: 28rpx; --r-xl: 40rpx; --r-pill: 999rpx;

  /* 弥散投影（小红书/点评式：低透明度、大扩散，不是硬阴影） */
  --sh-card: 0 2rpx 4rpx rgba(28,25,23,.03), 0 8rpx 24rpx -6rpx rgba(28,25,23,.10);
  --sh-float: 0 4rpx 12rpx -3rpx rgba(216,18,40,.45);

  /* 动效 */
  --e-expo: cubic-bezier(.16, 1, .3, 1);

  /* 底部安全区 */
  --safe-bottom: env(safe-area-inset-bottom);

  background-color: #faf8f5;
  font-size: 28rpx;
  color: #1c1917;
}

/* ---------- 2. 令牌 · 深色 ---------- */
.theme-dark {
  --dp-bg: #14110e;
  --dp-card: #221d19;
  --dp-soft: #2a2420;
  --dp-text: #f5f0e9;
  --dp-text2: #bdb3a8;
  --dp-text3: #8a8076;       /* 暗底 4.66:1 达 AA */
  --dp-text4: #635a52;
  --dp-line: #2e2823;
  --dp-accent-soft: #2b1614;
  --dp-brand-deep: #ff5a6e;  /* 暗底提亮 6.22:1 */
  --dp-orange: #ff9a6b;
  --dp-jade: #4fbf9e;
  --sh-card: 0 2rpx 4rpx rgba(0,0,0,.2), 0 8rpx 24rpx -6rpx rgba(0,0,0,.5);
}

/* ---------- 3. 基础组件 ---------- */
.card {
  background: var(--dp-card);
  border-radius: var(--r-lg);
  margin: var(--sp-2) var(--sp-3);
  padding: var(--sp-4);
  box-shadow: var(--sh-card);
}
.btn-primary {
  background: var(--dp-brand-deep);
  color: #ffffff;
  border-radius: var(--r-pill);
  font-size: var(--fs-base);
  font-weight: 600;
}
.btn-primary.disabled { opacity: .5; }

.tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: var(--r-pill);
  font-size: var(--fs-caption);
}
.tag-reviewer {
  background: var(--dp-accent-soft);
  color: var(--dp-brand-deep);
  display: inline-flex;
  align-items: center;
}
.tag-star { width: 22rpx; height: 22rpx; margin-right: 6rpx; flex-shrink: 0; }
.tag-admin  { background: var(--dp-text); color: var(--dp-card); }
.tag-banned { background: #fcebeb; color: #a32d2d; }

.muted { color: var(--dp-text3); font-size: var(--fs-sm); }

/* 认证徽章（作者名后缀） */
.verify {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  font-size: var(--fs-caption);
  color: var(--dp-jade);
  font-weight: 600;
}
.verify image { width: 20rpx; height: 20rpx; }

/* 关注按钮 */
.follow-btn {
  flex-shrink: 0;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: #ffffff;
  background: var(--dp-brand-deep);
  padding: 10rpx 30rpx;
  border-radius: var(--r-pill);
  box-shadow: var(--sh-float);
}
.follow-btn.done {
  background: var(--dp-soft);
  color: var(--dp-text2);
  box-shadow: none;
}

/* 骨架屏 */
.skeleton {
  background: linear-gradient(90deg, #f4f1ec 25%, #eae4dc 37%, #f4f1ec 63%);
  background-size: 400% 100%;
  animation: sk 1.2s ease infinite;
}
.theme-dark .skeleton {
  background: linear-gradient(90deg, #2a2420 25%, #332b26 37%, #2a2420 63%);
  background-size: 400% 100%;
}
@keyframes sk {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* 点赞红心 */
.like-icon { font-size: 34rpx; color: var(--dp-text3); }
.like-icon.liked { color: var(--dp-brand-deep); }

/* ---------- 4. 内容卡片（双列瀑布流） ---------- */
.wf { display: flex; gap: var(--sp-2); padding: var(--sp-4); }
.wf-col { flex: 1; display: flex; flex-direction: column; gap: var(--sp-2); min-width: 0; }

.mcard {
  background: var(--dp-card);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--sh-card);
}
.mcover { position: relative; overflow: hidden; background: var(--dp-soft); }
.mcover image { width: 100%; height: 100%; display: block; }
.mbody { padding: var(--sp-3); }
.mtitle {
  font-size: var(--fs-base);
  font-weight: 600;
  line-height: 1.45;
  color: var(--dp-text);
  margin-bottom: var(--sp-2);
}
.mpoi {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: var(--fs-caption);
  color: var(--dp-text3);
  margin-bottom: var(--sp-2);
}
.mfoot { display: flex; align-items: center; gap: var(--sp-1); }
.mname { font-size: var(--fs-caption); color: var(--dp-text2); flex: 1; min-width: 0; }
.mlike {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: var(--fs-caption);
  color: var(--dp-text3);
  flex-shrink: 0;
}
.mlike.liked { color: var(--dp-brand-deep); }

/* 封面角标：分类 / 视频 / 时长 */
.cover-badge {
  position: absolute;
  left: var(--sp-2);
  top: var(--sp-2);
  font-size: var(--fs-caption);
  font-weight: 600;
  color: #ffffff;
  background: rgba(28,25,23,.52);
  padding: 4rpx 14rpx;
  border-radius: var(--r-pill);
}
.cover-dur {
  position: absolute;
  right: var(--sp-2);
  bottom: var(--sp-2);
  font-size: var(--fs-caption);
  font-weight: 600;
  color: #ffffff;
  background: rgba(28,25,23,.6);
  padding: 4rpx 12rpx;
  border-radius: var(--r-sm);
}
.cover-play {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 5. 作者与地点（详情页 · 扁平无卡，小红书式） ---------- */
.note-head { display: flex; align-items: center; gap: var(--sp-3); }
.note-head .follow-btn { margin-left: auto; }
.nh-av {
  width: 88rpx;
  height: 88rpx;
  border-radius: var(--r-pill);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: var(--fs-md);
  font-weight: 700;
  background: linear-gradient(140deg, var(--dp-orange), var(--dp-brand-deep));
}
.nh-body { flex: 1; min-width: 0; }
.nh-name {
  font-size: var(--fs-base);
  font-weight: 700;
  color: var(--dp-text);
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.nh-sub { font-size: var(--fs-caption); color: var(--dp-text3); margin-top: 6rpx; }
/* 标题独立成行、占满宽度（别让头像挤成两行） */
.note-title {
  font-size: var(--fs-lg);
  font-weight: 700;
  line-height: 1.42;
  color: var(--dp-text);
  margin-top: var(--sp-4);
}
/* 地点：内联浅色标签，不是卡片 */
.poi-inline {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: var(--sp-4);
  padding: 12rpx var(--sp-3);
  border-radius: var(--r-pill);
  background: var(--dp-accent-soft);
  font-size: var(--fs-sm);
  color: var(--dp-brand-deep);
  font-weight: 500;
  max-width: 100%;
}

/* ---------- 6. 引导式空状态（说清 + 给下一步） ---------- */
.empty {
  padding: var(--sp-8) var(--sp-5);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-3);
}
.empty-ic {
  width: 128rpx;
  height: 128rpx;
  border-radius: var(--r-pill);
  background: var(--dp-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-t { font-size: var(--fs-md); font-weight: 600; color: var(--dp-text); }
.empty-d {
  font-size: var(--fs-sm);
  color: var(--dp-text3);
  line-height: 1.7;
  text-align: center;
}

/* ---------- 7. 底部操作栏（毛玻璃 + 安全区） ---------- */
.glass-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  background: rgba(250,248,245,.88);
  backdrop-filter: blur(36rpx) saturate(1.6);
  border-top: 1rpx solid var(--dp-line);
  padding-bottom: var(--safe-bottom);
}
.theme-dark .glass-bar {
  background: rgba(20,17,14,.88);
}
</style>
