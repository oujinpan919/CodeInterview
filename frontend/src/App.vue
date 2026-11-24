<template>
  <DefaultLayout>
    <RouterView v-slot="{ Component }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
  </DefaultLayout>
</template>

<script setup>
import { RouterView } from 'vue-router'
import DefaultLayout from '@/layout/DefaultLayout.vue'
</script>
