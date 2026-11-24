<template>
  <span
    class="category-icon"
    :style="{ '--category-icon-color': icon.color }"
    aria-hidden="true"
  >
    <svg
      v-if="icon.paths"
      class="category-icon__svg"
      viewBox="0 0 24 24"
      fill="currentColor"
      stroke="none"
    >
      <g :transform="icon.transform">
        <path v-for="(p, i) in icon.paths" :key="i" :d="p.d" :fill="p.fill" />
      </g>
    </svg>
    <span v-else class="category-icon__initial">{{ icon.initial }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { resolveCategoryIcon } from '@/data/categoryIcons'

/**
 * 分类图标（首页「按分类刷题」卡片、题库筛选等场景）。
 *
 * 分类来自后端接口，只有 id/name/description/questionCount，没有图标字段，
 * 所以按分类名匹配预置图案（图案定义见 @/data/categoryIcons，由脚本生成）：
 *   - 命中 → 单色实心图形 + 家族色；
 *   - 未命中（管理员新建的分类）→ 退回收首字方块，不会报错也不会空白。
 *
 * v4.9：改用官方品牌 logo（Simple Icons / Devicon）+ Phosphor 概念图标，
 * 统一单色实心风格。v4.8 的手绘描边图标在放大后粗细不匀、大小不一，已废弃。
 */
const props = defineProps({
  name: { type: String, default: '' }
})

const icon = computed(() => {
  const resolved = resolveCategoryIcon(props.name)
  if (resolved) return resolved

  const name = String(props.name || '').trim()
  return {
    color: 'var(--color-primary)',
    initial: name ? name.slice(0, 1).toUpperCase() : '·'
  }
})
</script>

<style scoped>
.category-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--category-icon-color, var(--color-primary));
  /* 12% 同色淡底：让图标有「一块」的体积感，又不抢标题 */
  background-color: color-mix(in srgb, var(--category-icon-color, var(--color-primary)) 12%, transparent);
  border-radius: 12px;
}

.category-icon__svg {
  /*
   * 图形本身在 24 画布内已归一化到 90%（见 scripts/_build-category-icons.mjs），
   * 所以 36px 的 svg 实际可见图形约 32.4px，占 40px 外框的 ~81%，视觉上「沾满」。
   * 外框尺寸、圆角、底色浓度都不动，只放大里面的图案。
   *
   * 36px 是实测的安全上限：45 个图标里最接近圆角的（scenario / mongodb / php /
   * java / gobasic / hr）四角仍留 7.80px 余量，无一被 12px 圆角切到；
   * 再往上（38px+）开始贴边，故不再加。
   */
  width: 36px;
  height: 36px;
}

.category-icon__initial {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-body);
  font-weight: 600;
}
</style>
