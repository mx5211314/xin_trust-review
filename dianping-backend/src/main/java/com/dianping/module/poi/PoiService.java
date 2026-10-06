package com.dianping.module.poi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.RegionUtil;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
import com.dianping.module.content.ContentCreateReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店服务。
 *
 * 设计要点见《门店体系改造方案_v0.1.md》，这里只说三件容易做错的事：
 *  1. **谁能建店**：点评人可以建，但新建的进 PENDING 待审；管理员建的直接可用。
 *     门店是公共资产 —— 一个人把地址写错，影响的是所有人的榜单。
 *  2. **待审门店的可见性**：仅创建者本人可见。否则别人在联想列表里
 *     会搜到还没审的店，等于把脏数据提前放出去。
 *  3. **合并用指针不删记录**：`merged_to` 指向"正主"，读取时统一跟随 ——
 *     所以任何拿到门店的地方都应该走 {@link #resolve}，别自己判状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {

    /** 联想结果上限，防止一次拉爆 */
    private static final int SEARCH_LIMIT_MAX = 20;

    private final PoiMapper poiMapper;
    /**
     * 用 Mapper 而不是 ContentService：ContentService 已经依赖了本类，
     * 反过来注入会形成循环依赖。这里只需要读几列，用 Mapper 更干净。
     */
    private final com.dianping.module.content.ContentMapper contentMapper;

    /**
     * 门店联想：按名称模糊匹配 + 城市过滤。
     *
     * 可见性：NORMAL 对所有人可见；PENDING 只对创建者本人可见（见类注释第 2 点）。
     */
    public List<Map<String, Object>> search(Long me, String keyword, String cityCode, Integer limit) {
        int size = limit == null || limit < 1 ? 10 : Math.min(limit, SEARCH_LIMIT_MAX);
        LambdaQueryWrapper<Poi> w = new LambdaQueryWrapper<Poi>()
                .eq(Poi::getStatus, Poi.STATUS_NORMAL)
                .eq(cityCode != null && !cityCode.isBlank(), Poi::getCityCode, cityCode)
                .like(keyword != null && !keyword.isBlank(), Poi::getName, keyword)
                .orderByDesc(Poi::getContentCount)
                .orderByDesc(Poi::getId)
                .last("LIMIT " + size);
        List<Poi> normal = poiMapper.selectList(w);

        // 自己创建的待审门店也一并带出来，否则建完就"找不到了"
        if (me != null && me > 0) {
            LambdaQueryWrapper<Poi> pw = new LambdaQueryWrapper<Poi>()
                    .eq(Poi::getStatus, Poi.STATUS_PENDING)
                    .eq(Poi::getCreatedBy, me)
                    .like(keyword != null && !keyword.isBlank(), Poi::getName, keyword)
                    .orderByDesc(Poi::getId)
                    .last("LIMIT " + size);
            normal.addAll(poiMapper.selectList(pw));
        }
        return normal.stream().map(p -> toVo(p, me)).toList();
    }

    /**
     * 新建门店。
     *
     * @param isAdmin 管理员建的直接 NORMAL；点评人建的进 PENDING 待审
     * @return 新门店 id
     */
    public Long create(Long me, boolean isAdmin, PoiCreateReq req) {
        // 同名 + 同城 已存在时直接复用，避免用户重复建店 ——
        // 这是最廉价的一道"防脏"：大部分重复门店其实是同一个人在两次发布里各建了一次
        String cityCode = RegionUtil.cityCode(req.regionCode());
        String name = req.name().trim();
        Poi existing = findByNameAndCity(name, cityCode);
        if (existing != null) {
            log.info("门店已存在，复用而不新建：name={}, id={}", name, existing.getId());
            return existing.getId();
        }

        Poi p = new Poi();
        p.setName(name);
        p.setAddress(req.address() == null ? "" : req.address().trim());
        p.setLng(req.lng() == null ? null : BigDecimal.valueOf(req.lng()));
        p.setLat(req.lat() == null ? null : BigDecimal.valueOf(req.lat()));
        p.setCategory(req.category() == null ? "" : req.category().trim());
        p.setRegionCode(req.regionCode() == null ? "" : req.regionCode().trim());
        p.setCityCode(cityCode == null ? "" : cityCode);
        p.setPhone(req.phone() == null ? "" : req.phone().trim());
        p.setOpenHours(req.openHours() == null ? "" : req.openHours().trim());
        p.setStatus(isAdmin ? Poi.STATUS_NORMAL : Poi.STATUS_PENDING);
        p.setCreatedBy(me == null ? 0L : me);
        p.setContentCount(0);
        poiMapper.insert(p);
        return p.getId();
    }

    /** 同名同城查重（只比 NORMAL/PENDING，已合并/已停业的不参与） */
    private Poi findByNameAndCity(String name, String cityCode) {
        return poiMapper.selectOne(new LambdaQueryWrapper<Poi>()
                .eq(Poi::getName, name)
                .eq(Poi::getCityCode, cityCode == null ? "" : cityCode)
                .in(Poi::getStatus, Poi.STATUS_NORMAL, Poi.STATUS_PENDING)
                .last("LIMIT 1"));
    }

    /**
     * 取门店并校验可见性。不可见抛 NOT_FOUND（不泄露"存在但你没权限"）。
     * 返回值已跟随 merged_to 解析过。
     */
    public Poi get(Long me, Long id) {
        Poi p = resolve(id);
        if (p == null || !canSee(p, me)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return p;
    }

    /**
     * 校验一个门店能否被"关联到笔记上"。
     *
     * 与 {@link #get} 的区别：这里是要写入引用，要求更严 ——
     * 不能关联到 REJECTED/MERGED/CLOSED 的门店，否则会往废记录上挂笔记。
     * 关联自己的 PENDING 门店是允许的（自己发的笔记先用自己的待审店）。
     *
     * @return 实际应写入的 poiId（若门店已被合并，返回正主的 id）
     */
    public Long requireLinkable(Long me, Long poiId) {
        Poi p = resolve(poiId);
        if (p == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "门店不存在");
        }
        boolean mine = me != null && me.equals(p.getCreatedBy());
        boolean ok = Poi.STATUS_NORMAL.equals(p.getStatus())
                || (Poi.STATUS_PENDING.equals(p.getStatus()) && mine);
        if (!ok) {
            throw new BizException(ResultCode.PARAM_ERROR, "该门店不可关联");
        }
        return p.getId();
    }

    /**
     * 沿 merged_to 链找到当前"正主"。
     *
     * **所有按 id 取门店的地方都应该走这里**，否则会读到已被合并的旧记录，
     * 出现"点评挂在一家已经不存在的店上"。链深理论上可大于 1，所以用循环而非单次跳转；
     * 同时限深兜底，防止数据异常时死循环。
     */
    public Poi resolve(Long id) {
        if (id == null) {
            return null;
        }
        Poi p = poiMapper.selectById(id);
        int guard = 0;
        while (p != null && Poi.STATUS_MERGED.equals(p.getStatus())
                && p.getMergedTo() != null && ++guard < 10) {
            p = poiMapper.selectById(p.getMergedTo());
        }
        return p;
    }

    /**
     * 维护冗余的 content_count。
     *
     * 为什么冗余：榜单要按热度排序，每次 COUNT(*) 太慢。
     * 代价是要在发布/删除/编辑/合并时同步 —— 所以只在这里改，别到处 UPDATE。
     */
    public void adjustContentCount(Long poiId, int delta) {
        Poi p = resolve(poiId);
        if (p == null) {
            return;
        }
        int next = Math.max(0, (p.getContentCount() == null ? 0 : p.getContentCount()) + delta);
        Poi upd = new Poi();
        upd.setId(p.getId());
        upd.setContentCount(next);
        poiMapper.updateById(upd);
    }

    /** 可见性：NORMAL 人人可见；PENDING 仅创建者本人；其余一律不可见（白名单式） */
    public boolean canSee(Poi p, Long me) {
        if (p == null) {
            return false;
        }
        if (Poi.STATUS_NORMAL.equals(p.getStatus())) {
            return true;
        }
        if (Poi.STATUS_PENDING.equals(p.getStatus())) {
            return me != null && me.equals(p.getCreatedBy());
        }
        return false;
    }

    // ---------- 存量迁移 ----------

    /**
     * 一次性迁移：把历史 `content.poi_name` 归并成门店记录并回填 `poi_id`。
     *
     * **为什么做成接口而不是纯 SQL 脚本**：城市码换算（RegionUtil）、同名去重、
     * 计数维护这些规则都在应用层。纯 SQL 迁移等于把同一套规则再抄一份到 SQL 里 ——
     * 规则一旦分散就必然漂移（本项目已经在可见性、地区码上各吃过一次）。
     * 走接口可以完全复用 {@link #create} 的逻辑。
     *
     * 幂等：只处理 `poi_id IS NULL 且 poi_name != ''` 的笔记，
     * 重复执行不会重复建店，也不会改动已关联的笔记。
     *
     * @return {scanned, linked, created} —— 扫描数 / 回填数 / 新建门店数
     */
    public Map<String, Object> migrateFromNames(List<Content> targets) {
        int linked = 0;
        int createdBefore = countAll();
        for (Content c : targets) {
            // create() 内部已经做了"同名同城复用"，所以这里不用自己去重
            PoiCreateReq req = new PoiCreateReq(
                    c.getPoiName().trim(),   // name
                    "",                      // address（历史数据没有）
                    null, null,              // lng / lat
                    "",                      // category
                    c.getRegionCode(),       // regionCode → 由 create 推出 cityCode
                    "", "");                 // phone / openHours
            Long poiId = create(0L, true, req);
            // 回填关联（不走 updateContent，避免触发重新审核等副作用）
            Content upd = new Content();
            upd.setId(c.getId());
            upd.setPoiId(poiId);
            // 用 UpdateWrapper 只改 poi_id 一个字段，别把整行覆盖回去
            contentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Content>()
                            .eq(Content::getId, c.getId())
                            .set(Content::getPoiId, poiId));
            linked++;
        }
        // 计数统一重算一次：迁移过程中 create() 不会去加 content_count
        recalcContentCounts();
        int createdAfter = countAll();
        Map<String, Object> r = new HashMap<>();
        r.put("scanned", targets.size());
        r.put("linked", linked);
        r.put("created", createdAfter - createdBefore);
        return r;
    }

    /** 待迁移的笔记：有店名、但还没有关联门店 */
    public List<Content> findMigratableContents() {
        return contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .isNull(Content::getPoiId)
                .ne(Content::getPoiName, "")
                .orderByAsc(Content::getId));
    }

    private int countAll() {
        return Math.toIntExact(poiMapper.selectCount(null));
    }

    /**
     * 按 content.poi_id 重算所有门店的 content_count。
     * 迁移后跑一次即可把冗余计数校正（平时由 ContentService 增量维护）。
     */
    public void recalcContentCounts() {
        List<Content> all = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .isNotNull(Content::getPoiId));
        Map<Long, Integer> cnt = new HashMap<>();
        for (Content c : all) {
            // 已被合并的门店要归到正主头上，否则正主少算、旧店多算
            Poi p = resolve(c.getPoiId());
            if (p != null) {
                cnt.merge(p.getId(), 1, Integer::sum);
            }
        }
        for (Poi p : poiMapper.selectList(null)) {
            Poi upd = new Poi();
            upd.setId(p.getId());
            upd.setContentCount(cnt.getOrDefault(p.getId(), 0));
            poiMapper.updateById(upd);
        }
    }

    // ---------- 管理端：门店审核 ----------

    /** 待审门店列表（不传 status 默认只列 PENDING） */
    public List<Map<String, Object>> adminList(String status) {
        String st = (status == null || status.isBlank()) ? Poi.STATUS_PENDING : status;
        return poiMapper.selectList(new LambdaQueryWrapper<Poi>()
                        .eq(Poi::getStatus, st)
                        .orderByAsc(Poi::getId))
                .stream().map(p -> toVo(p, null)).toList();
    }

    /**
     * 审核门店。通过 → NORMAL（对所有人可见）；驳回 → REJECTED。
     *
     * 注意：**已关联到笔记上的待审门店被驳回后，那些笔记的 poi_id 仍指向它**。
     * 这里不做级联处理，原因：
     *   · 笔记本身是另一条独立的内容，不该因为门店被驳回而消失
     *   · 真要清理应走"合并/迁移"而不是删引用，否则会静默丢数据
     * 后续做管理员合并入口时一并处理（记入改造方案批 3）。
     */
    public void audit(Long id, boolean pass, String reason) {
        Poi p = poiMapper.selectById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!Poi.STATUS_PENDING.equals(p.getStatus())) {
            throw new BizException(ResultCode.PARAM_ERROR, "该门店不在待审状态");
        }
        Poi upd = new Poi();
        upd.setId(id);
        upd.setStatus(pass ? Poi.STATUS_NORMAL : Poi.STATUS_REJECTED);
        poiMapper.updateById(upd);
        if (!pass) {
            log.info("门店审核驳回：id={}, reason={}", id, reason);
        }
    }

    /**
     * 批量取门店（按 id），返回 id → Poi（缺失的 id 不在结果里）。
     *
     * 榜单/聚合页要展示门店名，必须批量查 —— 在循环里 selectById 就是 N+1。
     * 若某个 id 指向的是**已被合并**的门店，会解析到正主
     * （所以两个不同的旧 id 可能返回同一条，调用方如果要按正主聚合需要注意）。
     */
    public Map<Long, Poi> findByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Poi> out = new HashMap<>();
        for (Poi p : poiMapper.selectBatchIds(ids)) {
            out.put(p.getId(), p);
        }
        // 只对 MERGED 的做二次解析：正常数据里这种很少，
        // 所以不必为它把整条链也批量预取（resolve 内部有环保护）
        for (Long id : ids) {
            Poi p = out.get(id);
            if (p != null && Poi.STATUS_MERGED.equals(p.getStatus())) {
                out.put(id, resolve(id));
            }
        }
        return out;
    }

    /** 转成前端用的扁平结构（与项目其它模块一致，不额外建 VO 类） */
    public Map<String, Object> toVo(Poi p, Long me) {        Map<String, Object> m = new HashMap<>();
        m.put("poiId", String.valueOf(p.getId()));
        m.put("name", p.getName());
        m.put("address", p.getAddress());
        m.put("category", p.getCategory());
        m.put("cityCode", p.getCityCode());
        m.put("phone", p.getPhone());
        m.put("openHours", p.getOpenHours());
        m.put("contentCount", p.getContentCount() == null ? 0 : p.getContentCount());
        m.put("score", p.getScore());
        m.put("status", p.getStatus());
        // 待审门店要让前端知道"这个还没过审"，好在选择器里标注出来
        m.put("pending", Poi.STATUS_PENDING.equals(p.getStatus()));
        return m;
    }
}
