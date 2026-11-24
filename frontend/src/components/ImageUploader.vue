<template>
  <span class="image-uploader">
    <!-- 触发器由调用方决定：头像用整个圆形头像区，Markdown 编辑器用「插入图片」按钮 -->
    <span
      class="image-uploader__trigger"
      role="button"
      tabindex="0"
      :aria-label="ariaLabel"
      :aria-busy="uploading ? 'true' : 'false'"
      @click="pick"
      @keydown.enter.prevent="pick"
      @keydown.space.prevent="pick"
    >
      <slot :uploading="uploading">
        <span class="image-uploader__default">{{ uploading ? '上传中…' : '上传图片' }}</span>
      </slot>
    </span>
    <input
      ref="inputRef"
      class="image-uploader__input"
      type="file"
      :accept="accept"
      tabindex="-1"
      aria-hidden="true"
      @change="handleChange"
    />
  </span>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'

/**
 * 通用图片上传（v4.12）。头像 / 题目配图 / 笔记配图共用这一个组件，
 * 差别只在 folder 与触发区外观（由默认插槽决定）。
 *
 * 校验分两层，前端这层只为「少一次白跑的请求 + 更快的提示」：
 * 真实防线在后端（按文件头魔数嗅探），因为浏览器给的 file.type 完全可伪造。
 */

const props = defineProps({
  /** 业务子目录：avatar / question / note */
  folder: { type: String, default: 'misc' },
  /** 单文件上限（字节），与后端 app.storage.max-bytes 对齐 */
  maxBytes: { type: Number, default: 5 * 1024 * 1024 },
  accept: { type: String, default: 'image/png,image/jpeg,image/webp,image/gif' },
  ariaLabel: { type: String, default: '上传图片' }
})

const emit = defineEmits(['uploaded', 'error'])

const inputRef = ref(null)
const uploading = ref(false)

function pick() {
  if (uploading.value) return
  inputRef.value?.click()
}

async function handleChange(event) {
  const file = event.target.files && event.target.files[0]
  // 必须清空，否则连续选同一张图不会再触发 change
  event.target.value = ''
  if (!file) return

  if (file.size > props.maxBytes) {
    const mb = Math.round(props.maxBytes / 1024 / 1024)
    ElMessage.warning(`图片不能超过 ${mb}MB`)
    return
  }

  uploading.value = true
  try {
    const result = await uploadImage(file, props.folder)
    emit('uploaded', result)
  } catch (error) {
    // request.js 已对未捕获错误统一提示；这里再兜一层，保证组件在父级 catch 时也有反馈
    if (!error || !error.__notified) {
      ElMessage.error((error && error.message) || '图片上传失败')
    }
    emit('error', error)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-uploader {
  display: inline-flex;
  min-width: 0;
}

.image-uploader__trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  cursor: pointer;
}

.image-uploader__trigger[aria-busy='true'] {
  cursor: progress;
}

.image-uploader__trigger:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: var(--radius-button);
}

/* 视觉隐藏但保留在 DOM 里：display:none 会让部分浏览器拒绝触发 click() */
.image-uploader__input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.image-uploader__default {
  color: var(--color-primary);
  font-size: var(--font-size-aux);
}
</style>
