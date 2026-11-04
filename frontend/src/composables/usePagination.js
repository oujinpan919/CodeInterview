import { computed } from 'vue'

/**
 * 分页辅助（T05）：page / pageSize / total 由调用方以 ref/computed 传入，
 * 这里只提供总页数、偏移量与页码边界收敛，不改调用方的数据流。
 */
export function usePagination({ page, pageSize, total }) {
  const totalPages = computed(() =>
    Math.max(1, Math.ceil((total.value || 0) / (pageSize.value || 10)))
  )

  const offset = computed(() => (Math.max(1, page.value) - 1) * (pageSize.value || 10))

  function clampPage(value) {
    if (value < 1) return 1
    return value > totalPages.value ? totalPages.value : value
  }

  return { totalPages, offset, clampPage }
}
