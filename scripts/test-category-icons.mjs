/**
 * 分类图标验收脚本（离线，不需要浏览器）。
 *
 * 校验：
 *  1. 库里 44 个启用分类是否全部命中预置图标（有没有掉进单字兜底）；
 *  2. 是否有两个分类用到同一个图形（视觉上无法区分）；
 *  3. 每个图标的 svg 片段是否结构合法、是否被归一化到 24×24 画布；
 *  4. 归一化后各图标的「可见尺寸」是否一致（防止有的撑满、有的缩成一团）。
 *
 * 用法：node scripts/test-category-icons.mjs
 */
import { pathToFileURL } from 'node:url'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { resolveCategoryIcon, CATEGORY_ICONS } from '../frontend/src/data/categoryIcons.js'

const here = path.dirname(fileURLToPath(import.meta.url))

/** 与生成脚本共用同一套精确包围盒实现，避免两处口径不一致 */
const bboxLibPath = path.join(here, '..', 'frontend', 'node_modules', 'svg-path-bbox', 'dist', 'index.js')
const { svgPathBbox } = await import(pathToFileURL(bboxLibPath).href).then(
  (m) => m.default ?? m
)

/** 库里 44 个启用分类（与 DB 顺序一致） */
const CATEGORIES = [
  'Java 基础面试题',
  'Java 集合面试题',
  'Java 并发面试题',
  'Java 虚拟机面试题',
  'MySQL 面试题',
  'Redis 面试题',
  'Spring 面试题',
  'SpringBoot 面试题',
  'SpringCloud 面试题',
  '消息队列面试题',
  'RabbitMQ 面试题',
  '计算机网络面试题',
  '操作系统面试题',
  '后端系统设计面试题',
  '后端场景面试题',
  'JavaScript 面试题',
  'Vue 基础面试题',
  '设计模式面试题',
  'MyBatis 面试题',
  'Netty 面试题',
  'MongoDB 面试题',
  'Git 基础面试题',
  '算法和数据结构面试题',
  '数据结构面试题',
  '最全 AI 应用开发面试题',
  'AI Agent 智能体面试题',
  'AI 超级智能体项目面试题',
  'Claude Code 面试题',
  'OpenClaw 面试题',
  'Go 基础面试题',
  'Go 面试题',
  'C++ 基础面试题',
  'C++ 面试题',
  'C# 面试题',
  'PHP 面试题',
  'Python 面试题',
  'React 基础面试题',
  '前端手写代码面试题',
  '前端代码分析面试题',
  'Nginx 面试题',
  'HR 面试题',
  '系统运维面试题',
  'IT 运维工程师面试题',
  'DevOps 运维面试题'
]

let failed = 0
const fail = (msg) => {
  console.error('✗ ' + msg)
  failed++
}

// 1. 全部图标结构合法
console.log('— 图标合法性 —')
for (const [key, def] of Object.entries(CATEGORY_ICONS)) {
  if (!def.color) fail(`${key} 缺少 color`)

  // 必须带归一化变换，否则不同来源的图标大小会不一致
  if (typeof def.transform !== 'string' || !/^translate\([-\d. ]+\) scale\([\d.]+\)$/.test(def.transform)) {
    fail(`${key} 缺少合法的 transform（无法归一化到 24×24）`)
  }

  if (!Array.isArray(def.paths) || def.paths.length === 0) {
    fail(`${key} 没有 paths`)
    continue
  }

  for (const [i, p] of def.paths.entries()) {
    if (typeof p.d !== 'string' || !p.d.trim()) {
      fail(`${key} 第 ${i} 个 path 没有 d 属性`)
      continue
    }
    // 路径语法白名单（防止把非路径内容混进来）
    if (!/^[MmZzLlHhVvCcSsQqTtAa0-9\s.,+-]+$/.test(p.d)) {
      fail(`${key} 第 ${i} 个 path 含非法字符`)
    }
    // 不能残留写死的填充色，否则会盖掉家族色
    if (p.fill && p.fill !== 'none') {
      fail(`${key} 第 ${i} 个 path 写死了 fill=${p.fill}，会覆盖家族色`)
    }
  }
}
console.log(`  ${Object.keys(CATEGORY_ICONS).length} 个图标通过结构与归一化校验`)

