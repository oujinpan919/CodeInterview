<template>
  <!-- v3.22 应用态骨架（对齐豆包）：侧栏 + 主区（顶栏 / 滚动区 / 输入卡） -->
  <div class="ap">
    <!-- ============ 左侧会话侧边栏 ============ -->
    <aside
      v-if="showSidebar"
      class="ap-sidebar"
      :class="{ 'ap-sidebar--overlay': !isDesktop }"
      aria-label="会话列表"
    >
      <div class="ap-sidebar__head">
        <RouterLink
          to="/home"
          class="ap-sidebar__brand"
          title="返回码上记首页"
          aria-label="返回码上记首页"
        >
          <span class="ap-mark ap-mark--sm" aria-hidden="true">✦</span>
          <span class="ap-sidebar__brand-text">
            <span class="ap-sidebar__brand-name">AI 助手</span>
            <span class="ap-sidebar__brand-sub">码上记 · 面试题库</span>
          </span>
        </RouterLink>
      </div>

      <div class="ap-sidebar__actions">
        <button type="button" class="ap-new-chat" :disabled="store.streaming" @click="handleNew">
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
            <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
          </svg>
          <span>新建对话</span>
        </button>
      </div>

      <div class="ap-sidebar__scroll">
        <div class="ap-sidebar__label-row">
          <p class="ap-sidebar__label">最近</p>
          <span v-if="store.conversations.length" class="ap-sidebar__count">
            {{ store.conversations.length }}
          </span>
        </div>
        <ul class="ap-conv-list">
          <li v-for="conv in store.conversations" :key="conv.id" class="ap-conv-item">
            <button
              type="button"
              class="ap-conv"
              :class="{ 'is-active': conv.id === store.conversationId }"
              :aria-current="conv.id === store.conversationId ? 'true' : undefined"
              :title="conv.title"
              @click="handleOpen(conv.id)"
            >
              <span class="ap-conv__title">{{ conv.title }}</span>
              <span class="ap-conv__meta">{{ formatTime(conv.updatedAt) }}</span>
            </button>

            <button
              type="button"
              class="ap-conv__more"
              :class="{ 'is-open': menuId === conv.id }"
              :aria-label="`更多操作：${conv.title}`"
              :aria-expanded="menuId === conv.id ? 'true' : 'false'"
              @click.stop="toggleMenu(conv.id)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
                <circle cx="6" cy="12" r="1.6" fill="currentColor" />
                <circle cx="12" cy="12" r="1.6" fill="currentColor" />
                <circle cx="18" cy="12" r="1.6" fill="currentColor" />
              </svg>
            </button>

            <div v-if="menuId === conv.id" class="ap-menu" role="menu">
              <button type="button" class="ap-menu__item" role="menuitem" @click.stop="handleRename(conv)">
                重命名
              </button>
              <button
                type="button"
                class="ap-menu__item ap-menu__item--danger"
                role="menuitem"
                @click.stop="handleDelete(conv)"
              >
                删除
              </button>
            </div>
          </li>
        </ul>
        <p v-if="store.conversations.length === 0" class="ap-sidebar__empty">还没有历史对话</p>
      </div>

      <!-- 左栏 · 底部模型配置入口 -->
      <div class="ap-sidebar__foot">
        <RouterLink to="/assistant/settings" class="ap-sidebar__link" aria-label="打开模型配置">
          <span class="ap-sidebar__link-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
              <path
                fill="currentColor"
                d="M12 15.5A3.5 3.5 0 0 1 8.5 12 3.5 3.5 0 0 1 12 8.5a3.5 3.5 0 0 1 3.5 3.5 3.5 3.5 0 0 1-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97 0-.33-.03-.66-.07-.99l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.37-2.65c-.04-.24-.25-.41-.5-.41h-4c-.25 0-.46.17-.5.41l-.37 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.12.22-.07.49.12.64l2.11 1.65c-.04.33-.07.66-.07.99 0 .33.03.65.07.97l-2.11 1.66c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.31.61.22l2.49-1c.52.39 1.06.73 1.69.98l.37 2.65c.04.24.25.41.5.41h4c.25 0 .46-.17.5-.41l.37-2.65c.63-.25 1.17-.59 1.69-.98l2.49 1c.22.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64z"
              />
            </svg>
          </span>
          <span class="ap-sidebar__link-text">
            <span class="ap-sidebar__link-title">模型配置</span>
            <span class="ap-sidebar__link-sub">模型 · 额度 · 偏好</span>
          </span>
          <svg
            class="ap-sidebar__link-arrow"
            viewBox="0 0 24 24"
            width="14"
            height="14"
            aria-hidden="true"
            focusable="false"
          >
            <path fill="currentColor" d="M9 6l6 6-6 6-1.4-1.4 4.6-4.6-4.6-4.6z" />
          </svg>
        </RouterLink>
      </div>
    </aside>

    <div v-if="showMask" class="ap-mask" @click="sidebarOpen = false"></div>

    <!-- ============ 主对话区 ============ -->
    <section class="ap-main">
      <!-- 顶栏：只留折叠按钮 + 标题（模型/模板已移入输入卡，对齐豆包） -->
      <header class="ap-topbar">
        <button
          type="button"
          class="ap-iconbtn"
          :aria-label="isDesktop ? (collapsed ? '展开侧栏' : '收起侧栏') : '打开会话列表'"
          :title="isDesktop ? (collapsed ? '展开侧栏' : '收起侧栏') : '打开会话列表'"
          @click="toggleSidebar"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false">
            <path
              fill="currentColor"
              d="M3 5h18v2H3V5m0 6h18v2H3v-2m0 6h18v2H3v-2z"
            />
          </svg>
        </button>
        <h1 class="ap-title">
          <span class="ap-title__mark" aria-hidden="true"></span>
          <span class="ap-title__text">{{ pageTitle }}</span>
        </h1>
        <span v-if="store.streaming" class="ap-title-badge" title="回答生成中">
          <i></i>生成中
        </span>
      </header>

      <!-- 带入题目提示 -->
      <div v-if="store.question" class="ap-context">
        <span class="ap-context__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true" focusable="false">
            <path
              fill="currentColor"
              d="M6 2h8l6 6v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm7 1.6V9h5.4L13 3.6Z"
            />
          </svg>
        </span>
        <span class="ap-context__text">已带入：{{ store.question.title || '当前题目' }}</span>
        <RouterLink
          v-if="store.question.id"
          class="ap-context__link"
          :to="`/question/${store.question.id}`"
        >
          看原题
        </RouterLink>
        <button type="button" class="ap-context__close" aria-label="取消带入题目" @click="store.clearQuestion()">
          ×
        </button>
      </div>

      <!-- 无可用模型引导 -->
      <div v-if="!store.hasAnyUsableConfig" class="ap-blocked">
        <p>还没有可用的模型。</p>
        <RouterLink v-if="store.allowUserOwnKey" to="/assistant/settings" class="ap-blocked__cta">
          去配置模型 →
        </RouterLink>
        <p v-else class="ap-blocked__hint">管理员已关闭用户自带凭证，请联系管理员配置站点模型。</p>
      </div>

      <!-- 中栏 · 唯一可滚动区域：状态A（薄弱练习面板）/ 状态B（消息流） -->
      <div ref="listRef" class="ap-scroll" @scroll="onScroll">
        <div class="ap-inner">
          <!-- 空态：v4.13 改为「带真实数据」的陪练入口。
               原实现是一句问候 + 三条写死的问题，和任何聊天机器人无区别，
               而本页其实连着题库、错题本和掌握度统计 —— 把这些摊开才说明「为什么用它」。 -->
          <div
            v-if="store.messages.length === 0"
            class="ap-empty"
            :class="{ 'ap-empty--lean': (drillLoaded && !hasDrillData) || Boolean(store.questionId) }"
          >
            <div class="ap-empty__head">
              <span class="ap-mark ap-mark--lg" aria-hidden="true">✦</span>
              <span class="ap-empty__eyebrow">
                {{
                  store.questionId
                    ? '当前题目 · 深度追问'
                    : drillLoaded && hasDrillData
                      ? 'AI 面试陪练 · 错题驱动'
                      : 'AI 面试陪练 · 随时开问'
                }}
              </span>
              <h2 class="ap-empty__title">
                {{
                  store.questionId
                    ? '这题想从哪问起？'
                    : drillLoaded && hasDrillData
                      ? '今天从你的薄弱点开始练'
                      : '有什么我能帮你的吗？'
                }}
              </h2>
              <p class="ap-empty__sub">
                {{
                  store.questionId
                    ? '已带入当前题目，可以直接问它的考点、边界情况或与相似题的区别。'
                    : drillLoaded && hasDrillData
                      ? '我看了你的错题本和自评记录，下面这些还没真正掌握。可以直接开练，也可以问任何面试题。'
                      : '问我面试题的考点、区别与实现细节；也可以从题目详情页点「问助手」带入题干。'
                }}
              </p>
            </div>

            <!-- 建议问法：默认给通用问题；带入某题后切换为针对该题的追问 -->
            <div
              class="ap-recs ap-panel"
              :class="{
                'ap-recs--compact': hasDrillData && !store.questionId,
                'ap-recs--question': Boolean(store.questionId),
                'ap-recs--solo': drillLoaded && !hasDrillData && !store.questionId
              }"
            >
              <div class="ap-panel__head">
                <div class="ap-panel__title">
                  <span class="ap-panel__icon ap-panel__icon--ask" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="15" height="15" aria-hidden="true" focusable="false">
                      <path
                        fill="currentColor"
                        d="M12 2.2 14 9.9l7.8 2.1-7.8 2.1L12 21.8l-2-7.7-7.8-2.1 7.8-2.1z"
                      />
                    </svg>
                  </span>
                  <span class="ap-panel__title-text">
                    {{ store.questionId ? '就这道题，可以这样问' : '试试这样问' }}
                  </span>
                </div>
                <span class="ap-panel__hint">点击填入</span>
              </div>
              <div class="ap-recs__grid">
                <button
                  v-for="example in store.questionId ? QUESTION_PROMPTS : EXAMPLES"
                  :key="example"
                  type="button"
                  class="ap-rec"
                  @click="fillExample(example)"
                >
                  <span class="ap-rec__text">{{ example }}</span>
                  <svg
                    class="ap-rec__arrow"
                    viewBox="0 0 24 24"
                    width="14"
                    height="14"
                    aria-hidden="true"
                    focusable="false"
                  >
                    <path fill="currentColor" d="M9 6l6 6-6 6-1.4-1.4 4.6-4.6-4.6-4.6z" />
                  </svg>
                </button>
              </div>
              <p v-if="!store.questionId" class="ap-recs__tip">也可以直接在下方输入框自由提问</p>
            </div>

            <!-- 错题复习：真实待复习题目（点击即带入题干开练）。
                 已带入某题时不再显示 —— 用户注意力已在一道具体题目上，
                 再推「别的错题」只会分散。 -->
            <div v-if="drillItems.length && !store.questionId" class="ap-drill ap-panel">
              <div class="ap-panel__head">
                <div class="ap-panel__title">
                  <span class="ap-panel__icon ap-panel__icon--drill" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" focusable="false">
                      <path
                        d="M20 11.5A8 8 0 1 1 16.6 5M20 5v5h-5"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </span>
                  <span class="ap-panel__title-text">待复习</span>
                  <span class="ap-panel__hint">来自你的错题本</span>
                </div>
                <span class="ap-panel__badge">
                  <i aria-hidden="true"></i>{{ drillSummary.learning }} 题在学
                </span>
              </div>
              <div class="ap-drill__grid">
                <button
                  v-for="item in drillItems"
                  :key="item.questionId"
                  type="button"
                  class="ap-drill__item"
                  @click="startDrill(item)"
                >
                  <span class="ap-drill__top">
                    <span class="ap-drill__cat">{{ item.categoryName }}</span>
                  </span>
                  <span class="ap-drill__title">{{ item.title }}</span>
                  <span class="ap-drill__foot">
                    <span class="ap-drill__assess">
                      <span class="ap-drill__assess-label">自评</span>
                      <span class="ap-drill__dots" :title="`上次自评：${item.lastAssessLabel}`">
                        <i
                          v-for="n in 3"
                          :key="n"
                          :class="{ 'is-on': n <= item.lastAssess }"
                          aria-hidden="true"
                        ></i>
                      </span>
                    </span>
                    <span class="ap-drill__go">
                      带入题目
                      <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true" focusable="false">
                        <path fill="currentColor" d="M9 6l6 6-6 6-1.4-1.4 4.6-4.6-4.6-4.6z" />
                      </svg>
                    </span>
                  </span>
                </button>
              </div>
            </div>

            <!-- 薄弱分类：按掌握率升序，给出「哪里最虚」 -->
            <div v-if="weakCategories.length && !store.questionId" class="ap-weak ap-panel">
              <div class="ap-panel__head">
                <div class="ap-panel__title">
                  <span class="ap-panel__icon ap-panel__icon--weak" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" focusable="false">
                      <path
                        d="M4 19V9M10 19V5M16 19v-7M22 19H2"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                      />
                    </svg>
                  </span>
                  <span class="ap-panel__title-text">薄弱分类</span>
                </div>
                <span class="ap-panel__hint">按掌握率排序</span>
              </div>
              <ul class="ap-weak__list">
                <li v-for="cat in weakCategories" :key="cat.categoryId" class="ap-weak__item">
                  <div class="ap-weak__row">
                    <span class="ap-weak__name">{{ cat.name }}</span>
                    <span class="ap-weak__rate">{{ Math.round(cat.masteredRate * 100) }}%</span>
                  </div>
                  <div class="ap-weak__track">
                    <span class="ap-weak__bar" aria-hidden="true">
                      <i :style="{ width: `${Math.round(cat.masteredRate * 100)}%` }"></i>
                    </span>
                    <span class="ap-weak__count">{{ cat.count }} 题</span>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- 消息列表 -->
          <div
            v-for="message in store.messages"
            :key="message.id"
            class="ap-msg"
            :class="`ap-msg--${message.role}`"
          >
            <template v-if="message.role === 'user'">
              <div class="ap-user">{{ message.content }}</div>
            </template>
            <template v-else>
              <div class="ap-assistant">
                <div class="ap-assistant__label">
                  <span class="ap-mark ap-mark--xs" aria-hidden="true">✦</span>
                  <span>AI 助手</span>
                </div>
                <div class="ap-assistant__body">
                  <MarkdownView
                    v-if="message.status === 'done' || message.status === 'failed' || message.status === 'aborted'"
                    :content="message.content"
                    empty-text="（空回答）"
                  />
                  <template v-else>
                    <span v-if="!message.displayContent" class="ap-thinking">
                      <span class="ap-dots" aria-hidden="true"><i></i><i></i><i></i></span>
                      <span>正在思考…</span>
                    </span>
                    <span v-else class="ap-stream">{{ message.displayContent }}</span>
                    <span
                      v-if="message.status === 'connecting' || message.status === 'streaming'"
                      class="ap-cursor"
                      aria-hidden="true"
                    ></span>
                  </template>
                </div>
                <div v-if="message.statusNote" class="ap-msg__foot">
                  <span class="ap-msg__state">{{ message.statusNote }}</span>
                  <button
                    v-if="message.status === 'failed' || message.status === 'aborted'"
                    type="button"
                    class="ap-retry"
                    @click="store.regenerate()"
                  >
                    <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true" focusable="false">
                      <path
                        fill="currentColor"
                        d="M17.65 6.35A7.96 7.96 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35Z"
                      />
                    </svg>
                    重新生成
                  </button>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 只在「有消息 + 已经往上翻」时出现；空态不该出现这个浮起按钮 -->
        <button
          v-if="!stickToBottom && store.messages.length > 0"
          type="button"
          class="ap-to-bottom"
          aria-label="回到底部"
          title="回到底部"
          @click="scrollToBottomAndFollow"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
            <path fill="currentColor" d="M12 16.5 5.5 10l1.4-1.4 5.1 5.1 5.1-5.1L18.5 10z" />
          </svg>
          <span class="ap-to-bottom__text">回到底部</span>
        </button>
      </div>

      <!-- 输入区 -->
      <div class="ap-composer-wrap">
        <p v-if="store.notice" class="ap-notice" role="status">{{ store.notice }}</p>

        <div class="ap-composer" :class="{ 'is-disabled': store.quotaExhausted }">
          <textarea
            ref="inputRef"
            v-model="draft"
            class="ap-textarea"
            :placeholder="inputPlaceholder"
            maxlength="2000"
            rows="1"
            :disabled="store.quotaExhausted || !store.hasAnyUsableConfig"
            aria-label="消息输入框"
            @keydown.enter.exact.prevent="handleSend"
            @input="resizeInput"
          ></textarea>

          <div class="ap-composer__bar">
            <div class="ap-composer__tools">
              <InlineSelect
                v-if="modelOptions.length"
                :model-value="store.activeModelConfigId"
                :options="modelOptions"
                placeholder="选择模型"
                aria-label="选择模型"
                size="lg"
                @update:model-value="handleModelChange"
              >
                <!-- v4.19：图标 + 文字，对齐豆包工具行的技能项形态 -->
                <template #icon>
                  <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
                    <path fill="currentColor" d="M12 2.2 14 9.9l7.8 2.1-7.8 2.1L12 21.8l-2-7.7-7.8-2.1 7.8-2.1z" />
                  </svg>
                </template>
              </InlineSelect>

              <InlineSelect
                v-if="templateOptions.length"
                :model-value="store.templateId"
                :options="templateOptions"
                placeholder="讲解风格"
                aria-label="选择讲解风格"
                size="lg"
                @update:model-value="store.selectTemplate"
              >
                <template #icon>
                  <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
                    <path
                      fill="currentColor"
                      d="M5 4.5h11a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-2v3.2L10.6 15.5H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Zm2 3v1.5h7V7.5H7Zm0 3v1.5h4.5v-1.5H7Z"
                    />
                  </svg>
                </template>
              </InlineSelect>
            </div>

            <span
              class="ap-quota"
              :class="{ 'is-empty': store.quotaExhausted }"
              :title="quotaTitle"
            >
              <i aria-hidden="true"></i>{{ quotaText }}
            </span>

            <button
              v-if="store.streaming"
              type="button"
              class="ap-send ap-send--stop"
              aria-label="停止生成"
              title="停止生成"
              @click="store.stop()"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true" focusable="false">
                <rect x="6" y="6" width="12" height="12" rx="2.5" fill="currentColor" />
              </svg>
            </button>
            <button
              v-else
              type="button"
              class="ap-send"
              :disabled="sendDisabled"
              aria-label="发送"
              title="发送（Enter）"
              @click="handleSend"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
                <path fill="currentColor" d="M12 4.5 5.9 10.6l1.4 1.4 3.7-3.7V20h2V8.3l3.7 3.7 1.4-1.4z" />
              </svg>
            </button>
          </div>
        </div>

        <p class="ap-footnote">
          <span class="ap-footnote__hint">
            <kbd class="ap-kbd">Enter</kbd> 发送
            <span class="ap-footnote__sep" aria-hidden="true">·</span>
            <kbd class="ap-kbd">Shift</kbd><span class="ap-kbd__plus" aria-hidden="true">+</span><kbd class="ap-kbd">Enter</kbd>
            换行
          </span>
          <span class="ap-footnote__meta">
            <span class="ap-footnote__count">{{ draft.length }}/2000</span>
            <span aria-hidden="true">·</span>
            <span>AI 生成，仅供参考</span>
          </span>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import { useMediaQuery } from '@vueuse/core'
