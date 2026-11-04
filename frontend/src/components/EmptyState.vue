<template>
  <div class="empty-state">
    <svg
      class="empty-state__illustration"
      viewBox="0 0 120 120"
      role="img"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="60" cy="60" r="52" fill="var(--color-bg-page)" />
      <rect
        x="30"
        y="28"
        width="52"
        height="64"
        rx="6"
        fill="var(--color-bg-card)"
        stroke="var(--color-border)"
        stroke-width="2"
      />
      <line x1="40" y1="44" x2="70" y2="44" stroke="var(--color-border)" stroke-width="3" />
      <line x1="40" y1="56" x2="68" y2="56" stroke="var(--color-border)" stroke-width="3" />
      <line x1="40" y1="68" x2="60" y2="68" stroke="var(--color-border)" stroke-width="3" />
      <circle cx="82" cy="80" r="16" fill="var(--color-bg-card)" stroke="var(--color-primary)" stroke-width="3" />
      <line x1="93" y1="91" x2="102" y2="100" stroke="var(--color-primary)" stroke-width="4" stroke-linecap="round" />
    </svg>

    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <el-button
      v-if="actionText"
      class="empty-state__action"
      type="primary"
      @click="emit('action')"
    >
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionText: { type: String, default: '' }
})

const emit = defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-4);
  text-align: center;
}

.empty-state__illustration {
  width: 120px;
  height: 120px;
}

.empty-state__title {
  margin-top: var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.empty-state__description {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.empty-state__action {
  min-height: 44px;
  margin-top: var(--space-5);
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}
</style>
