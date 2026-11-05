<template>
  <section class="wrong-book">
    <header class="wrong-book__head">
      <h1 class="wrong-book__title">错题本</h1>
      <p class="wrong-book__hint">自评「模糊」或「不会」的题会自动收进来，掌握后移出</p>
    </header>

    <section class="wrong-book__card">
      <el-tabs v-model="tab" class="wrong-book__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="未掌握" name="unresolved" />
        <el-tab-pane label="已掌握" name="mastered" />
      </el-tabs>

      <div v-if="loading && items.length === 0" class="wrong-book__skeleton" aria-busy="true">
        <el-skeleton :rows="4" animated />
      </div>

      <ErrorState
        v-else-if="loadError && items.length === 0"
        :message="loadError"
        @retry="loadList"
      />

      <EmptyState
        v-else-if="items.length === 0"
        :title="tab === 'unresolved' ? '错题本是空的，去刷几道题吧' : '还没有已掌握的错题'"
        :description="tab === 'unresolved' ? '自评「模糊」或「不会」后会出现在这里' : '把错题标记掌握后会归档到这里'"
        action-text="去刷题"
        @action="goQuestionList"
      />

      <template v-else>
        <el-table
          v-if="!isMobile"
          :data="items"
          class="wrong-book__table"
          row-key="questionId"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="题目" min-width="260">
            <template #default="{ row }">
              <RouterLink
                class="wrong-book__link"
                :to="{ name: 'question-detail', params: { id: row.questionId } }"
              >
                {{ row.title }}
              </RouterLink>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="难度" width="90">
            <template #default="{ row }">
              <DifficultyTag :value="row.difficulty" />
            </template>
          </el-table-column>
          <el-table-column label="自评" width="90">
            <template #default="{ row }">
              <el-tag :type="assessType(row.lastAssess)" size="small" effect="plain">
                {{ assessLabel(row.lastAssess) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="wrongCount" label="错次" width="80" />
          <el-table-column prop="lastWrongAt" label="最近答错" width="170" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="wrong-book__actions">
                <el-tooltip
                  :disabled="row.status !== 2"
                  content="题目已下架"
                  placement="top"
                >
                  <span>
                    <el-button
                      class="wrong-book__action"
                      type="primary"
                      link
                      :disabled="row.status === 2"
                      @click="goReview(row)"
                    >
                      去复习
                    </el-button>
                  </span>
                </el-tooltip>
                <el-button
                  v-if="row.resolved === 0"
                  class="wrong-book__action"
                  type="success"
                  link
                  :loading="actingId === row.questionId"
                  @click="markMastered(row)"
                >
                  标记掌握
                </el-button>
                <el-button
                  class="wrong-book__action wrong-book__action--danger"
                  type="danger"
                  link
                  :loading="actingId === row.questionId"
                  @click="removeItem(row)"
                >
                  彻底移除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <ul v-else class="wrong-book__cards">
          <li v-for="row in items" :key="row.questionId" class="wrong-book__item">
            <RouterLink
              class="wrong-book__item-title"
              :to="{ name: 'question-detail', params: { id: row.questionId } }"
            >
              {{ row.title }}
            </RouterLink>
            <div class="wrong-book__item-meta">
              <DifficultyTag :value="row.difficulty" />
              <el-tag :type="assessType(row.lastAssess)" size="small" effect="plain">
                {{ assessLabel(row.lastAssess) }}
              </el-tag>
              <span class="wrong-book__item-text">{{ row.categoryName }}</span>
              <span class="wrong-book__item-text">错 {{ row.wrongCount }} 次</span>
            </div>
            <p class="wrong-book__item-time">最近答错：{{ row.lastWrongAt }}</p>
            <div class="wrong-book__item-actions">
              <el-button
                class="wrong-book__action"
                type="primary"
                :disabled="row.status === 2"
                @click="goReview(row)"
              >
                去复习
              </el-button>
              <el-button
                v-if="row.resolved === 0"
                class="wrong-book__action"
                type="success"
                plain
                :loading="actingId === row.questionId"
                @click="markMastered(row)"
              >
                标记掌握
              </el-button>
              <el-button
                class="wrong-book__action"
                type="danger"
                plain
                :loading="actingId === row.questionId"
                @click="removeItem(row)"
              >
                彻底移除
              </el-button>
            </div>
            <p v-if="row.status === 2" class="wrong-book__offline">题目已下架，仅供查看</p>
          </li>
        </ul>

        <PageBar :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
      </template>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import { getWrongBook, markWrongResolved, removeWrong } from '@/api/record'
import DifficultyTag from '@/components/DifficultyTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10
const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }
const ASSESS_TYPES = { 1: 'success', 2: 'warning', 3: 'danger' }

const router = useRouter()
const isMobile = useMediaQuery('(max-width: 767px)')

const tab = ref('unresolved')
const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadError = ref('')
const actingId = ref('')

let loadSeq = 0

function assessLabel(value) {
  return ASSESS_LABELS[value] || '—'
}

function assessType(value) {
  return ASSESS_TYPES[value] || 'info'
}

async function loadList() {
  const seq = ++loadSeq
  loading.value = true
  try {
    const data = await getWrongBook({
      resolved: tab.value === 'mastered' ? 1 : 0,
      page: page.value,
      pageSize: PAGE_SIZE
    })
    if (seq !== loadSeq) return
    items.value = Array.isArray(data.list) ? data.list : []
    total.value = typeof data.total === 'number' ? data.total : 0
    loadError.value = ''
  } catch (error) {
    if (seq !== loadSeq) return
    if (items.value.length === 0) {
      loadError.value = (error && error.message) || '加载失败，请稍后重试'
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  loadList()
}

function handlePageChange(next) {
  page.value = next
  loadList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 去复习带 ?from=wrong，详情页据此传 source=3（04 B4.7 / 01 F04） */
function goReview(row) {
  if (row.status === 2) return
  router.push({ name: 'question-detail', params: { id: row.questionId }, query: { from: 'wrong' } })
}

async function markMastered(row) {
  if (actingId.value) return
  actingId.value = row.questionId
  try {
    await markWrongResolved(row.questionId)
    // 成功后从当前 tab 移除（04 B4.7）
    items.value = items.value.filter((item) => item.questionId !== row.questionId)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已标记为掌握')
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadList()
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    actingId.value = ''
  }
}

async function removeItem(row) {
  if (actingId.value) return
  try {
    await ElMessageBox.confirm('移除后该题的历史错题记录将不可恢复，确定彻底移除吗？', '彻底移除', {
      type: 'warning',
      confirmButtonText: '彻底移除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  actingId.value = row.questionId
  try {
    await removeWrong(row.questionId)
    items.value = items.value.filter((item) => item.questionId !== row.questionId)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已彻底移除')
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadList()
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    actingId.value = ''
  }
}

function goQuestionList() {
  router.push('/question')
}

onMounted(loadList)
</script>

<style scoped>
.wrong-book__head {
  margin-bottom: var(--space-6);
}

.wrong-book__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.wrong-book__hint {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__card {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.wrong-book__skeleton {
  padding: var(--space-4) 0;
}

.wrong-book__link {
  color: var(--color-primary);
  overflow-wrap: anywhere;
}

.wrong-book__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.wrong-book__action {
  min-height: 44px;
  margin-left: 0;
}

.wrong-book__action--danger {
  color: var(--color-danger);
}

.wrong-book__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.wrong-book__item {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.wrong-book__item-title {
  display: block;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.wrong-book__item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__item-time {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.wrong-book__item-actions .wrong-book__action {
  flex: 1 1 0;
  min-width: 0;
  margin-left: 0;
}

.wrong-book__offline {
  margin-top: var(--space-2);
  color: var(--color-warning);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

@media (max-width: 767px) {
  .wrong-book__head {
    margin-bottom: var(--space-4);
  }

  .wrong-book__card {
    padding: var(--space-4);
  }
}
</style>
