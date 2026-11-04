import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ imports: ['vue', 'vue-router', 'pinia'], resolvers: [ElementPlusResolver()], dts: false }),
    Components({ resolvers: [ElementPlusResolver()], dts: false })
  ],
  resolve: { alias: { '@': '/src' } },
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
    // 02 §4.2 给的是对象形式；Vite 8 底层为 Rolldown，manualChunks 只接受函数
    // （对象形式构建报 “TypeError: manualChunks is not a function”），
    // 这里用等价函数保持「echarts / shiki 单独分包」的原意。
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/echarts')) return 'echarts'
          if (id.includes('node_modules/shiki') || id.includes('node_modules/@shikijs')) {
            return 'shiki'
          }
          return undefined
        }
      }
    }
  }
})
