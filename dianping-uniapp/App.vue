<script>
export default {
  onLaunch() {
    console.log('App launched')
  }
}
</script>

<style>
page {
  background-color: #f7f8fa;
  font-size: 28rpx;
  color: #1f2430;
}
/* 品牌红主题（小红书式） */
.card {
  background: #ffffff;
  border-radius: 16rpx;
  margin: 16rpx 24rpx;
  padding: 24rpx;
}
.btn-primary {
  background: #ff2442;
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
  background: #ffe8ea;
  color: #ff2442;
}
.tag-admin {
  background: #eeedfe;
  color: #3c3489;
}
.tag-banned {
  background: #fcebeb;
  color: #a32d2d;
}
.muted {
  color: #999999;
  font-size: 24rpx;
}
/* 骨架屏闪烁 */
.skeleton {
  background: linear-gradient(90deg, #f1f2f4 25%, #e8eaed 37%, #f1f2f4 63%);
  background-size: 400% 100%;
  animation: sk 1.2s ease infinite;
}
@keyframes sk {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
/* 点赞红心 */
.like-icon {
  font-size: 34rpx;
  color: #999999;
}
.like-icon.liked {
  color: #ff2442;
}
</style>
