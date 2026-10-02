<template>
  <view class="page" :class="{'theme-dark': isDark}" v-if="content">
    <!-- 驳回提示（仅作者可见） -->
    <view v-if="isRejectedMine" class="reject-bar">
      <text class="reject-text">未通过审核：{{ content.rejectReason || '内容不符合规范' }}</text>
    </view>

    <!-- 沉浸式图区（黑底，custom 导航） -->
    <view class="media" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-ghost" :style="{ top: statusBarHeight + 'px' }">
        <view class="nav-btn" @tap="goBack">
          <view class="chev"></view>
        </view>
        <view class="nav-btn nav-more" @tap="showMore">
          <view class="dots-row">
            <view class="ndot"></view>
            <view class="ndot"></view>
            <view class="ndot"></view>
          </view>
        </view>
      </view>
      <swiper
        v-if="content.images && content.images.length"
        class="banner"
        :indicator-dots="content.images.length > 1"
        indicator-color="rgba(255,255,255,0.4)"
        indicator-active-color="#FFFFFF"
        circular
        @change="onSwiperChange"
      >
        <swiper-item v-for="(img, i) in content.images" :key="i">
          <image
            class="banner-img"
            :src="img"
            mode="aspectFill"
            @tap="preview(i)"
            @longpress="saveImage(i)"
            @error="imgError = true"
          />
          <view v-if="imgError && currentImage === i" class="img-err-tip">
            <text class="img-err-text">图片加载失败 · 点击重试</text>
          </view>
        </swiper-item>
      </swiper>
      <view v-if="content.images && content.images.length > 1" class="page-badge">
        <text class="page-badge-text">{{ currentImage + 1 }}/{{ content.images.length }}</text>
      </view>
      <video
        v-else-if="content.videoUrl"
        class="banner"
        :src="content.videoUrl"
        :poster="content.coverUrl"
        controls
        object-fit="contain"
      />
      <view v-else class="banner banner-empty">
        <text class="banner-empty-text">视频加载失败，请检查网络后重试</text>
      </view>
    </view>

    <!-- 内容区 -->
    <view class="body">
      <text class="title">{{ content.title }}</text>
      <view class="author-row" @tap="goAuthor">
        <view class="avatar-ph avatar">
          <text class="avatar-text">{{ shortName }}</text>
        </view>
        <text class="nickname">{{ content.author ? content.author.nickname : '匿名' }}</text>
        <view v-if="isReviewerAuthor" class="tag tag-reviewer">
          <image class="tag-star" src="/static/icons/star-red.png" mode="aspectFit" />
          <text>{{ content.author.role === 'ADMIN' ? '管理员' : '点评人' }}</text>
        </view>
        <view
          v-if="!isMyContent"
          class="follow-mini"
          :class="{ on: authorFollowing }"
          @tap.stop="toggleFollowAuthor"
        >
          <text class="follow-mini-text">{{ authorFollowing ? '已关注' : '+ 关注' }}</text>
        </view>
      </view>
      <view class="text">
        <text
          v-for="(p, i) in textParts"
          :key="i"
          :class="['seg', { mention: p.t === 'mention' }]"
          @tap.stop="p.t === 'mention' && goUser(p.v)"
        >{{ p.t === 'mention' ? '@' + p.v : p.v }}</text>
      </view>

      <!-- 话题标签 -->
      <view class="topic-row" v-if="content.tags && content.tags.length">
        <text
          v-for="(t, i) in content.tags"
          :key="i"
          class="topic"
          @tap="goTopic(t)"
        >#{{ t }}</text>
      </view>

      <!-- 地点 -->
      <view class="poi-row" v-if="content.poiName">
        <view class="poi tap" @tap="goPoi(content.poiName)">
          <image class="poi-ico" src="/static/icons/location.png" />
          <text class="poi-text">{{ content.poiName }}</text>
        </view>
        <text class="region" v-if="content.regionCode">{{ regionName(content.regionCode) }}</text>
      </view>
      <text class="time">编辑于 {{ content.createTime }}</text>
    </view>

    <!-- 评论区 -->
    <view class="comments">
      <view class="comments-head">
        <text class="comments-title">评论 {{ commentTotal }}</text>
      </view>
      <view v-for="c in comments" :key="c.commentId" class="comment-item" @longpress="onCommentLong(c)">
        <view class="cavatar">
          <text class="cavatar-text">{{ c.user ? c.user.nickname.slice(0, 1) : '客' }}</text>
        </view>
        <view class="cbody">
          <view class="crow">
            <view class="cmain">
              <view class="cnick-row">
                <text class="cnick">{{ c.user ? c.user.nickname : '匿名' }}</text>
                <text class="ctime">{{ c.createTime }}</text>
              </view>
              <text class="ctext">{{ c.text }}</text>
              <view class="cops">
                <text class="creply" @tap="tapReply(c, null)">回复</text>
                <text v-if="isMyComment(c)" class="cdel" @tap="delComment(c)">删除</text>
              </view>
            </view>
            <!-- 点赞：竖排在评论最右侧（小红书式） -->
            <view class="clike-col" @tap="likeComment(c)">
              <image class="clike-img" :src="c.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
              <text class="clike-num">{{ c.likeCount > 0 ? c.likeCount : '' }}</text>
            </view>
          </view>

          <!-- 楼中楼：子回复缩进挂在本评论下（同样带头像） -->
          <view v-if="c.replies && c.replies.length" class="replies">
            <view v-for="r in c.replies" :key="r.commentId" class="reply-item">
              <view class="ravatar">
                <text class="ravatar-text">{{ r.user ? r.user.nickname.slice(0, 1) : '客' }}</text>
              </view>
              <view class="rbody">
                <view class="cnick-row">
                  <text class="cnick rnick">{{ r.user ? r.user.nickname : '匿名' }}</text>
                  <text class="ctime">{{ r.createTime }}</text>
                </view>
                <text class="rtext">
                  <text v-if="r.replyToNickname && (!r.user || r.user.nickname !== r.replyToNickname)" class="reply-tag">回复 @{{ r.replyToNickname }}：</text>{{ r.text }}
                </text>
                <view class="cops">
                  <text class="creply" @tap="tapReply(c, r)">回复</text>
                  <text v-if="isMyComment(r)" class="cdel" @tap="delComment(r)">删除</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view v-if="!comments.length" class="empty">
        <text class="muted">还没有评论，来抢沙发～</text>
      </view>
      <view v-if="comments.length && loadedRoots < rootTotal" class="more-comments" @tap="loadMoreComments">
        <text class="more-comments-text">查看更多评论（{{ rootTotal - loadedRoots }}）</text>
      </view>
    </view>

    <!-- 悬浮操作栏（小红书式：输入前显示三图标，输入后变"发送"） -->
    <view class="footer">
      <view class="input-wrap">
        <input
          v-model="commentText"
          class="comment-input"
          :placeholder="replyTarget ? '回复 @' + replyTarget.nickname : '说点什么...'"
          placeholder-class="ph"
          :focus="inputFocus"
          confirm-type="send"
          @confirm="sendComment"
        />
        <view v-if="replyTarget" class="input-clear" @tap="cancelReply">
          <text class="input-clear-text">✕</text>
        </view>
      </view>

      <!-- 有输入内容：显示发送 -->
      <view v-if="commentText.trim()" class="send-btn" @tap="sendComment">
        <text class="send-text">发送</text>
      </view>
      <!-- 无输入：显示 赞/藏/评论 三图标 -->
      <view v-else class="acts">
        <view class="act" @tap="doLike">
          <image class="act-img" :src="content.liked ? '/static/icons/heart-on.png' : '/static/icons/heart.png'" />
          <text class="act-num">{{ fmtNum(content.likeCount) }}</text>
        </view>
        <view class="act" @tap="doFav">
          <image class="act-img" :src="favorited ? '/static/icons/star-on.png' : '/static/icons/star.png'" />
          <text class="act-num">{{ fmtNum(favoriteCount) }}</text>
        </view>
        <view class="act">
          <image class="act-img" src="/static/icons/bubble.png" />
          <text class="act-num">{{ fmtNum(commentTotal) }}</text>
        </view>
      </view>
    </view>

    <!-- 收藏夹选择面板（底部弹层） -->
    <view v-if="favPanelShow" class="fav-mask" @tap="favPanelShow = false">
      <view class="fav-sheet" @tap.stop>
        <view class="fav-sheet-head">
          <text class="fav-sheet-title">{{ favorited ? '管理收藏' : '收藏到' }}</text>
          <text class="fav-sheet-close" @tap="favPanelShow = false">✕</text>
        </view>
        <scroll-view class="fav-sheet-list" scroll-y="true">
          <view
            v-for="f in folders"
            :key="f.folderId"
            class="fav-row"
            @tap="chooseFolder(f.folderId)"
          >
            <text class="fav-row-icon">📁</text>
            <text class="fav-row-name">{{ f.name }}</text>
            <text class="fav-row-count">{{ f.count }}</text>
          </view>
          <view class="fav-row fav-row-add" @tap="createFolderInPanel">
            <text class="fav-row-icon">＋</text>
            <text class="fav-row-name">新建收藏夹</text>
          </view>
        </scroll-view>
        <view v-if="favorited" class="fav-cancel" @tap="cancelFavFromPanel">
          <text class="fav-cancel-text">取消收藏</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request'