import { useAssistantStore } from '@/stores/assistant'
import { getStatsCategory } from '@/api/stats'
import { getWrongBook, getWrongBookSummary } from '@/api/record'
import MarkdownView from '@/components/MarkdownView.vue'
import InlineSelect from '@/components/InlineSelect.vue'

const EXAMPLES = [
  'HashMap 的扩容机制是什么？',
  'MySQL 为什么用 B+ 树做索引？',
  'TCP 三次握手为什么不能是两次？'
]

/**
 * v4.13：带入题目后的快捷提问。
 *
 * 原实现带入题目后仍然推荐「HashMap 扩容」这类与当前题无关的问题，
 * 既没用也占地方。这里换成围绕当前题的四个高频问法，
 * 用户点一下就能问，不用自己组织措辞。
 */
const QUESTION_PROMPTS = [
  '这题的核心考点是什么？',
  '面试官通常会怎么追问？',
  '有哪些容易答漏的边界情况？',
  '和相似题的关键区别在哪？'
]

/**
 * v4.13：空态的真实数据源。
 *
 * 助手页原来只有三条写死的问题，看起来和任何通用聊天机器人一样。
 * 但这个页面背后有题库、错题本和自评统计 —— 把「你的薄弱点」摆到首屏，
 * 才是它区别于通用聊天的地方，也直接给出「现在该练什么」。
 *
 * 失败一律静默降级为空数组：这是首屏装饰性数据，
 * 拿不到就退回原来的纯推荐位，不弹错误、不挡输入。
 */
const drillItems = ref([])
const drillSummary = ref({ learning: 0, reviewing: 0, mastered: 0, total: 0 })
const weakCategories = ref([])
/** 首屏数据是否已落地：未落地前不切标题，避免「通用问候 → 陪练文案」闪一下 */
const drillLoaded = ref(false)

const hasDrillData = computed(() => drillItems.value.length > 0 || weakCategories.value.length > 0)

/** 自评档位 → 文案（1/2/3 对应「不会/模糊/会」，与练习页一致） */
const ASSESS_LABELS = { 1: '不会', 2: '模糊', 3: '会' }

async function loadDrillData() {
  const [summary, list, cats] = await Promise.allSettled([
    getWrongBookSummary(),
    getWrongBook({ tab: 'learning', page: 1, size: 6 }),
    getStatsCategory()
  ])

  if (summary.status === 'fulfilled' && summary.value) {
    drillSummary.value = summary.value
  }

  if (list.status === 'fulfilled') {
    const rows = list.value?.list || []
    // 只取 4 条：2 列 × 2 行。6 条要占 3 行，会把「薄弱分类」和推荐位全挤到折叠线以下
    drillItems.value = rows.slice(0, 4).map((row) => ({
      ...row,
      lastAssessLabel: ASSESS_LABELS[row.lastAssess] || '未自评'
    }))
  }

  if (cats.status === 'fulfilled') {
    // 只留「练过但没掌握透」的分类：有记录、掌握率 <100%，按掌握率升序（最虚的在前）
    weakCategories.value = (cats.value || [])
      .filter((cat) => Number(cat.count) > 0 && Number(cat.masteredRate) < 1)
      .sort((a, b) => Number(a.masteredRate) - Number(b.masteredRate))
      .slice(0, 4)
  }

  drillLoaded.value = true
}

/** 点错题卡片：带入题干并聚焦输入框，让用户直接问「这题我怎么答」 */
async function startDrill(item) {
  store.setQuestion(item.questionId, item.title)
  await nextTick()
  // 带入后回到顶部：否则上一屏的滚动位置会让标题和带入提示条落在视口外
  const el = listRef.value
  if (el) el.scrollTop = 0
  focusInput()
}

/** 侧栏折叠状态的持久化键（S1：刷新不丢） */
const COLLAPSE_KEY = 'ci_assistant_sidebar_collapsed'

const route = useRoute()
const router = useRouter()
const store = useAssistantStore()

const draft = ref('')
const listRef = ref(null)
const inputRef = ref(null)
const stickToBottom = ref(true)
/** 平板/移动端的覆盖式侧栏开关；桌面端用 collapsed 控制 */
const sidebarOpen = ref(false)

/** R1 三档：≥1024 桌面内联 / 768–1023 平板 / <768 移动 */
const isDesktop = useMediaQuery('(min-width: 1024px)')
const isMobile = useMediaQuery('(max-width: 767px)')

function readCollapsed() {
  try {
    return localStorage.getItem(COLLAPSE_KEY) === '1'
  } catch {
    return false
  }
}

const collapsed = ref(readCollapsed())

watch(collapsed, (value) => {
  try {
    localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
  } catch {
    // 隐私模式下写入失败不阻塞使用
  }
})

const showSidebar = computed(() => (isDesktop.value ? !collapsed.value : sidebarOpen.value))
const showMask = computed(() => !isDesktop.value && sidebarOpen.value)

/** 桌面端切换折叠；平板/移动端切换覆盖层 */
function toggleSidebar() {
  if (isDesktop.value) {
    collapsed.value = !collapsed.value
  } else {
    sidebarOpen.value = !sidebarOpen.value
  }
}

// 跨断点时收敛状态：回到桌面关掉覆盖层；离开桌面关掉覆盖层避免遮罩残留
watch(isDesktop, (desktop) => {
  if (desktop) sidebarOpen.value = false
})

/** 顶栏标题：优先当前会话标题，否则新对话 */
const pageTitle = computed(() => {
  const current = store.conversations.find((conv) => conv.id === store.conversationId)
  return current ? current.title : store.conversationId ? '对话' : '新对话'
})

const quotaText = computed(() => {
  if (store.unlimited) return '管理员 · 不限次'
  const quota = store.quota
  if (!quota || typeof quota.remaining !== 'number') return '额度加载中…'
  return isMobile.value ? `${quota.remaining}/${quota.dailyLimit}` : `今日剩余 ${quota.remaining}/${quota.dailyLimit}`
})

/** 额度胶囊的补充说明（悬停可读；自带 Key 时说明不占站点额度） */
const quotaTitle = computed(() => {
  const quota = store.quota
  if (!quota) return ''
  if (store.unlimited) return '管理员不受每日次数限制（并发与间隔仍生效）'
  const parts = [`每日 ${quota.dailyLimit} 次`]
  if (quota.remaining != null) parts.push(`剩余 ${quota.remaining} 次`)
  if (quota.consumesSiteQuota === false) parts.push('使用自带凭证，不占用站点额度')
  return parts.join(' · ')
})

const inputPlaceholder = computed(() => {
  if (!store.hasAnyUsableConfig) return '请先配置模型'
  if (store.quotaExhausted) return '今日额度已用完'
  // v4.0：快捷键提示移到输入卡下方的 footnote，输入框内只留一句引导语
  return '发消息，问我任何面试题…'
})

const sendDisabled = computed(
  () => draft.value.trim().length === 0 || store.streaming || store.quotaExhausted || !store.hasAnyUsableConfig
)

/** 模型下拉选项（D13：只列已启用配置） */
const modelOptions = computed(() =>
  store.models.map((model) => ({ value: model.id, label: model.displayName }))
)

/** 讲解风格选项（D11）：只列数据库模板。
    默认风格为「默认讲解」(id=1)，由 store.loadMeta 在首次加载时选中，
    这样文案改在数据库/管理端即可生效，而不是锁死在 jar 内的 prompt/system.txt。 */
const templateOptions = computed(() =>
  store.templates.map((tpl) => ({ value: tpl.id, label: tpl.name }))
)

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  if (sameDay) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

/**
 * v4.25：上限 180px 与 .ap-textarea 的 max-height 同步。
 * 超过后由 textarea 自身滚动，容器高度随之定格。
 */
