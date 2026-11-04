<template>
  <div class="error-state" role="alert">
    <p class="error-state__message">{{ message }}</p>
    <el-button class="error-state__retry" @click="emit('retry')">重试</el-button>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '加载失败，请稍后重试' }
})

const emit = defineEmits(['retry'])
</script>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) var(--space-4);
  text-align: center;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.error-state__message {
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.error-state__retry {
  min-height: 44px;
  margin-top: var(--space-4);
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}
</style>