// 1.5 归一化后各图标的可见尺寸必须一致
// 这是 v4.9.1 修过的真实 bug：早先按 viewBox 缩放，源图标自身留白差异被保留下来，
// 实测可见占比从 63% 到 92% 不等，肉眼看就是「有的撑满、有的缩成一团」。
console.log('— 可见尺寸一致性 —')
{
  const ratios = []
  for (const [key, def] of Object.entries(CATEGORY_ICONS)) {
    // 解析 transform 里的 scale，乘回真实包围盒，得到归一化后的实际尺寸
    const m = def.transform.match(/scale\(([\d.]+)\)/)
    const scale = m ? Number(m[1]) : 1
    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity
    for (const p of def.paths) {
      // svgPathBbox 返回 [minX, minY, maxX, maxY]
      const [x1, y1, x2, y2] = svgPathBbox(p.d)
      if (!Number.isFinite(x1)) continue
      minX = Math.min(minX, x1)
      minY = Math.min(minY, y1)
      maxX = Math.max(maxX, x2)
      maxY = Math.max(maxY, y2)
    }
    if (!Number.isFinite(minX)) {
      fail(`${key} 无法计算包围盒`)
      continue
    }
    const visible = Math.max(maxX - minX, maxY - minY) * scale
    ratios.push({ key, visible })
  }

  // 全部应落在同一目标尺寸附近（允许 0.5 单位误差，抵消坐标取整）
  const values = ratios.map((r) => r.visible)
  const lo = Math.min(...values)
  const hi = Math.max(...values)
  if (hi - lo > 0.5) {
    const bad = ratios
      .filter((r) => r.visible < hi - 0.5)
      .map((r) => `${r.key}=${r.visible.toFixed(2)}`)
    fail(`图标可见尺寸不一致（${lo.toFixed(2)} ~ ${hi.toFixed(2)}），偏小的有：${bad.join(' / ')}`)
  } else {
    console.log(`  ${ratios.length} 个图标可见尺寸一致（${lo.toFixed(2)} ~ ${hi.toFixed(2)} / 24）`)
  }
}

// 2. 每个分类都要命中
console.log('— 分类命中 —')
const used = new Map()
const misses = []
for (const name of CATEGORIES) {
  const icon = resolveCategoryIcon(name)
  if (!icon) {
    misses.push(name)
    continue
  }
  const key = Object.keys(CATEGORY_ICONS).find((k) => CATEGORY_ICONS[k] === icon)
  if (!used.has(key)) used.set(key, [])
  used.get(key).push(name)
}
if (misses.length) fail(`未命中图标（会退化成方块）：${misses.join(' / ')}`)
console.log(`  ${CATEGORIES.length} 个分类命中 ${used.size} 个图标`)

// 3. 不允许两个分类共用同一图形（相邻卡片出现相同图标会像渲染错误）
console.log('— 图标去重 —')
const dup = [...used.entries()].filter(([, names]) => names.length > 1)
if (dup.length) {
  for (const [key, names] of dup) fail(`图标 ${key} 被多个分类共用：${names.join(' / ')}`)
} else {
  console.log('  无冲突：每个分类都有独立图形')
}

// 4. 未命中的兜底不能崩
console.log('— 兜底 —')
const unknown = resolveCategoryIcon('管理员新建的神秘分类')
if (unknown !== null) fail('未知分类应返回 null 以走首字兜底')
else console.log('  未知分类正确返回 null（组件退回首字方块）')

console.log(failed === 0 ? '\n✓ 全部通过' : `\n✗ ${failed} 项失败`)
process.exit(failed === 0 ? 0 : 1)