function resizeInput() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 180)}px`
}

function focusInput() {
  const el = inputRef.value
  if (!el) return
  // preventScroll：聚焦时浏览器会把输入框滚进视口，连带把空态顶部的 ✦ 与标题推出可见区
  el.focus({ preventScroll: true })
  resizeInput()
}

function fillExample(text) {
  draft.value = text
  nextTick(focusInput)
}

async function handleSend() {
  if (sendDisabled.value) return
  const text = draft.value
  draft.value = ''
  stickToBottom.value = true
  const sent = await store.send(text)
  if (!sent) draft.value = text
  nextTick(() => {
    resizeInput()
    focusInput()
  })
}

async function handleNew() {
  if (store.streaming) return
  closeMenu()
  store.startNewConversation()
  sidebarOpen.value = false
  nextTick(focusInput)
}

async function handleOpen(id) {
  if (store.streaming) {
    ElMessageBox.alert('当前回答还在生成中，请先停止或等待完成', '请稍候', { type: 'info' })
    return
  }
  closeMenu()
  const ok = await store.openConversation(id)
  if (ok) {
    sidebarOpen.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function handleDelete(conv) {
  menuId.value = null
  try {
    await ElMessageBox.confirm(`确定删除会话「${conv.title}」？`, '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await store.removeConversation(conv.id)
}

/** 会话「更多」菜单：同一时间只开一个 */
const menuId = ref(null)

function toggleMenu(id) {
  menuId.value = menuId.value === id ? null : id
}

function closeMenu() {
  menuId.value = null
}

/** 重命名（store.rename 早已实现，此前没有 UI 入口） */
async function handleRename(conv) {
  menuId.value = null
  try {
    const { value } = await ElMessageBox.prompt('输入新的会话名称', '重命名会话', {
      inputValue: conv.title,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValidator: (val) => (val && val.trim() ? true : '名称不能为空'),
      inputErrorMessage: '名称不能为空'
    })
    if (value && value.trim() !== conv.title) {
      await store.rename(conv.id, value.trim())
    }
  } catch {
    // 用户取消
  }
}

/** 点击菜单外部关闭 */
function onDocumentClick(event) {
  if (!menuId.value) return
  const target = event.target
  if (target && target.closest && target.closest('.ap-conv-item')) return
  closeMenu()
}

/** D13/A13：切换模型只影响当前会话，不改用户默认。 */
function handleModelChange(configId) {
  store.selectModel(configId)
}

function onScroll() {
  const el = listRef.value
  if (!el) return
  stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 40
}

/** 空态没有消息可贴，贴底会把首屏标题顶出视口 */
function shouldFollow() {
  return store.messages.length > 0 && stickToBottom.value
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function scrollToBottomAndFollow() {
  stickToBottom.value = true
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

let scrollFrame = 0
/**
 * 长会话滚动性能（A2）：
 * - 不引虚拟列表——它会与「贴底 / 回到底部 / MutationObserver」的语义冲突，
 *   风险大于收益；本系统单会话消息量在数十条量级，原生滚动足够。
 * - 用 rAF 合帧，避免每个 delta 都读 scrollHeight 触发布局抖动。
 */
function scheduleScroll() {
  if (!shouldFollow() || scrollFrame) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = 0
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

let messageObserver = null

function attachMessageObserver() {
  detachMessageObserver()
  if (!listRef.value || typeof MutationObserver === 'undefined') return
  messageObserver = new MutationObserver(() => scheduleScroll())
  messageObserver.observe(listRef.value, { childList: true, subtree: true, characterData: true })
}

function detachMessageObserver() {
  if (messageObserver) {
    messageObserver.disconnect()
    messageObserver = null
  }
}

/** D9/A10：后台标签页 rAF 暂停，回前台时把出字泵追平并贴底。 */
function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    store.flushPendingDisplay()
    scheduleScroll()
  }
}

/** 详情页「问助手」带 query 进来；题目后不再需要 query，清掉避免刷新重复带入。 */
function applyQuestionFromQuery() {
  const raw = route.query.questionId
  if (raw) {
    store.setQuestion(String(raw), '')
    router.replace({ path: '/assistant' })
  }
}

watch(
  () => store.messages.length,
  () => {
    scheduleScroll()
  }
)

watch(
  () => store.streaming,
  (value) => {
    if (!value) {
      scheduleScroll()
      nextTick(resizeInput)
    }
  }
)

onMounted(async () => {
  applyQuestionFromQuery()
  // 首屏数据与 bootstrap 并行，不阻塞对话加载
  loadDrillData()
  await store.bootstrap()
  attachMessageObserver()
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('click', onDocumentClick)
  // 只有已有消息的会话才贴底；空态贴底会把标题和推荐位顶出首屏
  if (store.messages.length > 0) await scrollToBottom()
  if (!isDesktop.value) sidebarOpen.value = false
})

onBeforeUnmount(() => {
  // D9：离开页面**不**中断流式（store 是全局单例），只清理 DOM 观察者
  detachMessageObserver()
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', onDocumentClick)
  if (scrollFrame) {
    cancelAnimationFrame(scrollFrame)
    scrollFrame = 0
  }
})
</script>

<style scoped>
/* ============================================================================
   码上记 · AI 助手「应用态」样式 v4.0
   ----------------------------------------------------------------------------
   视觉主张：一张安静的纸。
   - 全页只有一条 780px 的阅读列（顶栏标题/消息/输入卡/脚注共用同一条左右基准线），
     16px 中文约 48 字一行，长回答不串行；
   - 结构靠「灰阶层级 + 1px 发丝线 + 一处品牌蓝」，不靠重阴影和大色块；
   - 品牌标（✦）是全页唯一的有色块，只在侧栏标题、空态、回答标识三处复用。
   组织：token → 品牌标 → 侧栏 → 主区 → 顶栏 → 空态 → 消息 → Markdown → 输入卡 → 断点
   回滚：仅本文件 + base.css 的 --ai-* 令牌；外部依赖只有 DefaultLayout .--full
         与路由 meta.fullBleed，revert 这两个文件即可整体还原。
   ============================================================================ */

/* ---------- 0. 局部 token（不污染全局） ---------- */
.ap {
  /* 色（沿用 base.css 的 --ai-* 灰阶，这里只做本页别名） */
  --ap-bg: var(--color-bg-card);
  --ap-sidebar-bg: var(--ai-sidebar-bg);
  --ap-hover: var(--ai-hover);
  --ap-active: var(--ai-active);
  --ap-text: var(--ai-text);
  --ap-text-2: var(--ai-text-2);
  --ap-text-3: var(--ai-text-3);
  --ap-border: var(--ai-border);
  --ap-border-strong: var(--ai-border-strong);
  --ap-tint: var(--ai-tint);
  --ap-tint-border: #e3eaff;
  --ap-user-bubble: var(--ai-bubble);
  --ap-ink: #111827; /* 回答内加粗/小标题，比正文再深一档 */
  --ap-primary-weak: rgb(37 99 235 / 10%);

  /* 圆角 */
  --ap-radius-card: var(--ai-radius-card);
  --ap-radius-md: var(--ai-radius-md);
  --ap-radius-sm: var(--ai-radius-sm);

  /* 结构 */
  /* v4.26：阅读列 900 → 1020（+13%）。
     反馈是「内容列更宽、略微放大」——只放大字号会让行太长，加宽列宽才是对的做法。
     gutter 同步 24 → 20：列变宽后两侧留白收一点，否则在 1258 视口下
     --ap-box-max 会被 min() 压回原值，等于没改。 */
  --ap-column: 1020px;
  --ap-gutter: 20px;
  --ap-box: calc(var(--ap-column) + var(--ap-gutter) * 2); /* 含左右内边距的列外框 */
  /* 外框上限：宽屏按 1060 走；窄屏自动收窄，始终留出两侧 gutter 的空气 */
  --ap-box-max: min(var(--ap-box), calc(100% - var(--ap-gutter) * 2));
  /* 列重心左偏：会话侧栏占了左侧 264px，列若严格居中会显得“悬在中间偏右”
     v4.26：列变宽后左偏量相应收小（2.6vw → 1.6vw），避免右侧被挤 */
  --ap-shift: clamp(0px, 1.6vw, 24px);
  --ap-col-margin: max(var(--ap-gutter), calc(50% - var(--ap-box) / 2 - var(--ap-shift)));
  --ap-topbar-h: 56px;
  --ap-shadow-1: 0 1px 2px rgb(16 24 40 / 4%);
  --ap-shadow-2: 0 1px 2px rgb(16 24 40 / 5%), 0 10px 24px -18px rgb(16 24 40 / 28%);

  display: flex;
  height: calc(100dvh - var(--header-height));
  overflow: hidden;
  color: var(--ap-text);
  background-color: var(--ap-bg);
}

/* ---------- 1. 品牌标（✦） ---------- */
.ap-mark {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--color-on-primary);
  font-size: 11px;
  line-height: 1;
  background-image: linear-gradient(140deg, #5b8bff 0%, #2563eb 55%, #1d4ed8 100%);
  border-radius: 7px;
  box-shadow:
    0 2px 6px -2px rgb(37 99 235 / 45%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
}

.ap-mark--xs {
  width: 20px;
  height: 20px;
  font-size: 10px;
  border-radius: 6px;
}

.ap-mark--sm {
  width: 30px;
  height: 30px;
  font-size: 15px;
  border-radius: 9px;
}

/* v4.26: 48 -> 54, 配合标题与列宽一起放大 */
.ap-mark--lg {
  width: 54px;
  height: 54px;
  font-size: 27px;
  border-radius: 16px;
}

/* ---------- 2. 侧栏 ---------- */
.ap-sidebar {
  display: flex;
  flex: 0 0 264px;
  flex-direction: column;
  width: 264px;
  min-height: 0;
  background-color: var(--ap-sidebar-bg);
  border-right: 1px solid var(--ap-border);
}

/* 平板/移动端：覆盖式浮层（HC-2 读法甲：这是页内会话列表布局，非助手本体抽屉） */
.ap-sidebar--overlay {
  position: fixed;
  top: var(--header-height);
  bottom: 0;
  left: 0;
  z-index: 60;
  box-shadow: 0 24px 48px -24px rgb(16 24 40 / 28%);
}

.ap-sidebar__head {
  flex-shrink: 0;
  padding: 12px 12px 6px;
}

/* 品牌锁定组：标 + 两行文字（第一行是页面身份，第二行说明归属） */
.ap-sidebar__brand {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 5px 8px;
  margin: 0 -4px;
  border-radius: 10px;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.ap-sidebar__brand:hover {
  background-color: var(--ap-hover);
}

.ap-sidebar__brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ap-sidebar__brand-name {
  color: var(--ap-text);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.35;
}

.ap-sidebar__brand-sub {
  overflow: hidden;
  color: var(--ap-text-2);
  font-size: 11.5px;
  line-height: 1.45;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ap-sidebar__actions {
  flex-shrink: 0;
  padding: 4px 12px 4px;
}

/* 新建对话：它是一个「主操作」，所以给实体按钮，不再是一条裸文字 */
.ap-new-chat {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  color: var(--ap-text);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  background-color: var(--ap-bg);
  border: 1px solid var(--ap-border-strong);
  border-radius: 10px;
  box-shadow: var(--ap-shadow-1);
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ap-new-chat > svg {
  flex-shrink: 0;
  color: var(--color-primary);
  transition: color var(--motion-fast) var(--motion-ease);
}

.ap-new-chat:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: rgb(37 99 235 / 38%);
  box-shadow: 0 4px 12px -8px rgb(37 99 235 / 60%);
}

.ap-new-chat:disabled {
  color: var(--ap-text-3);
  background-color: transparent;
  border-color: var(--ap-border);
  box-shadow: none;
  cursor: not-allowed;
}

.ap-new-chat:disabled > svg {
  color: var(--ap-text-3);
}

.ap-sidebar__scroll {
  flex: 1;
  min-height: 0;
  padding: 0 8px 8px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.ap-sidebar__label {
  padding: 10px 8px 6px;
  color: var(--ap-text-2);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
  line-height: 1.5;
}

.ap-conv-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.ap-conv-item {
  position: relative;
  margin-bottom: 2px;
  border-radius: 10px;
}

.ap-conv {
  display: flex;
  flex-direction: column;
  gap: 1px;
  align-items: flex-start;
  width: 100%;
  min-width: 0;
  min-height: 42px;
  padding: 6px 30px 6px 10px;
  color: inherit;
  font-family: inherit;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ap-conv:hover {
  background-color: var(--ap-hover);
}

/* 当前会话：在灰底侧栏里浮成一张白卡，靠发丝环 + 1px 阴影成形，不用高饱和色块 */
.ap-conv.is-active {
  background-color: var(--ap-bg);
  box-shadow:
    0 0 0 1px rgb(16 24 40 / 5%),
    var(--ap-shadow-1);
}

.ap-conv__title {
  width: 100%;
  overflow: hidden;
  color: var(--ap-text);
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.45;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ap-conv.is-active .ap-conv__title {
  font-weight: 600;
}

.ap-conv__meta {
  color: var(--ap-text-2);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  line-height: 1.4;
}

/* 「更多」按钮：默认隐形，hover / 键盘聚焦 / 菜单展开时浮现
   （键盘用户 tab 到会话项时 :focus-within 也会让它出现，不会变成鼠标专属） */
.ap-conv__more {
  position: absolute;
  top: 50%;
  right: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: var(--ap-text-2);
  background: transparent;
  border: none;
  border-radius: 7px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(-50%);
  transition:
    opacity var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}

.ap-conv-item:hover .ap-conv__more,
.ap-conv-item:focus-within .ap-conv__more,
.ap-conv__more.is-open {
  opacity: 1;
}

.ap-conv__more:hover,
.ap-conv__more.is-open {
  background-color: rgb(16 24 40 / 7%);
}

/* 下拉菜单（与 InlineSelect 面板同一套节奏：项高 32 / 13px / 圆角 8） */
.ap-menu {
  position: absolute;
  top: calc(100% - 4px);
  right: 4px;
  z-index: 20;
  min-width: 116px;
  padding: 4px;
  background-color: var(--ap-bg);
  border: 1px solid var(--ap-border-strong);
  border-radius: 12px;
  box-shadow: 0 12px 28px -12px rgb(16 24 40 / 26%);
}

.ap-menu__item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 32px;
  padding: 0 10px;
  color: var(--ap-text);
  font-family: inherit;
  font-size: 13px;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.ap-menu__item:hover {
  background-color: var(--ap-hover);
}

.ap-menu__item--danger {
  color: var(--color-danger);
}

.ap-menu__item--danger:hover {
  background-color: rgb(var(--color-danger-rgb) / 8%);
}

.ap-sidebar__empty {
  padding: 8px;
  color: var(--ap-text-2);
  font-size: 12.5px;
  line-height: 1.6;
}

.ap-sidebar__foot {
  flex-shrink: 0;
  padding: 8px 12px 12px;
  border-top: 1px solid var(--ap-border);
}

.ap-sidebar__link {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 36px;
  padding: 0 8px;
  color: var(--ap-text-2);
  font-size: 13px;
  line-height: 1.5;
  border-radius: 8px;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}

.ap-sidebar__link:hover {
  color: var(--ap-text);
  background-color: var(--ap-hover);
}

.ap-mask {
  position: fixed;
  inset: var(--header-height) 0 0 0;
  z-index: 55;
  background-color: rgb(16 24 40 / 24%);
}

/* ---------- 3. 主区 ---------- */
.ap-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background-color: var(--ap-bg);
}

/* 顶栏：一条发丝线把它和正文分开，正文从下面滚过时不会「糊在标题上」 */
.ap-topbar {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  height: var(--ap-topbar-h);
  padding: 0 18px;
  border-bottom: 1px solid var(--ap-border);
}

.ap-iconbtn {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--ap-text-2);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}

.ap-iconbtn:hover {
  color: var(--ap-text);
  background-color: var(--ap-hover);
}

.ap-title {
  flex: 1;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--ap-text);
  font-size: 14.5px;
  font-weight: 600;
  /* h1 默认继承 --line-height-h1(38px)，对 15px 文字过松，会导致顶栏标题垂直偏移 */
  line-height: 1.4;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ap-title-badge {
  display: inline-flex;
  flex-shrink: 0;
  gap: 5px;
  align-items: center;
  padding: 3px 9px;
  color: var(--ap-text-2);
  font-size: 11.5px;
  line-height: 1.5;
  background-color: var(--ap-tint);
  border-radius: 999px;
}

.ap-title-badge > i {
  width: 6px;
  height: 6px;
  background-color: var(--color-primary);
  border-radius: 50%;
  animation: ap-pulse 1.2s var(--motion-ease) infinite;
}

@keyframes ap-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}

/* 带入题目：浅色提示条，和输入卡的左右基准线对齐 */
.ap-context {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  width: auto;
  max-width: var(--ap-box-max);
  padding: 6px 6px 6px 12px;
  margin: 10px 0 0 var(--ap-col-margin);
  color: var(--ap-text-2);
  font-size: 12.5px;
  line-height: 1.5;
  background-color: var(--ap-tint);
  border: 1px solid var(--ap-tint-border);
  border-radius: 10px;
}

.ap-context__text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* v4.13：带入题目后给一条回原题的路 —— 之前只能取消，想对照题干得自己回题库找 */
.ap-context__link {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--color-primary);
  font-size: 12.5px;
  text-decoration: none;
  white-space: nowrap;
}

.ap-context__link:hover {
  text-decoration: underline;
}

.ap-context__close {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  margin-left: auto;
  color: var(--ap-text-2);
  font-size: 15px;
  line-height: 1;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}

/* 有「看原题」时，关闭按钮不再吃 auto 外边距（否则两段各自 auto 会分开居中） */
.ap-context__link + .ap-context__close {
  margin-left: 0;
}

.ap-context__close:hover {
  color: var(--ap-text);
  background-color: rgb(16 24 40 / 6%);
}

/* 无可用模型引导 */
.ap-blocked {
  flex: 0 0 auto;
  width: auto;
  max-width: var(--ap-box-max);
  padding: 14px 16px;
  margin: 12px 0 0 var(--ap-col-margin);
  color: var(--ap-text-2);
  font-size: 13.5px;
  line-height: 1.7;
  text-align: center;
  background-color: #f8f9fc;
  border: 1px solid var(--ap-border);
  border-radius: 12px;
}

.ap-blocked__cta {
  display: inline-block;
  margin-top: 6px;
  color: var(--color-primary);
  font-weight: 600;
}

.ap-blocked__hint {
  margin-top: 4px;
  font-size: 12.5px;
  line-height: 1.6;
}

/* ---------- 4. 滚动区与阅读列 ---------- */
.ap-scroll {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  /* 顶部 12px 柔化：滚到中间时被截断的半行字不会硬切（列上内边距 18px 保证顶端不误伤） */
  -webkit-mask-image: linear-gradient(to bottom, transparent 0, #000 12px);
  mask-image: linear-gradient(to bottom, transparent 0, #000 12px);
}

.ap-inner {
  display: flex;
  flex-direction: column;
  width: auto;
  max-width: var(--ap-box-max);
  min-height: 100%;
  padding: 18px var(--ap-gutter) 28px;
  /* 列外框贴左基准线，右侧留白交给 max-width 兜住（不再 50/50 居中） */
  margin: 0 0 0 var(--ap-col-margin);
}

/* ---------- 5. 空态 ---------- */
/* v4.25：空态垂直「居中偏上」。
   原来 justify-content:flex-start 让剩余空间全堆在底部 —— 截图里
   「或者，随便问问」到输入框之间有约 140px 空白，像被掏空一块。
   改为居中后上下空白均衡；用 safe center 防止内容超高时顶部被裁。
   矮屏（<860px）仍回退到 flex-start，由下面的媒体查询接管。 */
.ap-empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: clamp(8px, 1.5vh, 20px);
  align-items: center;
  justify-content: center;
  justify-content: safe center;
  width: 100%;
  padding: clamp(6px, 1.6vh, 24px) 0 clamp(10px, 2vh, 28px);
}

/* v4.17：空态里的每一块都是「内容块」，不能被 flex 压扁。
   原先漏了这条：.ap-empty 是 column flex 容器，空间不足时子项按比例收缩，
   推荐区的 chip 被压到输入框底下（视觉上像被遮挡，实际是被压缩）。
   禁止收缩后内容会真实撑高，多出来的部分由 .ap-scroll 滚动承载。 */
.ap-empty > * {
  flex-shrink: 0;
}

/* v4.17：矮窗口按高度分两档收紧 —— 问题是垂直方向不够，与屏幕宽窄无关，
   所以断点用 max-height 而不是 max-width。
   第 1 档（<860px）：空态共 7 块（图标/标题/副文案/错题卡×2行/薄弱分类×2行/推荐），
   在 1440×900 这类常见笔记本上内容高度已接近临界，先收紧间距与推荐区。
   第 2 档（<620px）：无论怎么压都放不下，让装饰性的图标与副文案让位 ——
   标题 + 错题卡 + 薄弱分类才是可点入口。 */
@media (max-height: 760px) {
  .ap-empty {
    /* 矮屏内容本来就顶到边，居中会上下裁切，回退到顶部对齐 */
    justify-content: flex-start;
    gap: clamp(6px, 1.2vh, 12px);
    padding: 4px 0 4px;
  }

  .ap-empty__title {
    margin: 6px 0 5px;
    font-size: clamp(21px, 1.9vw, 26px);
  }

  .ap-empty__sub {
    font-size: 13.5px;
    line-height: 1.7;
  }

  .ap-drill__item {
    padding: 9px 11px;
  }

  /* 头部文案 + 错题卡 + 薄弱分类合计已占满可用高度时，推荐区收成一行 chip：
     标签与网格间距压到最小、chip 降低高度。
     实测 809px 高的视口里，标签底部到输入框顶部只剩 18px，而按原样式
     chip 高 35px + 标签间距 10px 必然溢出。 */
  .ap-recs__label {
    margin-bottom: 5px;
  }

  .ap-recs__grid {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .ap-rec {
    width: auto;
    max-width: 100%;
    min-height: 0;
    padding: 5px 10px;
    font-size: 12.5px;
    border-radius: 999px;
  }

  .ap-rec__text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ap-rec__arrow {
    display: none;
  }

  /* 错题卡在矮屏只留前 2 张。
     v4.26：阈值 860 → 760。字号与卡片放大后，860已经是中等窗口高度，
     在那个高度下隐藏第 2 行会导致下方留白但无内容，反而更空。 */
  .ap-drill__item:nth-child(n + 3) {
    display: none;
  }
}

@media (max-height: 560px) {
  .ap-mark--lg {
    display: none;
  }

  .ap-empty__sub {
    display: none;
  }

  .ap-empty__title {
    margin: 0 0 4px;
  }
}

/* 没有错题/统计数据时内容很少，顶部对齐会在下方留一大片空白；
   这种「新用户空态」仍回到垂直居中（内容少，居中不会显得头重脚轻）。 */
.ap-empty--lean {
  justify-content: center;
  padding: 12px 0 4px;
}

.ap-empty__head {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 520px;
  text-align: center;
}

.ap-empty__title {
  margin: 14px 0 9px;
  color: var(--ap-text);
  /* v4.26：列加宽后标题同步略放大（30 → 34 上限），保持与列宽的视觉比例 */
  font-size: clamp(26px, 2.4vw, 34px);
  font-weight: 600;
  letter-spacing: -0.022em;
  line-height: 1.36;
}

.ap-empty__sub {
  /* v4.26：副文案宽度与字号同步略放大，一行容纳更多字，减少折行 */
  max-width: 620px;
  margin: 0;
  color: var(--ap-text-2);
  font-size: 15.5px;
  line-height: 1.8;
  /* 中文一句话末尾容易掉出单字（孤字），让浏览器把两行长度拉平 */
  text-wrap: balance;
}

/* ---------- 5.1 待复习错题（v4.13） ---------- */
.ap-drill {
  width: 100%;
  max-width: var(--ap-box-max);
}

.ap-drill__head {
  display: flex;
  gap: 10px;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ap-drill__label {
  margin: 0;
  color: var(--ap-text-2);
  /* v4.26: 12 -> 13 */
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.02em;
  line-height: 1.5;
}

.ap-drill__count {
  flex-shrink: 0;
  color: var(--ap-text-3);
  /* v4.26: 12 -> 13 */
  font-size: 13px;
  line-height: 1.5;
}

.ap-drill__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.ap-drill__item {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
  /* v4.26：列加宽后题卡同步放大内边距，卡片更有分量 */
  padding: 13px 15px;
  color: var(--ap-text);
  font-family: inherit;
  text-align: left;
  background-color: var(--ap-bg);
  border: 1px solid var(--ap-border);
  border-radius: 13px;
  box-shadow: var(--ap-shadow-1);
  cursor: pointer;
  transition:
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-drill__item:hover {
  border-color: rgb(37 99 235 / 32%);
  box-shadow: 0 8px 20px -14px rgb(37 99 235 / 55%);
  transform: translateY(-1px);
}

.ap-drill__top {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.ap-drill__cat {
  overflow: hidden;
  color: var(--ap-text-3);
  /* v4.26：11.5 → 12.5，配合列加宽 */
  font-size: 12.5px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 三颗自评点：实心=当次自评档位，直观看出「这题上次答得怎么样」 */
.ap-drill__dots {
  display: inline-flex;
  flex-shrink: 0;
  gap: 3px;
  align-items: center;
}

.ap-drill__dots i {
  width: 5px;
  height: 5px;
  background-color: var(--ap-border-strong);
  border-radius: 50%;
}

.ap-drill__dots i.is-on {
  background-color: var(--color-primary);
}

.ap-drill__title {
  display: -webkit-box;
  overflow: hidden;
  /* v4.26：13.5 → 14.5，题目标题是卡片主信息，放大后更易扫读 */
  font-size: 14.5px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

/* ---------- 5.2 薄弱分类（v4.13） ---------- */
.ap-weak {
  width: 100%;
  max-width: var(--ap-box-max);
}

.ap-weak__list {
  /* 双列：4 条分类单列要占 4 行 ~130px，会把推荐位挤到折叠线以下；
     双列后压成 2 行，条形图仍保留（这是「哪里最虚」最直观的表达） */
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  /* v4.26: row gap 7 -> 10, col gap 18 -> 26 */
  gap: 10px 26px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.ap-weak__item {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  /* v4.26: 12.5 -> 13.5 */
  font-size: 13.5px;
  line-height: 1.5;
}

.ap-weak__name {
  flex: 0 0 auto;
  min-width: 0;
  max-width: 50%;
  overflow: hidden;
  color: var(--ap-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 掌握率条：底色是「未掌握」，填充是已掌握比例 */
.ap-weak__bar {
  flex: 1 1 auto;
  min-width: 0;
  /* v4.26: 5 -> 6, 更易看清比例 */
  height: 6px;
  overflow: hidden;
  background-color: var(--ap-hover);
  border-radius: 999px;
}

.ap-weak__bar i {
  display: block;
  height: 100%;
  background-color: var(--color-primary);
  border-radius: 999px;
  transition: width var(--motion-fast) var(--motion-ease);
}

.ap-weak__rate {
  flex: 0 0 auto;
  width: 34px;
  color: var(--ap-text-2);
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.ap-weak__count {
  flex: 0 0 auto;
  color: var(--ap-text-3);
  font-variant-numeric: tabular-nums;
}

.ap-recs {
  width: 100%;
}

.ap-recs__label {
  margin: 0 0 10px;
  color: var(--ap-text-2);
  /* v4.26: 12 -> 13 */
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.02em;
  line-height: 1.5;
}

/* v4.4：推荐位改竖排（单列）。原来 auto-fit 三列横排，
   三条短问题被拉成一排宽卡片，视觉上像并排的按钮组；
   竖排后每条独占一行、左对齐，更像「可选择的问题列表」。 */
.ap-recs__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.ap-rec {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  /* v4.5：竖排后不再占满整列 —— 满宽时短问题右侧会空出五六百像素，
     像一条被拉长的横条。收到 480px 并左对齐，读起来是「一列可点的问题」。 */
  justify-self: start;
  width: 100%;
  max-width: 520px;
  /* v4.26: 52 -> 56 */
  min-height: 56px;
  padding: 12px 14px 12px 16px;
  color: var(--ap-text);
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.55;
  text-align: left;
  background-color: var(--ap-bg);
  border: 1px solid var(--ap-border);
  border-radius: 14px;
  box-shadow: var(--ap-shadow-1);
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-rec__text {
  min-width: 0;
}

.ap-rec__arrow {
  flex-shrink: 0;
  color: var(--ap-text-3);
  transition:
    color var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-rec:hover {
  color: var(--color-primary);
  border-color: rgb(37 99 235 / 32%);
  box-shadow: 0 8px 20px -14px rgb(37 99 235 / 55%);
  transform: translateY(-1px);
}

.ap-rec:hover .ap-rec__arrow {
  color: var(--color-primary);
  transform: translateX(2px);
}

/* v4.13：有真实数据时，通用推荐降级成一行小 chip。
   它在空态里是「主推」；但当上面已经有错题和薄弱分类时，
   再占三条 52px 的整行会把首屏撑到两屏高，喧宾夺主。 */
.ap-recs--compact {
  max-width: var(--ap-box-max);
}

.ap-recs--compact .ap-recs__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ap-recs--compact .ap-rec {
  justify-self: auto;
  width: auto;
  max-width: 100%;
  min-height: 0;
  /* v4.26：列加宽后 chip 同步放大 */
  padding: 8px 14px;
  font-size: 13.5px;
  color: var(--ap-text-2);
  border-radius: 999px;
}

.ap-recs--compact .ap-rec__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ap-recs--compact .ap-rec__arrow {
  display: none;
}

/* 带入题目后的快捷问法：4 条短句，双列铺开更像「可点的问题」而不是一列长按钮 */
.ap-recs--question {
  max-width: var(--ap-box-max);
}

.ap-recs--question .ap-recs__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.ap-recs--question .ap-rec {
  justify-self: stretch;
  width: 100%;
  max-width: none;
  min-height: 0;
  padding: 10px 12px;
  font-size: 13px;
}

.ap-recs--question .ap-rec__arrow {
  width: 12px;
  height: 12px;
}

/* ---------- 6. 消息 ---------- */
/* v4.24：消息区重新设计 —— 用户消息与 AI 回答都进入「有边界的卡片」，
   靠左侧竖条 + 浅底建立「这是同一段对话」的节奏，取代原来散排在白底上的通排文字。 */
.ap-msg {
  width: 100%;
  max-width: 100%;
  margin: 0 0 22px;
}

.ap-msg--user {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 26px;
}

/* 用户：右侧浅蓝气泡，紧凑圆角，靠渐变区分于 AI 区 */
.ap-user {
  position: relative;
  max-width: min(76%, 560px);
  padding: 11px 15px;
  color: var(--ap-ink);
  font-size: 15.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background-image: linear-gradient(135deg, #eef3ff 0%, #e4ecff 100%);
  border: 1px solid rgb(37 99 235 / 14%);
  border-radius: 16px 16px 4px 16px;
  box-shadow: 0 2px 8px -6px rgb(37 99 235 / 45%);
}

/* 助手：通排卡片，左侧蓝色竖条标识「这是 AI 的输出」，不再是裸文字 */
.ap-assistant {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 18px 16px;
  background-color: rgb(248 250 255 / 72%);
  border: 1px solid rgb(37 99 235 / 10%);
  border-radius: 4px 14px 14px 14px;
}

.ap-assistant::before {
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 0;
  width: 3px;
  content: '';
  background: linear-gradient(180deg, #5b8bff, #2563eb);
  border-radius: 0 3px 3px 0;
}

.ap-assistant__label {
  display: flex;
  gap: 8px;
  align-items: center;
  align-self: flex-start;
  padding: 3px 10px 3px 5px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  background-color: var(--ap-primary-weak);
  border-radius: 999px;
}

.ap-assistant__body {
  color: var(--ap-text);
  font-size: 16px;
  line-height: 1.86;
  overflow-wrap: anywhere;
}

.ap-stream {
  white-space: pre-wrap;
}

.ap-thinking {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: var(--ap-text-2);
  font-size: 14px;
}

.ap-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}

.ap-dots > i {
  width: 5px;
  height: 5px;
  background-color: var(--ap-text-3);
  border-radius: 50%;
  animation: ap-dot 1.2s var(--motion-ease) infinite;
}

.ap-dots > i:nth-child(2) {
  animation-delay: 0.15s;
}

.ap-dots > i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes ap-dot {
  0%,
  60%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.ap-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background-color: var(--color-primary);
  animation: ap-blink 1s steps(2, start) infinite;
}

@keyframes ap-blink {
  to {
    visibility: hidden;
  }
}

.ap-msg__foot {
  display: flex;
  gap: 10px;
  align-items: center;
}

.ap-msg__state {
  color: var(--ap-text-2);
  font-size: 12px;
  line-height: 1.5;
}

.ap-retry {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.5;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.ap-retry:hover {
  background-color: var(--ap-primary-weak);
}

.ap-to-bottom {
  position: sticky;
  bottom: 10px;
  left: 50%;
  display: flex;
  gap: 4px;
  align-items: center;
  width: max-content;
  padding: 6px 12px;
  margin: 0 auto;
  color: var(--ap-text);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.5;
  background-color: var(--ap-bg);
  border: 1px solid var(--ap-border-strong);
  border-radius: 999px;
  box-shadow: 0 6px 18px -10px rgb(16 24 40 / 30%);
  cursor: pointer;
}

/* ---------- 7. 回答正文的 Markdown 排版 ----------
   只作用于本页（.ap-assistant__body 下），不改 markdown.css：
   题目详情等页面继续用全站默认节奏，这里按「长回答阅读」单独调。 */
.ap-assistant__body :deep(.markdown-body) {
  color: var(--ap-text);
  font-size: 16px;
  line-height: 1.86;
}

.ap-assistant__body :deep(p) {
  margin: 0 0 14px;
}

.ap-assistant__body :deep(h1),
.ap-assistant__body :deep(h2) {
  margin: 26px 0 10px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.5;
}

.ap-assistant__body :deep(h3) {
  margin: 24px 0 8px;
  font-size: 17.5px;
  font-weight: 600;
  line-height: 1.6;
}

.ap-assistant__body :deep(h4),
.ap-assistant__body :deep(h5),
.ap-assistant__body :deep(h6) {
  margin: 18px 0 6px;
  color: var(--ap-ink);
  /* 不能小于正文：markdown.css 里 h4–h6 是 16px，这里若降到 15.5 会让小标题比正文还小 */
  font-size: 16.5px;
  font-weight: 600;
  line-height: 1.6;
}

.ap-assistant__body :deep(ul),
.ap-assistant__body :deep(ol) {
  margin: 0 0 14px;
  padding-left: 22px;
}

.ap-assistant__body :deep(li) {
  margin: 5px 0;
  padding-left: 2px;
}

/* 列表符号跟着品牌蓝走，比默认黑点轻，长列表有节奏 */
.ap-assistant__body :deep(li)::marker {
  color: #8fa6d9;
}

.ap-assistant__body :deep(ol) > li::marker {
  color: var(--color-primary);
  font-weight: 600;
}

.ap-assistant__body :deep(li > ul),
.ap-assistant__body :deep(li > ol) {
  margin: 4px 0 2px;
}

.ap-assistant__body :deep(strong) {
  color: var(--ap-ink);
  font-weight: 600;
}

.ap-assistant__body :deep(blockquote) {
  padding: 10px 16px;
  margin: 14px 0;
  color: var(--ap-text-2);
  background-color: var(--ap-tint);
  border-left: 3px solid rgb(37 99 235 / 45%);
  border-radius: 0 10px 10px 0;
}

.ap-assistant__body :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

.ap-assistant__body :deep(hr) {
  margin: 24px 0;
  border-top-color: var(--ap-border);
}

.ap-assistant__body :deep(a) {
  color: var(--color-primary);
  text-decoration-color: rgb(37 99 235 / 35%);
  text-underline-offset: 3px;
}

.ap-assistant__body :deep(:not(pre) > code) {
  padding: 1px 6px;
  color: #2f3b52;
  font-size: 0.92em;
  background-color: #f3f5f9;
  border: 1px solid #e7eaf0;
  border-radius: 6px;
}

.ap-assistant__body :deep(pre) {
  margin: 14px 0;
  background-color: #fbfcfd;
  border-color: var(--ap-border);
  border-radius: 12px;
}

/* v4.13：代码块工具条（语言标签 + 复制按钮）的结构与配色由
   assets/styles/markdown.css 统一负责，这里只调正文字号 ——
   助手页正文比题目详情页略小，代码跟着小半档才不显突兀。
   间距也不要在这里覆盖：.md-code 已接管 pre 的外边距，
   再叠一层 :deep(pre) 的 margin 会出现双倍空隙。 */
.ap-assistant__body :deep(pre code) {
  padding: 14px 16px;
  font-size: 13.5px;
  line-height: 1.7;
}

.ap-assistant__body :deep(table) {
  margin: 14px 0;
  font-size: 14px;
}

.ap-assistant__body :deep(th) {
  padding: 8px 12px;
  color: var(--ap-text);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  background-color: #f7f8fb;
  border-color: var(--ap-border);
}

.ap-assistant__body :deep(td) {
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.7;
  border-color: var(--ap-border);
}

.ap-assistant__body :deep(tbody tr:nth-child(even)) {
  background-color: #fbfcfe;
}

/* ---------- 8. 输入卡 ---------- */
/* 左右内边距交给内部各行自己算，这样输入卡、提示条、脚注与阅读列共用同一条左基准线 */
.ap-composer-wrap {
  flex: 0 0 auto;
  /* v4.25：底部多留 6px，脚注不再贴着视口下沿 */
  padding: 6px 0 14px;
}

.ap-notice {
  max-width: var(--ap-box-max);
  padding: 8px 12px;
  margin: 0 0 8px var(--ap-col-margin);
  color: var(--color-danger);
  font-size: 12.5px;
  line-height: 1.6;
  background-color: rgb(var(--color-danger-rgb) / 7%);
  border-radius: 10px;
}

/* v4.25：修正 v4.24 的视觉失衡。
   问题：v4.24 让输入卡用 min(1600px, ...)，而上面的标题/题卡/薄弱分类
   都在 --ap-box-max（约 948px）这条阅读列里 —— 输入卡比内容列宽近一倍，
   上方内容收在中间、下方横贯整个容器，视觉重心断裂，中间大片空白。

   修复：
   1) 宽度回到 --ap-box-max，与标题、题卡、脚注共用同一条左右基准线；
   2) 高度压缩（128 → 104 起），一行的内容不该占小半屏；
   3) 工具行贴底、控件间距收紧，右侧额度+发送靠右对齐。
   功能、事件、状态全部不动。 */
.ap-composer {
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 与阅读列同宽：上下内容对齐，视觉重心统一 */
  width: var(--ap-box-max);
  min-height: 104px;
  max-height: 268px;
  padding: 10px 14px 8px;
  margin: 0 0 0 var(--ap-col-margin);
  background-color: var(--ap-bg);
  border: 1px solid rgb(16 24 40 / 9%);
  border-radius: 16px;
  box-sizing: border-box;
  box-shadow:
    0 1px 2px rgb(16 24 40 / 4%),
    0 10px 28px -18px rgb(16 24 40 / 16%);
  transition:
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

/* 聚焦：一圈 3px 的淡蓝光环，是整页唯一「会亮起来」的元素 */
.ap-composer:focus-within {
  border-color: rgb(37 99 235 / 42%);
  box-shadow:
    0 0 0 3px rgb(37 99 235 / 10%),
    0 14px 28px -20px rgb(16 24 40 / 34%);
}

.ap-composer.is-disabled {
  background-color: #fafbfc;
  border-color: var(--ap-border);
  box-shadow: none;
}

/* v4.25：输入区起手 52px（单行 + 少量呼吸），最高 180px。
   上限与 resizeInput() 的 Math.min 保持同步，否则 JS 会先封顶。 */
.ap-textarea {
  display: block;
  width: 100%;
  height: 52px;
  min-height: 52px;
  max-height: 180px;
  padding: 4px 2px 2px;
  color: var(--ap-text);
  font-family: inherit;
  font-size: 15.5px;
  line-height: 1.6;
  background: transparent;
  border: none;
  border-radius: 8px;
  outline: none;
  resize: none;
  box-sizing: border-box;
}

.ap-textarea::placeholder {
  color: var(--ap-text-3);
}

.ap-textarea:disabled {
  cursor: not-allowed;
}

/* v4.25：工具行贴底紧凑。左侧两个下拉、右侧额度+发送，
   中间用 auto margin 撑开，形成「左控件 / 右操作」的清晰分组。 */
.ap-composer__bar {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 40px;
  padding: 0;
}

/* 工具组（模型 + 讲解）靠左，内部 6px */
.ap-composer__tools {
  display: flex;
  flex-shrink: 0;
  min-width: 0;
  gap: 6px;
  align-items: center;
}

.ap-quota {
  margin-left: auto;
  /* v4.15：工具行变高后右侧多了 44px 发送键，额度文案必须可压缩，
     否则窄屏会把两个下拉挤到换行（flex 默认 min-width:auto 不收缩）。 */
  min-width: 0;
  overflow: hidden;
  color: var(--ap-text-2);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* v4.25：发送键收到 40px，与 40px 高的下拉保持同一基线。
   原 44px 在压缩后的工具行里偏大，会顶高整行。 */
.ap-send {
  position: relative;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: 2px;
  color: var(--color-on-primary);
  background-color: var(--color-primary);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-send:hover:not(:disabled) {
  background-color: var(--color-primary-hover);
  transform: translateY(-1px);
}

.ap-send:active:not(:disabled) {
  transform: translateY(0);
}

.ap-send:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

/* v4.19：禁用态对齐豆包圆形按钮 —— 浅灰底 + 清晰的深灰图标。
   原来图标色 #b9bec8 太淡，缩到 44px 圆里几乎看不见；
   豆包那颗麦克风是「明显可见但明显不可点」，靠的是底色浅而图标够深。 */
.ap-send:disabled {
  color: #9aa1ae;
  background-color: #f2f3f5;
  cursor: not-allowed;
}

.ap-send--stop {
  background-color: var(--ap-text);
}

.ap-send--stop:hover:not(:disabled) {
  background-color: #000;
}

.ap-footnote {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  max-width: var(--ap-box-max);
  padding: 0 4px;
  margin: 8px 0 0 var(--ap-col-margin);
  color: var(--ap-text-2);
  font-size: 11.5px;
  line-height: 1.5;
}

.ap-footnote__meta {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-variant-numeric: tabular-nums;
}

/* 字数统计等宽，输入时右侧不会左右抖动 */
.ap-footnote__count {
  font-variant-numeric: tabular-nums;
}

/* ============================================================
   10. 视觉增强（v4.23）—— 不动 DOM 与功能，仅覆盖样式
   目标：在保持「简约、低干扰」的前提下，补回蓝色主调与层级感。
   原设计几乎全靠灰阶 + 发丝线，蓝色只在品牌标和发送键出现，
   整体偏冷偏散；这里用「极淡蓝底 + 左侧强调条 + 蓝阶语义」
   把三块核心内容（当前会话 / 薄弱数据 / 输入区）串成一条视觉线。
   放在断点之前：现有 max-height / max-width 收紧规则仍可覆盖。
   ============================================================ */

/* 侧栏 · 当前会话：左侧 3px 蓝色强调条，选中态一眼可辨 */
.ap-conv.is-active {
  box-shadow:
    inset 3px 0 0 var(--color-primary),
    0 0 0 1px rgb(37 99 235 / 12%),
    var(--ap-shadow-1);
}

/* 侧栏 · 「新建对话」主操作：淡蓝底 + 蓝边，强化主操作地位 */
.ap-new-chat {
  background-color: var(--ap-primary-weak);
  border-color: rgb(37 99 235 / 22%);
}

/* v4.25：移除 v4.23 加的「标题上方短蓝线」。
   那条线绝对定位在 top:-14px，实际效果是在 ✦ 图标上方悬空一道横线，
   像渲染残留而不是设计元素，去掉更干净。 */

/* 标题里的品牌色点缀：整个标题保持深灰，只有视觉重心更稳 */
.ap-empty__title {
  color: var(--ap-ink);
}

/* 错题卡：序号徽章用青蓝渐层，卡片增加一层极淡的蓝色底，
   让「可点的练习入口」从白底上浮起来，同时不引入大色块 */
.ap-drill__item {
  position: relative;
  background:
    linear-gradient(135deg, rgb(37 99 235 / 4%) 0%, rgb(37 99 235 / 0%) 62%),
    var(--ap-bg);
}

.ap-drill__item::before {
  position: absolute;
  top: 11px;
  left: 0;
  width: 3px;
  height: 18px;
  content: '';
  background: linear-gradient(180deg, #5b8bff, #2563eb);
  border-radius: 0 3px 3px 0;
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease);
}

.ap-drill__item:hover::before {
  opacity: 1;
}

/* 分类标签：从纯灰文字升级为淡蓝胶囊，扫读时更快定位到知识点 */
.ap-drill__cat {
  display: inline-block;
  max-width: max-content;
  padding: 1px 6px;
  color: var(--color-primary);
  background-color: var(--ap-primary-weak);
  border-radius: var(--radius-tag);
}

/* 薄弱掌握度条：蓝色渐变 + 更明显的轨道，低掌握度也清晰可见 */
.ap-weak__bar {
  background-color: rgb(37 99 235 / 10%);
}

.ap-weak__bar i {
  background-image: linear-gradient(90deg, #5b8bff, #2563eb);
}

/* 薄弱分类整行：hover 时给一层淡蓝底，提示这一块也是可聚焦的信息区 */
.ap-weak__item {
  padding: 2px 6px;
  border-radius: 6px;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.ap-weak__item:hover {
  background-color: var(--ap-primary-weak);
}

/* v4.24：用户气泡与 AI 标识的视觉已在新版 .ap-user / .ap-assistant__label
   里完整实现（含渐变底与蓝边），v4.23 的这两条覆盖已删除，避免重复叠加。 */

/* 输入卡：顶部一道渐隐蓝线，把「输入区」与上方内容明确分层 */
.ap-composer {
  position: relative;
}

.ap-composer::before {
  position: absolute;
  top: -1px;
  right: 24px;
  left: 24px;
  height: 2px;
  content: '';
  background: linear-gradient(90deg, transparent, rgb(37 99 235 / 55%), transparent);
  border-radius: 2px;
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease);
}

.ap-composer:focus-within::before {
  opacity: 1;
}

/* 发送按钮：可用态加一圈柔和蓝色光晕，强化「可以发了」的反馈 */
.ap-send:not(:disabled) {
  box-shadow: 0 4px 12px -4px rgb(37 99 235 / 55%);
}

/* 脚注：字数接近上限时不做行为改动，仅让整行更轻，减少底部干扰 */
.ap-footnote {
  color: var(--ap-text-3);
}

/* ---------- 9. 断点 ---------- */
/* 平板 768–1023：侧栏改为覆盖层，阅读列吃满宽度（左偏在小屏上没有意义，一并取消） */
@media (max-width: 1023px) {
  .ap-sidebar {
    flex-basis: 280px;
    width: 280px;
  }

  .ap-inner {
    max-width: none;
    padding-right: 20px;
    padding-left: 20px;
    margin-left: 0;
  }

  .ap-composer-wrap {
    padding: 6px 16px 8px;
  }

  /* v4.20：v4.19 及以前这里会把 max-width 改成 none，让容器贴满宽度。
     现在 composer 用 width: min(1600px, 100%) 已经是「宽松屏幕 1600、窄屏贴合」，
     再设 max-width: none 会绕过 1600 上限，所以 composer 不再加入这一组。
     notice / footnote 仍保持原行为。 */
  .ap-notice,
  .ap-footnote {
    max-width: none;
    margin-left: 0;
  }

  /* composer 保留 max-width 兜底（见上），但左边距必须归零，否则整体右偏 */
  .ap-composer {
    margin-left: 0;
  }

  .ap-context,
  .ap-blocked {
    width: calc(100% - 40px);
    max-width: none;
    margin: 10px auto 0;
  }

  .ap-blocked {
    margin-top: 12px;
  }
}

/* 移动端 <768 */
@media (max-width: 767px) {
  .ap {
    height: calc(100dvh - 56px);
  }

  .ap-sidebar--overlay {
    top: 56px;
    width: min(86%, 320px);
    padding-bottom: env(safe-area-inset-bottom);
  }

  .ap-mask {
    inset: 56px 0 0 0;
  }

  .ap-topbar {
    height: 52px;
    padding: 0 12px;
  }

  .ap-inner {
    padding: 12px 16px 22px;
  }

  .ap-composer-wrap {
    padding: 4px 12px 6px;
  }

  /* v4.20：移动端同样保持「上下两区均分」的结构，
     只把总高从 160 压到 128（每区 64），否则 375px 宽的手机上输入区会太占屏。
     横向 padding 也收窄；纵向仍由 grid 精确分配，不加纵向 padding。 */
  .ap-composer {
    grid-template-rows: 64px 64px;
    height: 128px;
    padding: 0 12px;
  }

  /* 输入框 ≥16px，避免 iOS 聚焦自动放大（04 B7.3） */
  .ap-textarea {
    height: 100%;
    min-height: 0;
    padding: 10px 2px 4px;
    font-size: 16px;
  }

  .ap-empty {
    gap: 24px;
    padding-top: 4px;
  }

  .ap-empty__title {
    font-size: 23px;
  }

  .ap-empty__sub {
    font-size: 14px;
  }

  /* 移动端：薄弱分类改单列（双列在 375px 下标题只剩十来个字）；
     题卡组横向滚动本身就是单行，无需再改列数 */
  .ap-weak__list {
    grid-template-columns: 1fr;
  }

  .ap-weak__name {
    max-width: 38%;
  }

  /* 带入题目后的快捷问法在窄屏也收成一列，双列会把问句压成两行 */
  .ap-recs--question .ap-recs__grid {
    grid-template-columns: 1fr;
  }

  .ap-recs--question .ap-rec {
    min-height: 44px;
  }

  .ap-recs__grid {
    gap: 8px;
  }

  /* 竖排后每条都是整行宽，触控目标仍有 52px，够用 */
  .ap-rec {
    min-height: 52px;
  }

  /* 移动端气泡 86% */
  .ap-user {
    max-width: 86%;
  }

  /* v4.18：移动端三个控件同样等高（40px），与桌面 44px 保持一致的「齐平」策略。
     手机上略矮 4px，但三者仍然严格同高。 */
  .ap-send {
    width: 40px;
    height: 40px;
  }

  :deep(.inline-select__trigger--lg) {
    min-height: 40px;
    padding: 0 13px;
    font-size: 13px;
  }

  /* 手机上 Enter 是换行，快捷键提示没有意义 */
  .ap-footnote__hint {
    display: none;
  }

  /* v4.15：窄屏 375px 下，两个下拉 + 额度文案 + 44px 发送键挤不进一行。
     额度信息在「模型配置」页和会话页都能看到，这里让位给控件。 */
  .ap-quota {
    display: none;
  }

  .ap-context,
  .ap-blocked {
    width: calc(100% - 24px);
  }

  .ap-context {
    margin-top: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ap-cursor,
  .ap-dots > i,
  .ap-title-badge > i {
    animation: none;
  }

  .ap-rec:hover,
  .ap-send:hover:not(:disabled) {
    transform: none;
  }
}

/* ============================================================================
   v5.0 视觉焕新 ·「晨光陪练台」
   ----------------------------------------------------------------------------
   只改观感，不动交互：所有操作类名、事件与数据流保持原样。
   视觉主张：
   - 纸白底 + 两团极淡的蓝/靛蓝光晕，让整页从纯白里「浮」起来；
   - 层级靠 1px 发丝环 + 低饱和投影 + 灰阶字色，不堆大色块；
   - 品牌渐变只出现在品牌标、主操作、发送键与进度条，保持克制；
   - 空态从「一列文字」升级为「标题 + 待复习任务行 + 信息面板」三段结构。
   ============================================================================ */

/* ---------- v5.1 本页 token 校准 ---------- */
.ap {
  --ap-bg: #fbfcff;
  --ap-sidebar-bg: #f7f9fd;
  --ap-hover: #f1f4fb;
  --ap-active: #e9effb;
  --ap-text: #101828;
  --ap-text-2: #5b6474;
  --ap-text-3: #687284;
  --ap-border: rgb(15 23 42 / 7%);
  --ap-border-strong: rgb(15 23 42 / 12%);
  --ap-tint: #f4f7ff;
  --ap-tint-border: rgb(37 99 235 / 12%);
  --ap-ink: #0b1220;
  --ap-primary-weak: rgb(37 99 235 / 8%);
  --ap-radius-card: 18px;
  --ap-radius-md: 12px;
  --ap-radius-sm: 9px;
  --ap-topbar-h: 58px;
  /* v5：正文列与输入卡统一到同一条左右基准线。
     --ap-box-max 含左右 gutter，直接给它会让输入卡比内容列宽 40px；
     这里单独给出「正文内容宽度 / 左起点」，所有直接子块共用。 */
  --ap-content-width: min(calc(100% - var(--ap-gutter) * 4), var(--ap-column));
  --ap-content-margin: calc(var(--ap-col-margin) + var(--ap-gutter));
  --ap-shadow-1: 0 1px 2px rgb(16 24 40 / 4%);
  --ap-shadow-2: 0 1px 2px rgb(16 24 40 / 4%), 0 24px 48px -36px rgb(16 24 40 / 34%);
  --ap-gradient: linear-gradient(135deg, #4b8bff 0%, #2563eb 52%, #4f46e5 100%);
}

/* 主区底色：顶部两团柔光，向下收敛为纯白，长回答区保持干净 */
.ap-main {
  position: relative;
  background:
    radial-gradient(820px 460px at 86% -10%, rgb(37 99 235 / 9%), transparent 62%),
    radial-gradient(680px 400px at 4% -6%, rgb(99 102 241 / 7%), transparent 64%),
    linear-gradient(180deg, #fdfeff 0%, #fbfcff 46%, #fff 100%);
}

/* ---------- v5.2 品牌标与侧栏 ---------- */
.ap-mark {
  width: 22px;
  height: 22px;
  font-size: 11px;
  border-radius: 7px;
  background-image: var(--ap-gradient);
  box-shadow:
    0 2px 6px -2px rgb(37 99 235 / 45%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
}

.ap-mark--xs {
  width: 20px;
  height: 20px;
  font-size: 10px;
  border-radius: 6px;
}

.ap-mark--sm {
  width: 32px;
  height: 32px;
  font-size: 15px;
  border-radius: 10px;
}

.ap-mark--lg {
  position: relative;
  width: 64px;
  height: 64px;
  font-size: 28px;
  border-radius: 20px;
  box-shadow:
    0 0 0 12px rgb(37 99 235 / 5%),
    0 22px 42px -22px rgb(37 99 235 / 72%),
    inset 0 1px 0 rgb(255 255 255 / 32%);
}

.ap-mark--lg::after {
  position: absolute;
  inset: -9px;
  content: '';
  border: 1px solid rgb(37 99 235 / 14%);
  border-radius: 27px;
}

.ap-sidebar {
  background: linear-gradient(180deg, #f8faff 0%, #f6f8fd 100%);
  border-right: 1px solid rgb(15 23 42 / 6%);
}

.ap-sidebar__brand {
  padding: 6px 8px;
}

.ap-sidebar__brand-name {
  font-size: 15px;
  letter-spacing: -0.012em;
}

.ap-sidebar__brand-sub {
  color: var(--ap-text-3);
  font-size: 11px;
}

/* 新建对话：侧栏唯一的主操作，给品牌渐变与内高光 */
.ap-new-chat {
  min-height: 42px;
  padding: 0 14px;
  color: #fff;
  background-color: var(--color-primary);
  background-image: var(--ap-gradient);
  border: 1px solid rgb(255 255 255 / 16%);
  border-radius: 12px;
  box-shadow:
    0 10px 22px -14px rgb(37 99 235 / 80%),
    inset 0 1px 0 rgb(255 255 255 / 25%);
  transition:
    color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-new-chat > svg {
  color: #fff;
  opacity: 0.95;
}

.ap-new-chat:hover:not(:disabled) {
  color: #fff;
  border-color: rgb(255 255 255 / 24%);
  box-shadow:
    0 14px 26px -12px rgb(37 99 235 / 75%),
    inset 0 1px 0 rgb(255 255 255 / 28%);
  transform: translateY(-1px);
}

.ap-new-chat:disabled {
  color: #98a1b2;
  background-color: #eef1f7;
  background-image: none;
  border-color: transparent;
  box-shadow: none;
}

.ap-new-chat:disabled > svg {
  color: #98a1b2;
}

.ap-sidebar__label-row {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding: 13px 10px 6px;
}

.ap-sidebar__label {
  padding: 0;
  color: var(--ap-text-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.ap-sidebar__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  color: var(--ap-text-2);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  background-color: #eaf0fa;
  border-radius: 999px;
}

.ap-conv-item {
  margin-bottom: 4px;
}

.ap-conv {
  min-height: 46px;
  padding: 7px 32px 7px 11px;
  border: 1px solid transparent;
  border-radius: 11px;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ap-conv:hover {
  background-color: #eef3fc;
}

.ap-conv.is-active {
  background-color: #fff;
  border-color: rgb(37 99 235 / 16%);
  box-shadow:
    inset 3px 0 0 var(--color-primary),
    0 10px 22px -20px rgb(37 99 235 / 60%);
}

.ap-conv.is-active .ap-conv__title {
  color: var(--color-primary-hover);
}

.ap-conv__meta {
  color: var(--ap-text-3);
}

.ap-conv.is-active + .ap-conv__more {
  opacity: 1;
}

.ap-conv__more:hover,
.ap-conv__more.is-open {
  background-color: rgb(15 23 42 / 6%);
}

.ap-menu {
  padding: 5px;
  border-radius: 13px;
  box-shadow: 0 18px 36px -16px rgb(16 24 40 / 30%);
}

.ap-sidebar__empty {
  padding: 10px;
  color: var(--ap-text-3);
}

/* 侧栏底部：把「模型配置」做成一张可识别的入口卡 */
.ap-sidebar__foot {
  padding: 10px 12px 14px;
  border-top: 1px solid rgb(15 23 42 / 6%);
}

.ap-sidebar__link {
  gap: 10px;
  min-height: 52px;
  padding: 8px 10px;
  background-color: rgb(255 255 255 / 72%);
  border: 1px solid rgb(15 23 42 / 6%);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ap-sidebar__link:hover {
  color: var(--ap-text);
  background-color: #fff;
  border-color: rgb(37 99 235 / 22%);
  box-shadow: 0 12px 24px -20px rgb(37 99 235 / 55%);
}

.ap-sidebar__link-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: var(--color-primary);
  background-color: rgb(37 99 235 / 8%);
  border-radius: 9px;
}

.ap-sidebar__link-text {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.ap-sidebar__link-title {
  color: var(--ap-text);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.ap-sidebar__link-sub {
  color: var(--ap-text-3);
  font-size: 11px;
  line-height: 1.4;
}

.ap-sidebar__link-arrow {
  flex-shrink: 0;
  color: var(--ap-text-3);
  transition:
    color var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-sidebar__link:hover .ap-sidebar__link-arrow {
  color: var(--color-primary);
  transform: translateX(2px);
}

/* 自定义滚动条：默认灰色滚动条过于抢眼 */
.ap-scroll {
  scrollbar-width: thin;
  scrollbar-color: #cdd5e3 transparent;
}

.ap-scroll::-webkit-scrollbar {
  width: 10px;
}

.ap-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.ap-scroll::-webkit-scrollbar-thumb {
  background-color: #d3dae7;
  background-clip: content-box;
  border: 3px solid transparent;
  border-radius: 999px;
}

.ap-scroll::-webkit-scrollbar-thumb:hover {
  background-color: #b9c4d8;
}

.ap-sidebar__scroll {
  scrollbar-width: thin;
  scrollbar-color: #dde3ee transparent;
}

.ap-sidebar__scroll::-webkit-scrollbar {
  width: 8px;
}

.ap-sidebar__scroll::-webkit-scrollbar-thumb {
  background-color: #dde3ee;
  background-clip: content-box;
  border: 2px solid transparent;
  border-radius: 999px;
}

/* ---------- v5.3 顶栏 ---------- */
.ap-topbar {
  position: relative;
  z-index: 6;
  height: var(--ap-topbar-h);
  padding: 0 20px;
  background-color: rgb(255 255 255 / 72%);
  border-bottom-color: rgb(15 23 42 / 6%);
  backdrop-filter: blur(14px) saturate(1.4);
}

.ap-iconbtn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
}

.ap-iconbtn:hover {
  color: var(--color-primary);
  background-color: rgb(37 99 235 / 7%);
}

.ap-title {
  display: flex;
  gap: 9px;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.ap-title__mark {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  background-image: var(--ap-gradient);
  border-radius: 2px;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 8%);
  transform: rotate(45deg);
}

.ap-title__text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ap-title-badge {
  padding: 4px 10px;
  color: #1d4ed8;
  background-color: rgb(37 99 235 / 8%);
  border: 1px solid rgb(37 99 235 / 12%);
}

/* 带入题目提示条 */
.ap-context {
  gap: 9px;
  max-width: var(--ap-content-width);
  padding: 8px 8px 8px 10px;
  margin-top: 12px;
  margin-left: var(--ap-content-margin);
  background-image: linear-gradient(90deg, rgb(37 99 235 / 7%), rgb(99 102 241 / 5%));
  border-color: rgb(37 99 235 / 14%);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.ap-context__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--color-primary);
  background-color: #fff;
  border-radius: 7px;
  box-shadow: 0 1px 2px rgb(16 24 40 / 8%);
}

.ap-context__text {
  color: var(--ap-text-2);
}

.ap-context__link {
  font-weight: 500;
}

.ap-context__close {
  margin-left: auto;
}

.ap-context__link + .ap-context__close {
  margin-left: 0;
}

.ap-context__close:hover {
  color: var(--ap-text);
  background-color: rgb(15 23 42 / 6%);
}

/* 无可用模型 */
.ap-blocked {
  max-width: var(--ap-content-width);
  padding: 16px 18px;
  margin-left: var(--ap-content-margin);
  color: var(--ap-text-2);
  background-color: rgb(255 255 255 / 72%);
  background-image: linear-gradient(180deg, rgb(248 250 255 / 92%), #fff);
  border-color: var(--ap-border);
  border-radius: 14px;
  box-shadow: var(--ap-shadow-1);
}

.ap-blocked__cta {
  display: inline-flex;
  align-items: center;
  margin-top: 6px;
  font-weight: 600;
}

/* ---------- v5.4 阅读列与空态 ---------- */
.ap-inner {
  padding: 24px var(--ap-gutter) 36px;
}

.ap-empty {
  gap: clamp(14px, 2.1vh, 22px);
  padding: clamp(8px, 2vh, 26px) 0 clamp(14px, 2.4vh, 32px);
}

.ap-empty__head {
  max-width: 620px;
}

.ap-empty__eyebrow {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  margin-top: 18px;
  padding: 5px 12px;
  color: #1d4ed8;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 1.5;
  background-color: rgb(37 99 235 / 7%);
  border: 1px solid rgb(37 99 235 / 12%);
  border-radius: 999px;
}

.ap-empty__eyebrow::before {
  width: 5px;
  height: 5px;
  content: '';
  background-color: currentcolor;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

.ap-empty__title {
  margin: 16px 0 10px;
  color: var(--ap-ink);
  font-size: clamp(28px, 2.6vw, 38px);
  font-weight: 600;
  letter-spacing: -0.03em;
  line-height: 1.3;
}

.ap-empty__sub {
  max-width: 580px;
  color: var(--ap-text-2);
  font-size: 15px;
  line-height: 1.8;
}

.ap-empty--lean {
  justify-content: center;
  padding: 12px 0 8px;
}

/* ---------- v5.5 信息面板通用 ---------- */
.ap-panel {
  width: 100%;
  padding: 16px;
  background-color: rgb(255 255 255 / 74%);
  background-image: radial-gradient(420px 240px at 100% 0%, rgb(37 99 235 / 4.5%), transparent 72%);
  border: 1px solid var(--ap-border);
  border-radius: var(--ap-radius-card);
  box-shadow:
    0 1px 2px rgb(16 24 40 / 3%),
    0 28px 52px -44px rgb(16 24 40 / 44%);
  backdrop-filter: blur(8px);
}

.ap-panel__head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ap-panel__title {
  display: flex;
  gap: 9px;
  align-items: center;
  min-width: 0;
}

.ap-panel__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--color-primary);
  background-color: rgb(37 99 235 / 8%);
  border-radius: 9px;
}

.ap-panel__icon--weak {
  color: #4f46e5;
  background-color: rgb(99 102 241 / 9%);
}

.ap-panel__icon--ask {
  color: #0e7490;
  background-color: rgb(14 165 233 / 9%);
}

.ap-panel__title-text {
  color: var(--ap-text);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.ap-panel__hint {
  flex-shrink: 0;
  color: var(--ap-text-3);
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
}

.ap-panel__badge {
  display: inline-flex;
  flex-shrink: 0;
  gap: 6px;
  align-items: center;
  padding: 4px 10px;
  color: #1d4ed8;
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
  background-color: rgb(37 99 235 / 7%);
  border-radius: 999px;
}

.ap-panel__badge > i {
  width: 5px;
  height: 5px;
  background-color: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(34 197 94 / 12%);
}

/* ---------- v5.6 待复习卡片 ---------- */
.ap-drill {
  display: flex;
  flex-direction: column;
  max-width: none;
}

/* auto-fit：宽屏 4 张一行、中屏 3 张、平板 2 张、手机 1 张，
   把原来固定的 2×2 压成一行，首屏高度明显降低 */
.ap-drill__grid {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.ap-drill__item {
  position: relative;
  gap: 7px;
  padding: 12px 13px 10px;
  overflow: hidden;
  background-image: linear-gradient(180deg, #fff 0%, #fbfcff 100%);
  border: 1px solid rgb(15 23 42 / 7%);
  border-radius: 14px;
  box-shadow: none;
}

/* 顶部渐隐彩条：hover 时浮现，替代原来左侧的实心竖条 */
.ap-drill__item::before {
  top: 0;
  right: 0;
  bottom: auto;
  left: 0;
  width: auto;
  height: 2px;
  background: linear-gradient(90deg, #60a5fa, #2563eb 55%, #4f46e5);
  border-radius: 0;
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease);
}

.ap-drill__item:hover {
  border-color: rgb(37 99 235 / 28%);
  box-shadow: 0 18px 32px -26px rgb(37 99 235 / 60%);
  transform: translateY(-2px);
}

.ap-drill__item:hover::before {
  opacity: 1;
}

.ap-drill__cat {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  max-width: 100%;
  padding: 2px 8px;
  overflow: hidden;
  color: #1d4ed8;
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
  text-overflow: ellipsis;
  background-color: rgb(37 99 235 / 7%);
  border-radius: 6px;
}

.ap-drill__cat::before {
  flex-shrink: 0;
  width: 4px;
  height: 4px;
  content: '';
  background-color: #3b82f6;
  border-radius: 50%;
}

.ap-drill__dots i {
  width: 6px;
  height: 6px;
  background-color: #e2e6ef;
}

.ap-drill__dots i.is-on {
  background-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgb(37 99 235 / 10%);
}

.ap-drill__title {
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.55;
  -webkit-line-clamp: 3;
  line-clamp: 3;
}

.ap-drill__foot {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-top: 1px;
}

/* 「自评」+ 三颗档位点：放在卡片底部，避免右上角三点被误认成「更多」菜单 */
.ap-drill__assess {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.ap-drill__assess-label {
  color: var(--ap-text-3);
  font-size: 10.5px;
  line-height: 1.5;
  letter-spacing: 0.04em;
}

.ap-drill__go {
  display: inline-flex;
  gap: 2px;
  align-items: center;
  color: var(--color-primary);
  font-size: 11.5px;
  line-height: 1.5;
  white-space: nowrap;
  opacity: 0;
  transform: translateX(-3px);
  transition:
    opacity var(--motion-fast) var(--motion-ease),
    transform var(--motion-fast) var(--motion-ease);
}

.ap-drill__item:hover .ap-drill__go,
.ap-drill__item:focus-visible .ap-drill__go {
  opacity: 1;
  transform: none;
}

/* ---------- v5.7 薄弱分类 / 推荐问法 ---------- */
.ap-insights {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  width: 100%;
  align-items: stretch;
}

.ap-insights--split {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.ap-weak,
.ap-recs {
  display: flex;
  flex-direction: column;
  max-width: none;
}

.ap-weak__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ap-weak__item {
  display: flex;
  flex-direction: column;
  gap: 7px;
  /* 旧样式遗留的 align-items:center 会把行内元素缩成内容宽度并居中，
     这里显式拉满，进度条才能占满整行 */
  align-items: stretch;
  padding: 9px 11px;
  font-size: 13px;
  background-color: #fbfcfe;
  border: 1px solid rgb(15 23 42 / 5%);
  border-radius: 11px;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease);
}

.ap-weak__item:hover {
  background-color: #f5f8ff;
  border-color: rgb(37 99 235 / 14%);
}

.ap-weak__row {
  display: flex;
  gap: 10px;
  align-items: baseline;
  justify-content: space-between;
}

.ap-weak__name {
  flex: 1;
  min-width: 0;
  max-width: none;
  color: var(--ap-text);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
}

.ap-weak__rate {
  flex-shrink: 0;
  color: #1d4ed8;
  font-size: 12.5px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
}

.ap-weak__track {
  display: flex;
  gap: 10px;
  align-items: center;
}

.ap-weak__bar {
  flex: 1;
  height: 6px;
  background-color: rgb(37 99 235 / 10%);
  border-radius: 999px;
}

.ap-weak__bar i {
  background-image: linear-gradient(90deg, #60a5fa, #2563eb 70%, #4f46e5);
}

.ap-weak__count {
  flex-shrink: 0;
  color: var(--ap-text-3);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
}

.ap-recs__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.ap-recs__tip {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: auto;
  padding-top: 12px;
  color: var(--ap-text-3);
  font-size: 11.5px;
  line-height: 1.5;
}

.ap-recs__tip::before {
  color: #93a7e8;
  font-size: 10px;
  content: '✦';
}

.ap-rec {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  justify-self: stretch;
  width: 100%;
  max-width: none;
  min-height: 46px;
  padding: 10px 13px;
  color: #1a2233;
  font-size: 13px;
  line-height: 1.55;
  text-align: left;
  background-color: #fbfcfe;
  border: 1px solid rgb(15 23 42 / 6%);
  border-radius: 11px;
  box-shadow: none;
}

.ap-rec::before {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  content: '';
  background-image: var(--ap-gradient);
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgb(37 99 235 / 8%);
}

.ap-rec__text {
  flex: 1;
  min-width: 0;
}

.ap-rec:hover {
  color: var(--color-primary);
  background-color: #fff;
  border-color: rgb(37 99 235 / 26%);
  box-shadow: 0 14px 26px -22px rgb(37 99 235 / 60%);
  transform: translateY(-1px);
}

.ap-rec:hover .ap-rec__arrow {
  color: var(--color-primary);
  transform: translateX(2px);
}

/* 有真实数据时：收成「带圆点的问句行」，不像一排宽按钮 */
.ap-recs--compact .ap-recs__grid {
  display: flex;
  flex-direction: column;
  flex-wrap: nowrap;
  gap: 8px;
}

.ap-recs--compact .ap-rec {
  justify-self: auto;
  width: 100%;
  max-width: none;
  min-height: 44px;
  padding: 9px 12px;
  font-size: 13px;
  color: #1a2233;
  border-radius: 11px;
}

.ap-recs--compact .ap-rec__text {
  overflow: visible;
  white-space: normal;
  text-overflow: clip;
}

/* 只有推荐、没有错题数据时：三张等宽卡并排，撑住空态的下半屏 */
.ap-recs--solo .ap-recs__grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.ap-recs--solo .ap-rec {
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 112px;
  padding: 15px;
  font-size: 14px;
  border-radius: 14px;
  background-image: linear-gradient(180deg, #fff 0%, #fafcff 100%);
}

.ap-recs--solo .ap-rec__text {
  font-size: 14px;
  line-height: 1.6;
}

.ap-recs--solo .ap-rec__arrow {
  align-self: flex-end;
  color: #b6c0d4;
}

/* 带入题目后：4 条短问句铺成 2×2 */
.ap-recs--question .ap-recs__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ap-recs--question .ap-rec {
  min-height: 48px;
}

/* ---------- v5.8 消息区 ---------- */
.ap-msg {
  margin-bottom: 24px;
}

.ap-msg--user {
  margin-bottom: 28px;
}

.ap-user {
  max-width: min(72%, 560px);
  padding: 12px 16px;
  color: #fff;
  font-size: 15px;
  line-height: 1.7;
  background-color: var(--color-primary);
  background-image: var(--ap-gradient);
  border: 1px solid rgb(37 99 235 / 35%);
  border-radius: 18px 18px 6px 18px;
  box-shadow:
    0 12px 24px -18px rgb(37 99 235 / 80%),
    inset 0 1px 0 rgb(255 255 255 / 18%);
}

.ap-assistant {
  gap: 12px;
  padding: 16px 18px 18px;
  background-color: #fff;
  background-image: linear-gradient(180deg, rgb(248 250 255 / 94%), #fff 72%);
  border: 1px solid rgb(37 99 235 / 10%);
  border-radius: 6px 18px 18px 18px;
  box-shadow:
    0 1px 2px rgb(16 24 40 / 3%),
    0 28px 52px -46px rgb(16 24 40 / 45%);
}

.ap-assistant::before {
  top: 16px;
  bottom: 16px;
  width: 3px;
  background: linear-gradient(180deg, #60a5fa, #2563eb 50%, #4f46e5);
}

.ap-assistant__label {
  padding: 4px 10px 4px 5px;
  color: #1d4ed8;
  background-color: rgb(37 99 235 / 8%);
  border: 1px solid rgb(37 99 235 / 10%);
}

.ap-assistant__body {
  line-height: 1.85;
}

.ap-retry {
  color: var(--color-primary);
  background-color: rgb(37 99 235 / 6%);
}

.ap-retry:hover {
  background-color: rgb(37 99 235 / 12%);
}

.ap-to-bottom {
  padding: 7px 14px;
  background-color: rgb(255 255 255 / 92%);
  box-shadow:
    0 1px 2px rgb(16 24 40 / 4%),
    0 10px 24px -14px rgb(16 24 40 / 30%);
  backdrop-filter: blur(8px);
}

/* ---------- v5.9 输入卡 ---------- */
.ap-composer-wrap {
  position: relative;
  padding: 8px 0 16px;
}

/* 输入卡上方渐隐，滚动内容不会在卡片顶边被硬切 */
.ap-composer-wrap::before {
  position: absolute;
  top: -14px;
  right: 0;
  left: 0;
  z-index: 1;
  height: 14px;
  pointer-events: none;
  content: '';
  background: linear-gradient(to bottom, rgb(251 252 255 / 0%), rgb(251 252 255 / 92%));
}

.ap-notice {
  max-width: var(--ap-content-width);
  margin-left: var(--ap-content-margin);
  color: #b42318;
  background-color: rgb(220 38 38 / 6%);
  border: 1px solid rgb(220 38 38 / 10%);
  border-radius: 11px;
}

/* 渐变发丝描边：常态就有一圈极淡的品牌色，聚焦时点亮 */
.ap-composer {
  width: var(--ap-content-width);
  min-height: 108px;
  padding: 12px 14px 10px;
  margin-left: var(--ap-content-margin);
  background:
    linear-gradient(180deg, #fff, #fdfdff) padding-box,
    linear-gradient(135deg, rgb(37 99 235 / 35%), rgb(99 102 241 / 18%) 45%, rgb(37 99 235 / 10%))
      border-box;
  border: 1px solid transparent;
  border-radius: 20px;
  box-shadow:
    0 2px 4px rgb(16 24 40 / 4%),
    0 26px 52px -38px rgb(16 24 40 / 42%);
}

.ap-composer::before {
  display: none;
}

.ap-composer:focus-within {
  background:
    linear-gradient(180deg, #fff, #fff) padding-box,
    linear-gradient(135deg, rgb(37 99 235 / 62%), rgb(99 102 241 / 42%)) border-box;
  box-shadow:
    0 0 0 4px rgb(37 99 235 / 8%),
    0 30px 56px -38px rgb(37 99 235 / 48%);
}

.ap-composer.is-disabled {
  background:
    linear-gradient(#fafbfc, #fafbfc) padding-box,
    linear-gradient(#e9ecf3, #e9ecf3) border-box;
  border-color: transparent;
  box-shadow: none;
}

.ap-textarea {
  padding: 5px 2px 3px;
  line-height: 1.65;
}

.ap-textarea::placeholder {
  color: #8a93a3;
}

.ap-composer__bar {
  gap: 8px;
  min-height: 38px;
}

/* 工具行下拉：从「透明文字」改成有边界的浅色胶囊，图标跟随主色 */
.ap-composer :deep(.inline-select__trigger--lg) {
  min-height: 36px;
  padding: 0 11px;
  color: #344054;
  font-size: 13px;
  background-color: #f5f7fb;
  border: 1px solid rgb(15 23 42 / 5%);
  border-radius: 10px;
}

.ap-composer :deep(.inline-select__trigger--lg:hover) {
  color: var(--color-primary);
  background-color: #fff;
  border-color: rgb(37 99 235 / 22%);
}

.ap-composer :deep(.inline-select__trigger--lg.is-open) {
  color: var(--color-primary);
  background-color: #fff;
  border-color: rgb(37 99 235 / 30%);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 8%);
}

.ap-composer :deep(.inline-select__icon) {
  color: currentcolor;
}

.ap-composer :deep(.inline-select__panel) {
  border-radius: 14px;
  box-shadow: 0 20px 40px -18px rgb(16 24 40 / 30%);
}

.ap-quota {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 4px 9px;
  color: var(--ap-text-2);
  font-size: 11.5px;
  background-color: #f5f7fb;
  border: 1px solid rgb(15 23 42 / 4%);
  border-radius: 999px;
}

.ap-quota > i {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  background-color: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(34 197 94 / 10%);
}

.ap-quota.is-empty > i {
  background-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgb(220 38 38 / 10%);
}

.ap-send {
  width: 40px;
  height: 40px;
  color: #fff;
  background-color: var(--color-primary);
  background-image: var(--ap-gradient);
  box-shadow:
    0 10px 20px -12px rgb(37 99 235 / 80%),
    inset 0 1px 0 rgb(255 255 255 / 25%);
}

.ap-send:hover:not(:disabled) {
  background-color: var(--color-primary-hover);
  background-image: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 55%, #4338ca 100%);
  box-shadow:
    0 14px 26px -12px rgb(37 99 235 / 80%),
    inset 0 1px 0 rgb(255 255 255 / 28%);
}

.ap-send:disabled {
  color: #a4acba;
  background-color: #eef1f6;
  background-image: none;
  box-shadow: none;
}

.ap-send--stop {
  color: #fff;
  background-color: #101828;
  background-image: none;
  box-shadow: 0 10px 20px -14px rgb(16 24 40 / 60%);
}

.ap-send--stop:hover:not(:disabled) {
  background-color: #000;
  background-image: none;
}

.ap-footnote {
  gap: 12px;
  max-width: var(--ap-content-width);
  padding: 0 6px;
  margin-top: 10px;
  margin-left: var(--ap-content-margin);
  color: var(--ap-text-3);
}

.ap-footnote__hint {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.ap-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  color: #667085;
  font-family: var(--font-family-mono);
  font-size: 10.5px;
  line-height: 1;
  background-color: #f4f6fa;
  border: 1px solid rgb(15 23 42 / 8%);
  border-bottom-width: 2px;
  border-radius: 5px;
}

.ap-kbd__plus,
.ap-footnote__sep {
  color: #b0b8c6;
}

/* ---------- v5.10 响应式 ---------- */
@media (max-width: 1023px) {
  .ap-inner {
    max-width: none;
    padding: 20px 20px 30px;
    margin-left: 0;
  }

  .ap-composer-wrap {
    padding: 8px 20px 16px;
  }

  .ap-composer {
    width: 100%;
    margin-left: 0;
  }

  .ap-context,
  .ap-blocked {
    width: calc(100% - 40px);
    max-width: none;
    margin: 10px auto 0;
  }

  .ap-blocked {
    margin-top: 12px;
  }

  /* 外层 wrap 已有 20px 侧内边距，子项不再叠加外边距 */
  .ap-notice,
  .ap-footnote {
    max-width: none;
    margin-right: 0;
    margin-left: 0;
  }
}

@media (max-width: 767px) {
  .ap-topbar {
    height: 52px;
    padding: 0 12px;
  }

  .ap-inner {
    padding: 14px 16px 24px;
  }

  .ap-context,
  .ap-blocked {
    width: calc(100% - 24px);
  }

  .ap-context {
    margin-top: 8px;
  }

  .ap-empty {
    gap: 16px;
    padding-top: 6px;
  }

  .ap-mark--lg {
    width: 54px;
    height: 54px;
    font-size: 24px;
    border-radius: 17px;
  }

  .ap-mark--lg::after {
    inset: -8px;
    border-radius: 23px;
  }

  .ap-empty__eyebrow {
    margin-top: 14px;
    font-size: 11px;
  }

  .ap-empty__title {
    margin-top: 13px;
    font-size: 25px;
  }

  .ap-empty__sub {
    font-size: 14px;
    line-height: 1.75;
  }

  .ap-panel {
    padding: 14px;
    border-radius: 16px;
  }

  .ap-panel__head {
    gap: 8px;
    margin-bottom: 10px;
  }

  .ap-panel__hint {
    display: none;
  }

  .ap-drill__grid {
    grid-template-columns: 1fr;
  }

  .ap-drill__item {
    padding: 11px 12px 9px;
  }

  .ap-drill__title {
    -webkit-line-clamp: 2;
    line-clamp: 2;
  }

  .ap-recs--solo .ap-recs__grid,
  .ap-recs--question .ap-recs__grid {
    grid-template-columns: 1fr;
  }

  .ap-recs--solo .ap-rec {
    min-height: 64px;
    flex-direction: row;
    align-items: center;
  }

  .ap-recs--solo .ap-rec__arrow {
    align-self: center;
  }

  .ap-rec {
    min-height: 50px;
  }

  .ap-user {
    max-width: 88%;
  }

  .ap-composer-wrap {
    padding: 4px 16px 10px;
  }

  .ap-composer {
    height: auto;
    min-height: 116px;
    padding: 10px 12px 8px;
  }

  .ap-textarea {
    height: 56px;
    min-height: 56px;
    padding: 8px 2px 4px;
    font-size: 16px;
  }

  .ap-send {
    width: 40px;
    height: 40px;
  }

  .ap-composer :deep(.inline-select__trigger--lg) {
    min-height: 38px;
    padding: 0 11px;
    font-size: 13px;
  }

  /* 窄屏把额度让给两个下拉，避免工具行换行 */
  .ap-quota {
    display: none;
  }

  .ap-footnote {
    margin-top: 8px;
  }

  .ap-footnote__hint {
    display: none;
  }

  .ap-recs__tip {
    padding-top: 10px;
  }
}

/* 矮屏：压缩头部装饰，优先露出「题卡 + 面板」 */
@media (max-height: 760px) {
  .ap-empty {
    justify-content: flex-start;
    gap: clamp(10px, 1.4vh, 16px);
    padding: 4px 0 12px;
  }

  .ap-mark--lg {
    width: 52px;
    height: 52px;
    font-size: 23px;
    border-radius: 16px;
  }

  .ap-mark--lg::after {
    inset: -7px;
    border-radius: 21px;
  }

  .ap-empty__eyebrow {
    margin-top: 10px;
  }

  .ap-empty__title {
    margin: 10px 0 6px;
    font-size: clamp(24px, 2vw, 30px);
  }

  .ap-empty__sub {
    font-size: 14px;
    line-height: 1.7;
  }

  .ap-panel {
    padding: 14px;
  }

  .ap-recs__tip {
    display: none;
  }
}

/* 触屏设备没有 hover，把「带入题目」常显，避免操作不可发现 */
@media (hover: none) {
  .ap-drill__go {
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ap-new-chat:hover:not(:disabled),
  .ap-drill__item:hover {
    transform: none;
  }
}

/* ============================================================================
   v7.0 设计参考 · 豆包网页版
   ----------------------------------------------------------------------------
   目标：把助手页从「信息面板工作台」收敛成豆包式的对话界面。
   - 左栏中性浅灰、新建对话为灰底、当前会话为浅灰圆角块；
   - 内容列收窄到 860px 并居中，输入框改为浅灰大圆角，发送键为纯蓝圆形；
   - 空态只保留问候、建议卡与练习数据，去除厚重玻璃卡片；
   - 用户气泡浅灰，AI 回答去掉卡片改为正文直排；
   - 所有交互、数据结构与状态逻辑保持 v5 原样。
   ============================================================================ */

.ap {
  --ap-bg: #fff;
  --ap-sidebar-bg: #f7f7f8;
  --ap-hover: #f0f1f4;
  --ap-active: #e8e9ed;
  --ap-text: #191b1f;
  --ap-text-2: #5f636b;
  --ap-text-3: #8f949d;
  --ap-border: #ececef;
  --ap-border-strong: #e0e1e5;
  --ap-tint: #f7f8fa;
  --ap-tint-border: #ececef;
  --ap-ink: #111317;
  --ap-primary-weak: rgb(37 99 235 / 6%);
  --ap-radius-card: 16px;
  --ap-radius-md: 12px;
  --ap-radius-sm: 8px;
  --ap-topbar-h: 56px;
  --ap-column: 860px;
  --ap-gutter: 16px;
  --ap-shift: 0px;
  --ap-shadow-1: 0 1px 2px rgb(16 24 40 / 3%);
  --ap-shadow-2: 0 16px 32px -32px rgb(16 24 40 / 26%);
}

.ap-main {
  background:
    radial-gradient(680px 340px at 50% -12%, rgb(37 99 235 / 4%), transparent 70%),
    #fff;
}

/* ---------- 左栏：豆包式会话栏 ---------- */
.ap-sidebar {
  background: #f7f7f8;
  border-right: 1px solid #ececef;
}

.ap-sidebar__brand {
  padding: 6px 8px;
}

.ap-sidebar__brand-name {
  font-size: 15px;
  letter-spacing: -0.012em;
}

.ap-sidebar__brand-sub {
  color: #8f949d;
  font-size: 11px;
}

.ap-new-chat {
  min-height: 40px;
  color: #191b1f;
  background-color: #ecedf0;
  background-image: none;
  border: 1px solid transparent;
  border-radius: 10px;
  box-shadow: none;
}

.ap-new-chat > svg {
  color: #4b5563;
  opacity: 1;
}

.ap-new-chat:hover:not(:disabled) {
  color: #111827;
  background-color: #e3e4e8;
  border-color: transparent;
  box-shadow: none;
  transform: none;
}

.ap-new-chat:disabled {
  color: #a0a4ac;
  background-color: #f0f1f3;
  border-color: transparent;
}

.ap-new-chat:disabled > svg {
  color: #a0a4ac;
}

.ap-sidebar__label-row {
  padding: 13px 10px 6px;
}

.ap-sidebar__label {
  color: #8f949d;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: none;
}

.ap-sidebar__count {
  color: #8f949d;
  background-color: #ebedf0;
}

.ap-conv-item {
  margin-bottom: 2px;
}

.ap-conv {
  min-height: 42px;
  padding: 6px 30px 6px 10px;
  border: none;
  border-radius: 8px;
}

.ap-conv:hover {
  background-color: #efeff2;
}

.ap-conv.is-active {
  background-color: #e8e9ed;
  border-color: transparent;
  box-shadow: none;
}

.ap-conv.is-active .ap-conv__title {
  color: #191b1f;
  font-weight: 600;
}

.ap-conv__title {
  font-size: 13.5px;
  font-weight: 400;
}

.ap-conv__meta {
  color: #9a9ea7;
  font-size: 11px;
}

.ap-conv__more:hover,
.ap-conv__more.is-open {
  background-color: rgb(15 23 42 / 6%);
}

.ap-menu {
  border-color: #ececef;
  border-radius: 12px;
  box-shadow: 0 14px 32px -18px rgb(16 24 40 / 24%);
}

.ap-sidebar__empty {
  color: #9a9ea7;
}

.ap-sidebar__foot {
  padding: 8px 10px 12px;
  border-top: 1px solid #ececef;
}

.ap-sidebar__link {
  gap: 9px;
  min-height: 40px;
  padding: 0 8px;
  background-color: transparent;
  border-color: transparent;
  border-radius: 8px;
  box-shadow: none;
}

.ap-sidebar__link:hover {
  color: #191b1f;
  background-color: #efeff2;
  border-color: transparent;
  box-shadow: none;
}

.ap-sidebar__link-icon {
  width: 28px;
  height: 28px;
  color: #4b5563;
  background-color: #e9eaee;
  border-radius: 8px;
}

.ap-sidebar__link-title {
  font-size: 13px;
  font-weight: 500;
  color: #191b1f;
}

.ap-sidebar__link-sub {
  display: none;
}

.ap-sidebar__link-arrow {
  color: #a0a4ac;
}

.ap-sidebar__link:hover .ap-sidebar__link-arrow {
  color: #4b5563;
  transform: translateX(1px);
}

.ap-sidebar__scroll {
  scrollbar-color: #d9dbe0 transparent;
}

.ap-sidebar__scroll::-webkit-scrollbar-thumb {
  background-color: #d9dbe0;
}

/* ---------- 顶栏 ---------- */
.ap-topbar {
  height: var(--ap-topbar-h);
  padding: 0 20px;
  background-color: #fff;
  border-bottom-color: #f0f0f2;
  backdrop-filter: none;
}

.ap-iconbtn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.ap-iconbtn:hover {
  color: #191b1f;
  background-color: #f2f2f4;
}

.ap-title {
  gap: 0;
  color: #191b1f;
  font-size: 14px;
  font-weight: 500;
}

.ap-title__mark {
  display: none;
}

.ap-title-badge {
  padding: 4px 9px;
  color: #6b7280;
  background-color: #f3f4f6;
  border: none;
}

.ap-title-badge > i {
  background-color: #2563eb;
}

/* ---------- 带入题目 / 无模型 ---------- */
.ap-context {
  background-color: #f7f8fa;
  background-image: none;
  border-color: #ececef;
  box-shadow: none;
  color: #5f636b;
}

.ap-context__icon {
  color: #2563eb;
  background-color: #fff;
  border: 1px solid #ececef;
  box-shadow: none;
}

.ap-context__close:hover {
  color: #191b1f;
  background-color: #ececef;
}

.ap-blocked {
  background-color: #fff;
  background-image: none;
  border-color: #ececef;
  box-shadow: none;
}

/* ---------- 空态：问候 + 轻量建议 ---------- */
.ap-empty {
  gap: 18px;
  padding: 26px 0 20px;
}

.ap-empty__head {
  max-width: 560px;
}

.ap-mark--lg {
  width: 56px;
  height: 56px;
  font-size: 24px;
  border-radius: 16px;
  box-shadow:
    0 12px 28px -18px rgb(37 99 235 / 55%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
}

.ap-mark--lg::after {
  inset: -6px;
  border-color: rgb(37 99 235 / 10%);
  border-radius: 21px;
}

.ap-empty__eyebrow {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  padding: 0;
  margin-top: 12px;
  color: #8f949d;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0;
  background: none;
  border: none;
}

.ap-empty__eyebrow::before {
  width: 5px;
  height: 5px;
  background-color: #2563eb;
  box-shadow: none;
}

.ap-empty__title {
  margin: 12px 0 8px;
  color: #191b1f;
  font-size: clamp(26px, 2.4vw, 32px);
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.ap-empty__sub {
  max-width: 520px;
  color: #6b7280;
  font-size: 14.5px;
  line-height: 1.75;
}

.ap-empty--lean {
  padding: 20px 0 10px;
}

/* ---------- 区块：去掉玻璃卡片，只留标题与内容 ---------- */
.ap-panel {
  padding: 0;
  background: none;
  border: none;
  border-radius: 0;
  box-shadow: none;
  backdrop-filter: none;
}

.ap-panel__head {
  gap: 10px;
  margin-bottom: 10px;
}

.ap-panel__title {
  gap: 7px;
}

.ap-panel__icon {
  width: 22px;
  height: 22px;
  color: #6b7280;
  background-color: #f3f4f6;
  border-radius: 6px;
}

.ap-panel__icon--weak,
.ap-panel__icon--ask {
  color: #6b7280;
  background-color: #f3f4f6;
}

.ap-panel__title-text {
  color: #191b1f;
  font-size: 13.5px;
  font-weight: 600;
}

.ap-panel__hint {
  color: #9a9ea7;
  font-size: 11.5px;
}

.ap-panel__badge {
  gap: 5px;
  padding: 0;
  color: #9a9ea7;
  font-size: 11.5px;
  background: none;
}

.ap-panel__badge > i {
  background-color: #2563eb;
  box-shadow: none;
}

/* 建议区不再需要「点击填入」这类辅助提示 */
.ap-recs .ap-panel__hint {
  display: none;
}

/* ---------- 待复习：白底细边卡 ---------- */
.ap-drill {
  padding-top: 18px;
  border-top: 1px solid #f0f0f2;
}

.ap-drill__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ap-drill__item {
  gap: 7px;
  padding: 14px;
  background: #fff;
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: none;
}

.ap-drill__item::before {
  display: none;
}

.ap-drill__item:hover {
  background-color: #fcfcfd;
  border-color: #d9dce2;
  box-shadow: none;
  transform: none;
}

.ap-drill__cat {
  padding: 2px 7px;
  color: #4b5563;
  font-size: 11px;
  background-color: #f3f4f6;
  border-radius: 5px;
}

.ap-drill__cat::before {
  background-color: #9ca3af;
}

.ap-drill__dots i {
  background-color: #e5e7eb;
}

.ap-drill__dots i.is-on {
  background-color: #2563eb;
  box-shadow: none;
}

.ap-drill__title {
  color: #191b1f;
  font-size: 14px;
  line-height: 1.55;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.ap-drill__assess-label {
  color: #9ca3af;
  font-size: 11px;
}

.ap-drill__go {
  color: #2563eb;
  font-size: 11.5px;
}

/* ---------- 薄弱分类：浅灰统计块 ---------- */
.ap-weak {
  padding-top: 18px;
  border-top: 1px solid #f0f0f2;
}

.ap-weak__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.ap-weak__item {
  padding: 9px 11px;
  background-color: #f9fafb;
  border-color: #f0f0f2;
  border-radius: 10px;
}

.ap-weak__item:hover {
  background-color: #f3f5f8;
  border-color: #e5e7eb;
}

.ap-weak__name {
  color: #191b1f;
  font-size: 12.5px;
}

.ap-weak__rate {
  color: #2563eb;
  font-size: 12px;
}

.ap-weak__bar {
  height: 5px;
  background-color: #eef0f4;
}

.ap-weak__bar i {
  background-color: #2563eb;
  background-image: none;
}

.ap-weak__count {
  color: #9ca3af;
  font-size: 11px;
}

/* ---------- 建议问法：三张轻量卡片 ---------- */
.ap-recs {
  padding-top: 0;
  border-top: none;
}

.ap-recs__grid {
  gap: 10px;
}

.ap-recs--compact .ap-recs__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.ap-recs--compact .ap-rec {
  align-items: center;
  min-height: 86px;
  padding: 14px;
  color: #191b1f;
  font-size: 13.5px;
  background-color: #fff;
  background-image: none;
  border-radius: 14px;
}

.ap-recs--compact .ap-rec__text {
  overflow: visible;
  white-space: normal;
  text-overflow: clip;
}

.ap-rec {
  background: #fff;
  border-color: #ececef;
  box-shadow: none;
}

.ap-rec::before {
  width: 5px;
  height: 5px;
  background: #2563eb;
  box-shadow: none;
}

.ap-recs--solo .ap-rec {
  flex-direction: row;
  align-items: center;
  min-height: 86px;
  background-color: #fff;
  background-image: none;
}

.ap-recs--solo .ap-rec__arrow {
  align-self: center;
}

.ap-recs--solo .ap-rec__text {
  font-size: 13.5px;
}

.ap-recs--question .ap-recs__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ap-recs--question .ap-rec {
  min-height: 52px;
}

.ap-rec:hover {
  color: #191b1f;
  background-color: #fcfcfd;
  border-color: #d9dce2;
  box-shadow: none;
  transform: none;
}

.ap-rec:hover .ap-rec__arrow {
  color: #2563eb;
  transform: none;
}

.ap-recs__tip {
  color: #9ca3af;
  font-size: 11.5px;
}

/* ---------- 消息：豆包式灰气泡 + 正文直排 ---------- */
.ap-msg {
  margin-bottom: 26px;
}

.ap-msg--user {
  margin-bottom: 30px;
}

.ap-user {
  max-width: min(78%, 560px);
  padding: 10px 14px;
  color: #191b1f;
  font-size: 15px;
  line-height: 1.7;
  background-color: #f2f3f5;
  background-image: none;
  border: none;
  border-radius: 18px;
  box-shadow: none;
}

.ap-assistant {
  gap: 0;
  padding: 0;
  background: none;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.ap-assistant::before {
  display: none;
}

.ap-assistant__label {
  align-self: flex-start;
  gap: 7px;
  padding: 0;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 12.5px;
  font-weight: 500;
  background: none;
  border: none;
  border-radius: 0;
}

.ap-assistant__body,
.ap-assistant__body :deep(.markdown-body) {
  font-size: 16px;
  line-height: 1.85;
}

.ap-retry {
  color: #2563eb;
  background: none;
}

.ap-retry:hover {
  background-color: #f2f3f5;
}

.ap-to-bottom {
  color: #4b5563;
  background-color: #fff;
  border-color: #e5e7eb;
  box-shadow: 0 10px 24px -16px rgb(16 24 40 / 24%);
  backdrop-filter: none;
}

.ap-scroll {
  scrollbar-color: #d9dbe0 transparent;
}

.ap-scroll::-webkit-scrollbar-thumb {
  background-color: #dadce2;
}

/* ---------- 输入区：豆包式浅灰大圆角 ---------- */
.ap-composer-wrap::before {
  background: linear-gradient(to bottom, rgb(255 255 255 / 0%), #fff);
}

.ap-composer {
  min-height: 108px;
  padding: 14px 16px 10px;
  background: #f7f7f8;
  background-image: none;
  border: 1px solid transparent;
  border-radius: 24px;
  box-shadow: none;
}

.ap-composer:focus-within {
  background: #fff;
  background-image: none;
  border-color: #e5e7eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 6%);
}

.ap-composer.is-disabled {
  background: #f7f7f8;
  background-image: none;
  border-color: transparent;
  box-shadow: none;
}

.ap-textarea {
  font-size: 15.5px;
  line-height: 1.65;
}

.ap-textarea::placeholder {
  color: #9ca3af;
}

.ap-composer__bar {
  min-height: 36px;
}

.ap-composer :deep(.inline-select__trigger--lg) {
  min-height: 32px;
  padding: 0 8px;
  color: #4b5563;
  font-size: 13px;
  background-color: transparent;
  border-color: transparent;
  border-radius: 8px;
}

.ap-composer :deep(.inline-select__trigger--lg:hover) {
  color: #191b1f;
  background-color: #eceef1;
  border-color: transparent;
}

.ap-composer :deep(.inline-select__trigger--lg.is-open) {
  color: #191b1f;
  background-color: #fff;
  border-color: #e5e7eb;
  box-shadow: none;
}

.ap-composer :deep(.inline-select__icon) {
  color: #6b7280;
}

.ap-composer :deep(.inline-select__panel) {
  border-color: #ececef;
  border-radius: 12px;
  box-shadow: 0 14px 32px -18px rgb(16 24 40 / 24%);
}

.ap-quota {
  gap: 5px;
  padding: 0;
  color: #9ca3af;
  font-size: 11.5px;
  background: none;
  border: none;
}

.ap-quota > i {
  width: 5px;
  height: 5px;
  background-color: #22c55e;
  box-shadow: none;
}

.ap-quota.is-empty > i {
  background-color: #dc2626;
  box-shadow: none;
}

.ap-send {
  width: 36px;
  height: 36px;
  color: #fff;
  background-color: #2563eb;
  background-image: none;
  box-shadow: none;
}

.ap-send:hover:not(:disabled) {
  background-color: #1d4ed8;
  background-image: none;
  box-shadow: none;
  transform: none;
}

.ap-send:disabled {
  color: #9ca3af;
  background-color: #e5e7eb;
  background-image: none;
  box-shadow: none;
}

.ap-send--stop {
  color: #fff;
  background-color: #111827;
  background-image: none;
  box-shadow: none;
}

.ap-send--stop:hover:not(:disabled) {
  background-color: #000;
}

.ap-footnote {
  padding: 0 4px;
  margin-top: 8px;
  color: #9ca3af;
  font-size: 11px;
}

.ap-kbd {
  min-width: 0;
  height: auto;
  padding: 0;
  color: #9ca3af;
  font-family: inherit;
  font-size: 11px;
  background: none;
  border: none;
}

.ap-kbd__plus,
.ap-footnote__sep {
  color: #c4c8cf;
}

/* ---------- 响应式 ---------- */
@media (max-width: 767px) {
  .ap-topbar {
    height: 52px;
    padding: 0 12px;
  }

  .ap-empty {
    gap: 14px;
    padding: 6px 0 14px;
  }

  .ap-mark--lg {
    width: 50px;
    height: 50px;
    font-size: 22px;
    border-radius: 15px;
  }

  .ap-mark--lg::after {
    inset: -5px;
    border-radius: 19px;
  }

  .ap-empty__eyebrow {
    margin-top: 10px;
    font-size: 11.5px;
  }

  .ap-empty__title {
    margin: 10px 0 6px;
    font-size: 24px;
  }

  .ap-empty__sub {
    font-size: 13.5px;
    line-height: 1.7;
  }

  .ap-drill__grid {
    grid-template-columns: 1fr;
  }

  .ap-weak__list {
    grid-template-columns: 1fr;
  }

  .ap-recs--compact .ap-recs__grid,
  .ap-recs--solo .ap-recs__grid,
  .ap-recs--question .ap-recs__grid {
    grid-template-columns: 1fr;
  }

  .ap-recs--compact .ap-rec,
  .ap-recs--solo .ap-rec {
    min-height: 64px;
  }

  .ap-user {
    max-width: 88%;
  }

  .ap-composer {
    min-height: 112px;
    padding: 10px 12px 8px;
    border-radius: 20px;
  }

  .ap-textarea {
    font-size: 16px;
  }

  .ap-send {
    width: 40px;
    height: 40px;
  }

  .ap-composer :deep(.inline-select__trigger--lg) {
    min-height: 36px;
    padding: 0 9px;
  }

  .ap-footnote {
    margin-top: 6px;
  }
}

/* ============================================================================
   v8.0 输入区 / 消息区细化（豆包取向）
   ----------------------------------------------------------------------------
   - 输入区：工具项改为浅灰胶囊，静息态有淡边界，脚注只保留居中的 AI 提示；
   - 消息区：回到底部移到内容列右下，正文列表/引用改为中性灰，蓝色只留给链接；
   - 不改任何事件、状态与数据结构。
   ============================================================================ */

/* ---------- 工具项：浅灰胶囊 ---------- */
.ap-composer :deep(.inline-select__trigger--lg) {
  padding: 0 9px;
  color: #4b5563;
  font-size: 12.5px;
  background-color: #f1f2f5;
  border-color: transparent;
  border-radius: 8px;
}

.ap-composer :deep(.inline-select__trigger--lg:hover) {
  color: #191b1f;
  background-color: #e8e9ed;
  border-color: transparent;
}

.ap-composer :deep(.inline-select__trigger--lg.is-open) {
  color: #191b1f;
  background-color: #fff;
  border-color: #e5e7eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 5%);
}

.ap-composer :deep(.inline-select__icon) {
  color: #6b7280;
}

/* ---------- 输入框：静息淡边，聚焦柔和蓝边 ---------- */
.ap-composer {
  border-color: #ececef;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.ap-composer:focus-within {
  background: #fff;
  border-color: #cfd9ea;
  box-shadow:
    0 0 0 3px rgb(37 99 235 / 6%),
    0 8px 24px -20px rgb(16 24 40 / 30%);
}

.ap-composer.is-disabled {
  border-color: transparent;
  box-shadow: none;
}

/* ---------- 脚注：居中 AI 提示 ---------- */
.ap-footnote {
  gap: 6px;
  justify-content: center;
  padding: 0;
  color: #9ca3af;
}

.ap-footnote__hint {
  display: none;
}

.ap-footnote__meta {
  gap: 6px;
}

/* ---------- 回到底部：靠内容列右下，避免压住正文 ---------- */
.ap-to-bottom {
  left: auto;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  margin-right: calc(var(--ap-col-margin) + var(--ap-gutter) + 4px);
  margin-left: auto;
  border-radius: 50%;
}

.ap-to-bottom__text {
  display: none;
}

/* ---------- 正文：中性列表与引用 ---------- */
.ap-assistant__body :deep(li)::marker {
  color: #c0c4cc;
}

.ap-assistant__body :deep(ol) > li::marker {
  color: #6b7280;
}

.ap-assistant__body :deep(blockquote) {
  color: #5f636b;
  background-color: #f7f8fa;
  border-left-color: #e0e1e5;
  border-radius: 0 8px 8px 0;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1023px) {
  .ap-to-bottom {
    margin-right: 24px;
  }
}

@media (max-width: 767px) {
  .ap-to-bottom {
    margin-right: 20px;
  }
}

/* ============================================================================
   v9.0 右边界固定，向左加宽
   ----------------------------------------------------------------------------
   锚点 = 当前 860px 居中内容列的右边界。桌面侧栏 264px，原右留白为
   (100vw - 264 - 860) / 2；新宽度从 860 向左侧扩展，宽度封顶 1000px：
     --ap-column      = min(1000px, 50vw + 266px)
     --ap-col-margin  = max(16px, 50vw - 718px)   // 内容左留白 32px 起
   封顶后左留白继续增大，右边界保持不变；<1200px 左侧已无余量，沿用原布局。
   ============================================================================ */
@media (min-width: 1200px) {
  .ap {
    --ap-column: min(1000px, calc(50vw + 266px));
    --ap-col-margin: max(16px, calc(50vw - 718px));
  }
}

/* 回到底部：优先停在内容列右侧留白里，空间不足时贴近滚动区右缘 */
.ap-to-bottom {
  margin-right: max(
    16px,
    calc(100% - var(--ap-col-margin) - var(--ap-column) - var(--ap-gutter) - 42px)
  );
  margin-left: auto;
}

/* ============================================================================
   v10.0 字号适度上调
   ----------------------------------------------------------------------------
   整体 +0.5 ~ 1px：侧栏、面板标题、建议卡、消息正文与输入区同步放大，
   保持层级比例不变；移动端单独收一档，避免 320–390px 宽度下换行过多。
   ============================================================================ */

/* 侧栏 */
.ap-sidebar__brand-name {
  font-size: 15.5px;
}

.ap-sidebar__brand-sub {
  font-size: 11.5px;
}

.ap-new-chat {
  font-size: 14px;
}

.ap-sidebar__label {
  font-size: 11.5px;
}

.ap-sidebar__count {
  font-size: 11.5px;
}

.ap-conv__title {
  font-size: 14px;
}

.ap-conv__meta {
  font-size: 11.5px;
}

.ap-sidebar__link-title {
  font-size: 13.5px;
}

/* 顶栏与上下文 */
.ap-title {
  font-size: 14.5px;
}

.ap-title-badge {
  font-size: 12px;
}

.ap-context__text {
  font-size: 14px;
}

/* 空态 */
.ap-empty__eyebrow {
  font-size: 12.5px;
}

.ap-empty__title {
  font-size: clamp(28px, 2.6vw, 34px);
}

.ap-empty__sub {
  font-size: 15px;
}

/* 区块标题 */
.ap-panel__title-text {
  font-size: 14.5px;
}

.ap-panel__hint {
  font-size: 12px;
}

.ap-panel__badge {
  font-size: 12px;
}

/* 待复习 / 薄弱分类 / 建议卡 */
.ap-drill__cat {
  font-size: 11.5px;
}

.ap-drill__title {
  font-size: 14.5px;
}

.ap-drill__assess-label {
  font-size: 11.5px;
}

.ap-drill__go {
  font-size: 12px;
}

.ap-weak__name {
  font-size: 13px;
}

.ap-weak__rate {
  font-size: 12.5px;
}

.ap-weak__count {
  font-size: 11.5px;
}

.ap-rec {
  font-size: 13.5px;
}

.ap-recs--compact .ap-rec {
  font-size: 14px;
}

.ap-recs--solo .ap-rec {
  font-size: 14px;
}

.ap-recs--solo .ap-rec__text {
  font-size: 14px;
}

.ap-recs__tip {
  font-size: 12px;
}

/* 消息 */
.ap-user {
  font-size: 16px;
}

.ap-assistant__label {
  font-size: 13px;
}

.ap-assistant__body,
.ap-assistant__body :deep(.markdown-body) {
  font-size: 17px;
  line-height: 1.8;
}

.ap-assistant__body :deep(h1),
.ap-assistant__body :deep(h2) {
  font-size: 21px;
}

.ap-assistant__body :deep(h3) {
  font-size: 18.5px;
}

.ap-assistant__body :deep(h4),
.ap-assistant__body :deep(h5),
.ap-assistant__body :deep(h6) {
  font-size: 17.5px;
}

.ap-assistant__body :deep(pre code) {
  font-size: 14px;
}

.ap-assistant__body :deep(table) {
  font-size: 14.5px;
}

.ap-assistant__body :deep(th) {
  font-size: 13.5px;
}

/* 输入区 */
.ap-textarea {
  font-size: 16px;
}

.ap-composer :deep(.inline-select__trigger--lg) {
  font-size: 13px;
}

.ap-quota {
  font-size: 12px;
}

.ap-footnote {
  font-size: 11.5px;
}

.ap-notice {
  font-size: 13px;
}

.ap-blocked {
  font-size: 13.5px;
}

/* 移动端收一档，避免换行过密 */
@media (max-width: 767px) {
  .ap-empty__eyebrow {
    font-size: 12px;
  }

  .ap-empty__title {
    font-size: 26px;
  }

  .ap-empty__sub {
    font-size: 14px;
  }

  .ap-assistant__body,
  .ap-assistant__body :deep(.markdown-body) {
    font-size: 16.5px;
  }
}

</style>
