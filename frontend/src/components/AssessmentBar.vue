<template>
  <div class="assessment-bar" role="group" aria-label="三档自评">
    <el-button
      v-for="option in OPTIONS"
      :key="option.value"
      class="assessment-bar__button"
      :class="{ 'assessment-bar__button--active': modelValue === option.value }"
      :type="modelValue === option.value ? option.type : 'default'"
      :loading="loading && modelValue === option.value"
      :disabled="disabled || loading"
      @click="handleClick(option.value)"
    >
      {{ option.label }}
    </el-button>
  </div>
</template>

<script setup>
const OPTIONS = [
  { value: 1, label: '掌握', type: 'success' },
  { value: 2, label: '模糊', type: 'warning' },
  { value: 3, label: '不会', type: 'danger' }
]

const props = defineProps({
  modelValue: { type: Number, default: null },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])

function handleClick(assess) {
  if (props.disabled || props.loading) return
  emit('update:modelValue', assess)
  emit('change', assess)
}
</script>

<style scoped>
.assessment-bar {
  display: flex;
  gap: var(--space-3);
}

.assessment-bar__button {
  flex: 1 1 0;
  min-height: 44px;
  margin-left: 0;
  padding: 0 var(--space-4);
  border-radius: var(--radius-button);
  font-size: var(--font-size-body);
}

.assessment-bar__button--active {
  font-weight: 600;
}

@media (max-width: 767px) {
  .assessment-bar {
    gap: var(--space-2);
  }

  .assessment-bar__button {
    padding: 0 var(--space-2);
  }
}
</style>
