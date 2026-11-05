<template>
  <DefaultLayout>
    <RouterView />
  </DefaultLayout>
  <AssistantPanel />
</template>

<script setup>
import { RouterView } from 'vue-router'
import DefaultLayout from '@/layout/DefaultLayout.vue'
import AssistantPanel from '@/components/AssistantPanel.vue'
</script>
