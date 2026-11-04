<template>
  <el-config-provider :locale="zhCn">
    <div v-if="total > 0" class="page-bar">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        :layout="layout"
        :pager-count="isMobile ? 5 : 7"
        background
        @current-change="handleChange"
      />
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  total: { type: Number, required: true }
})

const emit = defineEmits(['change'])

const isMobile = useMediaQuery('(max-width: 767px)')

// 桌面按 04 B3；手机按 04 B7.2 只保留「上一页/下一页 + 页码」
const layout = computed(() => (isMobile.value ? 'prev, pager, next' : 'prev, pager, next, total'))

function handleChange(next) {
  if (next !== props.page) emit('change', next)
}
</script>

<style scoped>
.page-bar {
  display: flex;
  justify-content: center;
  margin-top: var(--space-6);
}

.page-bar :deep(.el-pagination) {
  flex-wrap: wrap;
  row-gap: var(--space-2);
}

@media (max-width: 767px) {
  .page-bar :deep(.el-pagination button),
  .page-bar :deep(.el-pager li) {
    min-width: 44px;
    min-height: 44px;
  }
}
</style>
