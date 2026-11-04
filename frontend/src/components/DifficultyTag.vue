<template>
  <el-tag :type="tagType" size="small" effect="plain" class="difficulty-tag">
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: Number,
    required: true,
    validator: (value) => [1, 2, 3].includes(value)
  }
})

// 04 B3：简单=绿、中等=橙、困难=红
const DIFFICULTY_MAP = {
  1: { label: '简单', type: 'success' },
  2: { label: '中等', type: 'warning' },
  3: { label: '困难', type: 'danger' }
}

const current = computed(() => DIFFICULTY_MAP[props.value] || DIFFICULTY_MAP[2])
const label = computed(() => current.value.label)
const tagType = computed(() => current.value.type)
</script>

<style scoped>
.difficulty-tag {
  border-radius: var(--radius-tag);
}
</style>