import { regionName } from '@/utils/config'
import { getUser } from '@/utils/auth'

export default {
  data() {
    return {
      id: '',
      content: null,
      statusBarHeight: 20,
      comments: [],
      commentTotal: 0,
      rootTotal: 0,
      loadedRoots: 0,
      commentPage: 1,
      currentImage: 0,
      imgError: false,
      commentText: '',
      replyTarget: null,
      inputFocus: false,
      authorFollowing: false,
      favorited: false,
      favoriteCount: 0,
      favPanelShow: false,
      folders: [],
      isBlockedAuthor: false,
      reportReasons: ['色情低俗', '广告诈骗', '违法违规', '不实信息', '其他']
    }
  },
  computed: {
    /** 正文分段：普通文本 / @昵称（可点跳转用户主页） */
    textParts() {
      const text = this.content ? this.content.text : ''
      if (!text) return []
      const re = /@([^\s@，。！？!?,，、]+)/g
      const parts = []
      let last = 0
      let m
      while ((m = re.exec(text)) !== null) {
        if (m.index > last) parts.push({ t: 'text', v: text.slice(last, m.index) })
        parts.push({ t: 'mention', v: m[1] })
        last = m.index + m[0].length
      }
      if (last < text.length) parts.push({ t: 'text', v: text.slice(last) })
      return parts
    },
    isRejectedMine() {
      const me = getUser()
      return (
        this.content &&
        this.content.status === 'REJECTED' &&
        me &&
        this.content.author &&
        String(me.userId) === String(this.content.author.userId)
      )
    },
    isReviewerAuthor() {
      // 真判断：只有点评人/管理员挂徽标（原为恒真占位，普通用户也会显示"点评人"）
      const r = this.content && this.content.author && this.content.author.role
      return r === 'REVIEWER' || r === 'ADMIN'
    },
    isMyContent() {
      const me = getUser()
      return !!(me && this.content && this.content.author
        && String(me.userId) === String(this.content.author.userId))
    },
    shortName() {
      const a = this.content && this.content.author
      return a && a.nickname ? a.nickname.slice(0, 1) : '客'
    },
    authorId() {
      const a = this.content && this.content.author
      return a && a.userId ? a.userId : ''
    }
  },
  onLoad(query) {
    this.id = query.id
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    } catch (e) { /* 默认值兜底 */ }
  },
  onShow() {
    this.fetch()
  },
  methods: {
    regionName,
    async fetch() {
      try {
        this.content = await request({ url: `/content/${this.id}` })
        if (this.content) {
          this.favorited = !!this.content.favorited
          this.favoriteCount = this.content.favoriteCount || 0
          if (this.content.status === 'APPROVED') {
            // 浏览计数上报（静默，失败不影响展示）
            request({ url: `/content/${this.id}/view`, method: 'POST', silent: true }).catch(() => {})
          }
          this.loadComments()
          this.loadAuthorFollow()
          this.loadBlockState()
        }
      } catch (e) {
        uni.showToast({ title: '内容不存在', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 800)
      }
    },
    async loadComments() {
      this.commentPage = 1
      try {
        const data = await request({
          url: `/content/${this.id}/comments?page=1&pageSize=10`
        })
        this.comments = (data.list || []).map(c => ({ ...c, liked: false }))
        this.commentTotal = data.total || 0
        this.rootTotal = data.rootTotal || this.comments.length
        this.loadedRoots = this.comments.length
      } catch (e) { /* ignore */ }
    },
    async loadMoreComments() {
      this.commentPage += 1
      try {
        const data = await request({
          url: `/content/${this.id}/comments?page=${this.commentPage}&pageSize=10`
        })
        this.comments = this.comments.concat((data.list || []).map(c => ({ ...c, liked: false })))
        this.loadedRoots = this.comments.length
      } catch (e) { /* ignore */ }
    },
    onSwiperChange(e) {
      this.currentImage = e.detail.current
      this.imgError = false
    },
    saveImage(index) {
      uni.showActionSheet({
        itemList: ['保存到相册'],
        success: () => {
          uni.downloadFile({
            url: this.content.images[index],
            success: (res) => {
              uni.saveImageToPhotosAlbum({
                filePath: res.tempFilePath,
                success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
                fail: () => uni.showToast({ title: '保存失败，请检查相册权限', icon: 'none' })
              })
            },
            fail: () => uni.showToast({ title: '图片下载失败', icon: 'none' })
          })
        }
      })
    },
    goTopic(tag) {
      uni.navigateTo({ url: '/pages/collection/collection?mode=topic&q=' + encodeURIComponent(tag) })
    },
    goPoi(name) {
      if (!name) return
      uni.navigateTo({ url: '/pages/collection/collection?mode=shop&q=' + encodeURIComponent(name) })
    },
    async goUser(nick) {
      if (!nick) return
      try {
        const data = await request({ url: '/user/by-nickname?nick=' + encodeURIComponent(nick), silent: true })
        if (data && data.userId) {
          uni.navigateTo({ url: '/pages/user/user?userId=' + data.userId })
        }
      } catch (e) { /* ignore */ }
    },
    async likeComment(c) {
      if (c.liked) {
        try {
          await request({ url: `/content/comments/${c.commentId}/like`, method: 'DELETE', silent: true })
          c.liked = false
          c.likeCount = Math.max(0, (c.likeCount || 0) - 1)
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/comments/${c.commentId}/like`, method: 'POST', silent: true })
          c.liked = true
          c.likeCount = (c.likeCount || 0) + 1
          uni.showToast({ title: '已点赞', icon: 'none', duration: 800 })
        } catch (e) {
          if (e.code === 2003) c.liked = true
        }
      }
    },
    isMyComment(c) {
      const me = getUser()
      return !!(me && c.user && String(me.userId) === String(c.user.userId))
    },
    /** 评论长按：删除（自己）/ 举报 */
    onCommentLong(c) {
      const items = []
      if (this.isMyComment(c)) items.push('删除')
      items.push('举报')
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          const label = items[res.tapIndex]
          if (label === '删除') {
            this.delComment(c)
          } else if (label === '举报') {
            this.reportComment(c.commentId)
          }
        }
      })
    },
    /** 举报评论：选原因后提交（雪花 ID 按字符串传） */
    reportComment(commentId) {
      uni.showActionSheet({
        itemList: this.reportReasons,
        success: async (res) => {
          const reason = this.reportReasons[res.tapIndex]
          try {
            await request({
              url: '/report',
              method: 'POST',
              data: { targetType: 'COMMENT', targetId: commentId, reason },
              silent: true
            })
            uni.showToast({ title: '举报已提交，感谢反馈', icon: 'none' })
          } catch (e) {
            if (e && e.code === 2003) uni.showToast({ title: '已举报，等待处理', icon: 'none' })
          }
        }
      })
    },
    delComment(c) {
      uni.showModal({
        title: '删除评论',
        content: c.text,
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({
              url: `/content/${this.id}/comments/${c.commentId}`,
              method: 'DELETE',
              silent: true
            })
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadComments()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    async sendComment() {
      const text = this.commentText.trim()
      if (!text) return
      try {
        await request({
          url: `/content/${this.id}/comments`,
          method: 'POST',
          data: {
            text,
            parentId: this.replyTarget ? this.replyTarget.commentId : undefined
          }
        })
        this.commentText = ''
        this.replyTarget = null
        this.inputFocus = false
        this.loadComments()
        uni.showToast({ title: '评论成功', icon: 'none' })
      } catch (e) { /* toast 已提示 */ }
    },
    /** 回复：root=顶级评论对象；sub=子回复对象（回复楼中楼里某条时传） */
    tapReply(root, sub) {
      this.replyTarget = {
        commentId: root.commentId,
        nickname: sub && sub.user ? sub.user.nickname : (root.user ? root.user.nickname : '匿名')
      }
      // 聚焦输入框（先复位再置位，保证重复点击也能唤起键盘）
      this.inputFocus = false
      this.$nextTick(() => { this.inputFocus = true })
    },
    cancelReply() {
      this.replyTarget = null
      this.inputFocus = false
    },
    /** 数字友好显示：1000 -> 1.0k */
    fmtNum(n) {
      const v = Number(n || 0)
      return v >= 1000 ? (v / 1000).toFixed(1) + 'k' : String(v)
    },
    async loadAuthorFollow() {
      const a = this.content && this.content.author
      if (!a || this.isMyContent) return
      try {
        const data = await request({ url: `/user/${a.userId}?page=1&pageSize=1` })
        this.authorFollowing = !!(data.profile && data.profile.following)
      } catch (e) { /* ignore */ }
    },
    /** 加载是否拉黑了作者 */
    async loadBlockState() {
      const a = this.content && this.content.author
      if (!a || this.isMyContent) {
        this.isBlockedAuthor = false
        return
      }
      try {
        const list = await request({ url: '/user/blocks', silent: true }) || []
        this.isBlockedAuthor = list.some(b => String(b.userId) === String(a.userId))
      } catch (e) {
        this.isBlockedAuthor = false
      }
    },
    async toggleFollowAuthor() {
      const a = this.content && this.content.author
      if (!a) return
      if (this.authorFollowing) {
        try {
          await request({ url: `/user/${a.userId}/follow`, method: 'DELETE', silent: true })
          this.authorFollowing = false
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/user/${a.userId}/follow`, method: 'POST', silent: true })
          this.authorFollowing = true
          uni.showToast({ title: '已关注 TA', icon: 'none' })
        } catch (e) {
          if (e.code === 2003) this.authorFollowing = true
        }
      }
    },
    async doFav() {
      // 打开收藏夹选择面板（已收藏则用于管理/移动/取消）
      this.favPanelShow = true
      if (!this.folders.length) {
        try {
          this.folders = await request({ url: '/user/favorite/folders', silent: true }) || []
        } catch (e) {
          this.folders = []
        }
      }
    },
    /** 收藏到指定收藏夹（已收藏时=移动，不重复加计数） */
    async chooseFolder(folderId) {
      const wasFav = this.favorited
      try {
        await request({
          url: `/content/${this.id}/favorite?folderId=${folderId}`,
          method: 'POST',
          silent: true
        })
        this.favorited = true
        if (!wasFav) this.favoriteCount += 1
        this.favPanelShow = false
        uni.showToast({ title: wasFav ? '已移动到该收藏夹' : '已收藏 ★', icon: 'none', duration: 900 })
      } catch (e) { /* toast 已提示 */ }
    },
    /** 取消收藏 */
    async cancelFavFromPanel() {
      try {
        await request({ url: `/content/${this.id}/favorite`, method: 'DELETE', silent: true })
        this.favorited = false
        this.favoriteCount = Math.max(0, this.favoriteCount - 1)
        this.favPanelShow = false
        uni.showToast({ title: '已取消收藏', icon: 'none', duration: 900 })
      } catch (e) { /* ignore */ }
    },
    /** 面板内新建收藏夹 */
    createFolderInPanel() {
      uni.showModal({
        title: '新建收藏夹',
        editable: true,
        placeholderText: '如：美食清单',
        success: async (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) return uni.showToast({ title: '名称不能为空', icon: 'none' })
          try {
            await request({ url: '/user/favorite/folder', method: 'POST', data: { name } })
            this.folders = await request({ url: '/user/favorite/folders', silent: true }) || []
            uni.showToast({ title: '已创建', icon: 'success' })
          } catch (e) { /* toast 已提示 */ }
        }
      })
    },
    preview(index) {
      uni.previewImage({ urls: this.content.images, current: index })
    },
    goBack() {
      uni.navigateBack()
    },
    showMore() {
      const items = ['复制链接', '分享给好友']
      if (!this.isMyContent && this.authorId) {
        items.push(this.isBlockedAuthor ? '取消拉黑' : '拉黑作者')
      }
      items.push('举报')
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          const label = items[res.tapIndex]
          if (label === '复制链接') {
            uni.setClipboardData({
              data: `本地点评 /pages/detail/detail?id=${this.id}`,
              success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
            })
          } else if (label === '分享给好友') {
            this.shareToFriend()
          } else if (label === '拉黑作者') {
            this.toggleBlockAuthor(true)
          } else if (label === '取消拉黑') {
            this.toggleBlockAuthor(false)
          } else if (label === '举报') {
            this.reportContent()
          }
        }
      })
    },
    /** 举报当前内容：选原因后提交（雪花 ID 一律按字符串传，Number 会精度失真） */
    reportContent() {
      uni.showActionSheet({
        itemList: this.reportReasons,
        success: async (res) => {
          const reason = this.reportReasons[res.tapIndex]
          try {
            await request({
              url: '/report',
              method: 'POST',
              data: { targetType: 'CONTENT', targetId: this.id, reason },
              silent: true
            })
            uni.showToast({ title: '举报已提交，感谢反馈', icon: 'none' })
          } catch (e) {
            if (e && e.code === 2003) uni.showToast({ title: '已举报，等待处理', icon: 'none' })
          }
        }
      })
    },
    /** 拉黑/取消拉黑作者 */
    async toggleBlockAuthor(block) {
      if (!this.authorId) return
      try {
        if (block) {
          await request({ url: `/user/${this.authorId}/block`, method: 'POST', silent: true })
          this.isBlockedAuthor = true
          uni.showToast({ title: '已拉黑，不再看到TA的内容', icon: 'none' })
        } else {
          await request({ url: `/user/${this.authorId}/block`, method: 'DELETE', silent: true })
          this.isBlockedAuthor = false
          uni.showToast({ title: '已解除拉黑', icon: 'none' })
        }
      } catch (e) { /* ignore */ }
    },
    shareToFriend() {
      const title = this.content ? this.content.title : '本地点评'
      const path = '/pages/detail/detail?id=' + this.id
      // #ifdef APP-PLUS
      uni.share({
        provider: 'weixin',
        scene: 'WXSceneSession',
        type: 0,
        href: 'https://dianping.demo' + path,
        title,
        summary: title,
        success: () => uni.showToast({ title: '已调起分享', icon: 'none' }),
        fail: () => uni.showToast({ title: '当前未安装微信或环境不支持', icon: 'none' })
      })
      // #endif
      // #ifndef APP-PLUS
      uni.showToast({ title: '请点右上角 ··· 转发给好友', icon: 'none' })
      // #endif
    },
    goAuthor() {
      if (this.content.author) {
        uni.navigateTo({
          url: '/pages/user/user?userId=' + this.content.author.userId
        })
      }
    },
    async doLike() {
      const c = this.content
      if (c.liked) {
        try {
          await request({ url: `/content/${c.contentId}/like`, method: 'DELETE', silent: true })
          c.liked = false
          c.likeCount = Math.max(0, c.likeCount - 1)
          uni.showToast({ title: '已取消点赞', icon: 'none', duration: 900 })
        } catch (e) { /* ignore */ }
      } else {
        try {
          await request({ url: `/content/${c.contentId}/like`, method: 'POST', silent: true })
          c.liked = true
          c.likeCount += 1
          uni.showToast({ title: '已点赞 ♥', icon: 'none', duration: 900 })
        } catch (e) {
          if (e.code === 2003) c.liked = true
        }
      }
    }
  },
  onShareAppMessage() {
    return { title: this.content ? this.content.title : '本地点评' }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--dp-card);
  padding-bottom: 150rpx;
}
.reject-bar {
  background: #fcebeb;
  padding: 20rpx 24rpx;
}
.reject-text {
  color: #a32d2d;
  font-size: 26rpx;
}
.media {
  background: #111111;
  position: relative;
}
.nav-ghost {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
}
/* 毛玻璃白钮：与右侧微信胶囊同基线，深浅图上都清晰 */
.nav-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16rpx);
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}
/* CSS 画返回箭头（加粗 chevron，替代细弱的 ‹ 字符） */
.chev {
  width: 20rpx;
  height: 20rpx;
  border-left: 5rpx solid #1f2430;
  border-bottom: 5rpx solid #1f2430;
  transform: rotate(45deg);
  margin-left: 8rpx;
}
.nav-more {
  margin-left: auto;
}
.dots-row {
  display: flex;
  align-items: center;
  gap: 6rpx;
}
.ndot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #1f2430;
}
.banner {
  width: 100%;
  height: 560rpx;
}
.banner-img {
  width: 100%;
  height: 560rpx;
}
.banner-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-empty-text {
  color: var(--dp-text2);
  font-size: 26rpx;
}
.body {
  padding: 28rpx 28rpx 0;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 500;
  line-height: 1.45;
  margin-bottom: 24rpx;
}
.author-row {
  display: flex;
  align-items: center;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: var(--dp-accent-soft);
  margin-right: 16rpx;
}
.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 28rpx;
  color: #ff2442;
}
.nickname {
  font-size: 28rpx;
  color: var(--dp-text);
  margin-right: 12rpx;
}
.text {
  display: block;
  margin-top: 26rpx;
  font-size: 30rpx;
  line-height: 1.75;
  color: var(--dp-text);
  white-space: pre-wrap;
}
.text .seg {
  display: inline;
}
.text .mention {
  color: #4a90d9;
  font-weight: 500;
}
.topic-row {
  margin-top: 26rpx;
  display: flex;
  flex-wrap: wrap;
}
.topic {
  font-size: 26rpx;
  color: #4a90d9;
  margin-right: 22rpx;
}
.poi-row {
  margin-top: 26rpx;
  display: flex;
  align-items: center;
}
.poi {
  font-size: 28rpx;
  color: #ff2442;
  font-weight: 500;
  margin-right: 20rpx;
}
.poi.tap {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  background: #fff5f6;
  border-radius: 999rpx;
}
.poi-ico {
  width: 28rpx;
  height: 28rpx;
  margin-right: 8rpx;
}
.poi-text {
  color: #ff2442;
  font-weight: 500;
}
.region {
  font-size: 24rpx;
  color: var(--dp-text3);
}
.time {
  display: block;
  margin-top: 22rpx;
  font-size: 22rpx;
  color: var(--dp-text4);
}
.comments {
  margin-top: 30rpx;
  border-top: 12rpx solid var(--dp-bg);
  padding: 24rpx 28rpx;
}
.comments-head {
  margin-bottom: 10rpx;
}
.comments-title {
  font-size: 30rpx;
  font-weight: 500;
}
.comment-item {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--dp-soft);
}
.cavatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: var(--dp-accent-soft);
  margin-right: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cavatar-text {
  font-size: 26rpx;
  color: #ff2442;
}
.cbody {
  flex: 1;
  min-width: 0;
}
.crow {
  display: flex;
  align-items: flex-start;
}
.cmain {
  flex: 1;
  min-width: 0;
}
.cnick-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.cnick {
  font-size: 25rpx;
  color: var(--dp-text3);
}
.ctime {
  font-size: 20rpx;
  color: var(--dp-text4);
}
.cops {
  display: flex;
  align-items: center;
  margin-top: 10rpx;
}
.clike-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: 20rpx;
  padding-top: 4rpx;
  flex-shrink: 0;
}

