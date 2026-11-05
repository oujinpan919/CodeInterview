<template>
  <div
    ref="container"
    class="base-chart"
    :style="{ height }"
    role="img"
    :aria-label="ariaLabel"
  />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 04 B6 / T11 要点 4：echarts/core + 显式 use，禁止 import * as echarts from 'echarts'
echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' },
  ariaLabel: { type: String, default: '统计图表' }
})

const container = ref(null)
let chart = null
let observer = null

onMounted(() => {
  chart = echarts.init(container.value)
  chart.setOption(props.option)
  // 响应式：ResizeObserver 自适应，卸载时 dispose（04 B3）
  observer = new ResizeObserver(() => {
    if (chart) chart.resize()
  })
  observer.observe(container.value)
})

watch(
  () => props.option,
  (option) => {
    if (chart) chart.setOption(option, true)
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.base-chart {
  width: 100%;
  min-width: 0;
}
</style>
