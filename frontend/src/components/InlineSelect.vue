<template>
  <div ref="rootRef" class="inline-select" @keydown.esc="close">
    <button
      type="button"
      class="inline-select__trigger"
      :class="[{ 'is-open': open }, size === 'lg' ? 'inline-select__trigger--lg' : '']"
      :aria-expanded="open ? 'true' : 'false'"
      aria-haspopup="listbox"
      :aria-label="ariaLabel"
      @click="toggle"
      @keydown.down.prevent="openAndMove(1)"
      @keydown.up.prevent="openAndMove(-1)"
    >
      <span v-if="$slots.icon" class="inline-select__icon" aria-hidden="true">
        <slot name="icon" />
      </span>
      <span class="inline-select__label" :class="{ 'is-placeholder': currentLabel === placeholder }">
        {{ currentLabel }}
      </span>
      <svg class="inline-select__caret" viewBox="0 0 24 24" width="12" height="12" aria-hidden="true" focusable="false">
        <path
          fill="currentColor"
          d="M12 15.5 5.5 9l1.4-1.4L12 12.7l5.1-5.1L18.5 9z"
        />
      </svg>
    </button>

    <ul v-if="open" ref="panelRef" class="inline-select__panel" role="listbox" :aria-label="ariaLabel">
      <li v-for="(opt, index) in options" :key="String(opt.value)" role="option" :aria-selected="opt.value === modelValue ? 'true' : 'false'">
        <button
          type="button"
          class="inline-select__option"
          :class="{ 'is-selected': opt.value === modelValue, 'is-focused': index === focusedIndex }"
          @click="pick(opt)"
          @mouseenter="focusedIndex = index"
        >
          <span class="inline-select__option-label">{{ opt.label }}</span>
          <svg
            v-if="opt.value === modelValue"
            class="inline-select__tick"
            viewBox="0 0 24 24"
            width="13"
            height="13"
            aria-hidden="true"
            focusable="false"
          >
            <path fill="currentColor" d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z" />
          </svg>
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/**
 * 轻量下拉（v3.22.2）：替换 el-select。
 *
 * 为什么不用 el-select：它的可视文字是绝对定位层，宽度由一个隐藏 input 撑开，
 * 导致「文字 ↔ 箭头」之间必然留出一段无法用 CSS 收窄的空隙（实测 20px）。
 * 自绘后 trigger 是普通 inline-flex（文字 + 4px + 箭头），间距完全可控。
 *
 * 无障碍：trigger 为 button（aria-haspopup/expanded），面板 role=listbox，
 * 选项 role=option + aria-selected；支持 Enter/Space/↑/↓/Esc 与点击外部关闭。
 */
const props = defineProps({
  modelValue: { type: [String, Number, null], default: null },
  /** [{ value, label }] */
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '' },
  ariaLabel: { type: String, default: '' },
  /**
   * v4.15：尺寸档位。默认保持既有观感（28px 高 / 12.5px 字）；
   * lg 供助手页输入区使用，对齐豆包工具行的 32px 文字项 / 13px 字。
   * 用 prop 而不是父级 :deep() 覆盖，是因为 scoped 优先级不可控。
   */
  size: { type: String, default: 'sm', validator: (v) => ['sm', 'lg'].includes(v) }
})

const emit = defineEmits(['update:modelValue'])

const rootRef = ref(null)
const panelRef = ref(null)
const open = ref(false)
const focusedIndex = ref(-1)

const currentLabel = computed(() => {
  const hit = props.options.find((opt) => opt.value === props.modelValue)
  return hit ? hit.label : props.placeholder
})

function toggle() {
  open.value = !open.value
  if (open.value) {
    focusedIndex.value = props.options.findIndex((opt) => opt.value === props.modelValue)
    if (focusedIndex.value < 0) focusedIndex.value = 0
  }
}

function close() {
  open.value = false
}

function openAndMove(step) {
  if (!open.value) {
    open.value = true
    focusedIndex.value = props.options.findIndex((opt) => opt.value === props.modelValue)
    if (focusedIndex.value < 0) focusedIndex.value = 0
    return
  }
  const len = props.options.length
  if (!len) return
  focusedIndex.value = (focusedIndex.value + step + len) % len
}