.clike-icon.liked {
  color: #ff2442;
}
.clike-img {
  width: 38rpx;
  height: 38rpx;
}
.clike-num {
  font-size: 20rpx;
  color: var(--dp-text3);
  margin-top: 2rpx;
  min-height: 20rpx;
}
.ctext {
  font-size: 28rpx;
  color: var(--dp-text);
  line-height: 1.5;
}
.empty {
  text-align: center;
  padding: 40rpx 0;
}
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--dp-card);
  border-top: 1rpx solid var(--dp-line);
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
}
.comment-input {
  flex: 1;
  font-size: 26rpx;
  height: 64rpx;
}
.ph {
  color: var(--dp-text4);
}
.send-btn {
  padding: 12rpx 24rpx;
  margin-right: 10rpx;
}
.send-btn.on {
  background: var(--dp-accent-soft);
  border-radius: 24rpx;
}
.send-text {
  color: #ff2442;
  font-size: 26rpx;
  font-weight: 500;
}
.act {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: 22rpx;
}

.act-icon.liked {
  color: #ff2442;
  animation: dpop 0.3s ease;
}
@keyframes dpop {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}
.act-icon.star {
  color: #ff8a3d;
  font-size: 38rpx;
}
.act-icon.star.faved {
  color: #ff8a3d;
}
.act-img {
  width: 46rpx;
  height: 46rpx;
}
.act-num {
  font-size: 20rpx;
  color: var(--dp-text3);
  margin-top: 2rpx;
}
.comment-item.reply {
  margin-left: 60rpx;
  background: #fafafa;
  border-radius: 12rpx;
  padding: 18rpx 20rpx;
}
.reply-tag {
  color: #4a90d9;
}
.creply {
  font-size: 22rpx;
  color: #4a90d9;
  margin-right: 16rpx;
}
.cdel {
  font-size: 22rpx;
  color: #ff2442;
  margin-right: 16rpx;
}
.replies {
  margin-top: 16rpx;
  background: var(--dp-bg);
  border-radius: 12rpx;
  padding: 8rpx 20rpx;
}
.reply-item {
  display: flex;
  align-items: flex-start;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--dp-line);
}
.reply-item:last-child {
  border-bottom: none;
}
.ravatar {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: var(--dp-card);
  border: 1rpx solid #ffe0e4;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 14rpx;
}
.ravatar-text {
  font-size: 22rpx;
  color: #ff2442;
}
.rbody {
  flex: 1;
  min-width: 0;
}
.rnick {
  font-size: 24rpx;
  color: var(--dp-text3);
}
.rtext {
  display: block;
  font-size: 26rpx;
  color: var(--dp-text);
  line-height: 1.5;
}
.input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--dp-soft);
  border-radius: 32rpx;
  padding: 0 20rpx;
  margin-right: 20rpx;
  height: 64rpx;
}
.input-clear {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: var(--dp-text4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.input-clear-text {
  color: #ffffff;
  font-size: 20rpx;
  line-height: 36rpx;
}
.acts {
  display: flex;
  align-items: center;
}
.act-icon.cmt {
  font-size: 34rpx;
}
.follow-mini {
  margin-left: auto;
  border: 1.5rpx solid #ff2442;
  border-radius: 999rpx;
  padding: 8rpx 26rpx;
}
.follow-mini.on {
  border-color: var(--dp-text4);
}
.follow-mini-text {
  font-size: 24rpx;
  color: #ff2442;
}
.follow-mini.on .follow-mini-text {
  color: var(--dp-text3);
}
.img-err-tip {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  text-align: center;
}
.img-err-text {
  color: var(--dp-text3);
  font-size: 24rpx;
}
.page-badge {
  position: absolute;
  right: 24rpx;
  top: 100rpx;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 20rpx;
  padding: 4rpx 18rpx;
}
.page-badge-text {
  color: #ffffff;
  font-size: 22rpx;
}
.more-comments {
  text-align: center;
  padding: 26rpx 0;
}
.more-comments-text {
  font-size: 26rpx;
  color: #ff2442;
}
.fav-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}
.fav-sheet {
  width: 100%;
  background: var(--dp-card);
  border-radius: 32rpx 32rpx 0 0;
  padding: 20rpx 0 calc(20rpx + env(safe-area-inset-bottom));
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}
.fav-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 32rpx 20rpx;
  border-bottom: 1rpx solid var(--dp-line);
}
.fav-sheet-title {
  font-size: 30rpx;
  font-weight: 500;
  color: var(--dp-text);
}
.fav-sheet-close {
  font-size: 30rpx;
  color: var(--dp-text4);
  padding: 0 10rpx;
}
.fav-sheet-list {
  flex: 1;
  max-height: 50vh;
}
.fav-row {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid var(--dp-soft);
}
.fav-row-icon {
  font-size: 32rpx;
  margin-right: 18rpx;
}
.fav-row-name {
  flex: 1;
  font-size: 28rpx;
  color: var(--dp-text);
}
.fav-row-count {
  font-size: 24rpx;
  color: var(--dp-text4);
  margin-left: 12rpx;
}
.fav-row-add .fav-row-name {
  color: #ff2442;
}
.fav-cancel {
  padding: 24rpx 32rpx;
  text-align: center;
  border-top: 1rpx solid var(--dp-line);
}
.fav-cancel-text {
  font-size: 28rpx;
  color: #ff2442;
}
</style>
