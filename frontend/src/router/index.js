import { createRouter, createWebHistory } from 'vue-router'
import { h } from 'vue'

// T01 临时最小路由：仅保证 main.js 的 createApp + router 可用。
// T04 会替换为 04 B1 的 14 条完整路由表与全局守卫，本文件届时整体重写。
const HomePlaceholder = {
  name: 'HomePlaceholder',
  render: () => h('div')
}

export default createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', name: 'home', component: HomePlaceholder, meta: { title: '首页' } }]
})