function pick(opt) {
  emit('update:modelValue', opt.value)
  open.value = false
}

/** 面板内的 ↑/↓/Enter 交给列表处理 */
function onPanelKeydown(event) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    openAndMove(event.key === 'ArrowDown' ? 1 : -1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    const opt = props.options[focusedIndex.value]
    if (opt) pick(opt)
  }
}

function onDocumentClick(event) {
  if (!open.value) return
  if (rootRef.value && rootRef.value.contains(event.target)) return
  close()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onPanelKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onPanelKeydown)
})

// 外部值变化时同步焦点
watch(
  () => props.modelValue,
  () => {
    focusedIndex.value = props.options.findIndex((opt) => opt.value === props.modelValue)
  }
)
</script>

<style scoped>
.inline-select {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
}

/* trigger：做成真正看得出是「可选控件」的小胶囊（文字 + 4px + 箭头，宽度随内容） */
.inline-select__trigger {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  min-height: 28px;
  padding: 0 9px;
  color: var(--ap-text-2, #626977);
  background-color: #f4f5f8;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  font-size: 12.5px;
  /* 12–13px 文字统一 1.5 行高，避免继承 body 的 24px（1.5×16）导致胶囊被撑高 */
  line-height: 1.5;
  transition:
    color 0.15s ease,
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.inline-select__trigger:hover {
  color: var(--ap-text, #16181d);
  background-color: #eceef3;
}

.inline-select__trigger.is-open {
  color: var(--color-primary);
  background-color: #edf2ff;
  border-color: rgb(37 99 235 / 22%);
}

/* lg 档：与发送按钮同高（40px），三者齐平。
   v4.19 起文字色提到接近正文、圆角 12px，降低「表单控件感」；
   v4.25 随输入卡压缩高度，44 → 40px。 */
.inline-select__trigger--lg {
  min-height: 40px;
  padding: 0 12px;
  color: var(--ap-text, #16181d);
  font-size: 13.5px;
  background-color: transparent;
  border-radius: 10px;
}

.inline-select__trigger--lg:hover {
  color: var(--ap-text, #16181d);
  background-color: #f4f5f8;
}

.inline-select__trigger--lg .inline-select__label {
  max-width: 180px;
}

.inline-select__label {
  max-width: 160px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* v4.19：可选前置图标（豆包工具行的「图标 + 文字」形态）。
   只在传入 icon 插槽时渲染，不影响其他调用方的纯文字胶囊。 */
.inline-select__icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  color: #5c6370;
}

/* 图标与文字之间给到 7px（基础 gap 是 4px，对「图标+文字」组合偏紧） */
.inline-select__trigger--lg .inline-select__icon {
  margin-right: 3px;
}

.inline-select__label.is-placeholder {
  color: var(--ap-text-2, #626977);
}

.inline-select__caret {
  flex-shrink: 0;
  color: #9aa1ae;
  transition:
    transform 0.18s cubic-bezier(0.4, 0, 0.2, 1),
    color 0.15s ease;
}

.inline-select__trigger.is-open .inline-select__caret {
  color: currentcolor;
  transform: rotate(180deg);
}

/* 面板：与页面内「更多」菜单同一套视觉语言 */
.inline-select__panel {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  z-index: 40;
  min-width: 100%;
  max-height: 280px;
  padding: 4px;
  margin: 0;
  overflow-y: auto;
  list-style: none;
  background-color: var(--color-bg-card, #fff);
  border: 1px solid var(--ap-border-strong, #dcdfe5);
  border-radius: 12px;
  box-shadow: 0 16px 32px -16px rgb(16 24 40 / 28%);
}

.inline-select__option {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 32px;
  padding: 0 10px;
  color: var(--ap-text, #16181d);
  font-family: inherit;
  font-size: 13px;
  white-space: nowrap;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.inline-select__option.is-focused {
  background-color: #f3f4f7;
}

.inline-select__option.is-selected {
  color: var(--color-primary, #2563eb);
  font-weight: 500;
  background-color: rgb(37 99 235 / 8%);
}

.inline-select__option-label {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.inline-select__tick {
  flex-shrink: 0;
  color: var(--color-primary, #2563eb);
}
</style>
