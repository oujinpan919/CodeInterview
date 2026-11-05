<template>
  <section class="favorites">
    <header class="favorites__head">
      <h1 class="favorites__title">我的收藏</h1>
      <p class="favorites__count">共 {{ total }} 道</p>
    </header>

    <div v-if="showSkeleton" class="favorites__list" aria-busy="true" aria-label="收藏加载中">
      <div v-for="item in 4" :key="`skeleton-${item}`" class="favorites__skeleton">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="h3" class="favorites__skeleton-title" />
            <el-skeleton-item variant="text" class="favorites__skeleton-meta" />
          </template>
        </el-skeleton>
      </div>
    </div>

    <ErrorState
      v-else-if="loadError && items.length === 0"
      :message="loadError"
      @retry="loadFavorites"
    />

    <EmptyState
      v-else-if="items.length === 0"
      title="还没有收藏的题目"
      description="看到值得再看一眼的题，点收藏就会出现在这里"
      action-text="去题库逛逛"
      @action="goQuestionList"
    />

    <template v-else>
      <div class="favorites__list">
        <div v-for="item in items" :key="item.id" class="favorites__item">
          <QuestionCard :question="item" @click="goDetail" />
          <div class="favorites__actions">
            <span class="favorites__time">收藏于 {{ formatTime(item.favoritedAt) }}</span>
            <el-button
              class="favorites__remove"
              type="primary"
              link
              :loading="removingId === item.id"
              @click="removeFavorite(item)"
            >
              取消收藏
            </el-button>
          </div>
        </div>
      </div>

      <PageBar :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getFavorites, unfavoriteQuestion } from '@/api/question'
import QuestionCard from '@/components/QuestionCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'

const PAGE_SIZE = 10

const router = useRouter()

const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadError = ref('')
const removingId = ref('')

const showSkeleton = computed(() => loading.value && items.value.length === 0)

let loadSeq = 0

async function loadFavorites() {
  const seq = ++loadSeq
  loading.value = true
  try {
    const data = await getFavorites({ page: page.value, pageSize: PAGE_SIZE })
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

async function removeFavorite(item) {
  if (removingId.value) return
  removingId.value = item.id
  try {
    await unfavoriteQuestion(item.id)
    // 04 B4.6：成功后从列表移除并 total-1；当前页删空时回退一页重拉
    items.value = items.value.filter((row) => row.id !== item.id)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已取消收藏')
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadFavorites()
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '取消收藏失败，请重试')
  } finally {
    removingId.value = ''
  }
}

function handlePageChange(next) {
  page.value = next
  loadFavorites()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goDetail(question) {
  router.push({ name: 'question-detail', params: { id: question.id } })
}

function goQuestionList() {
  router.push('/question')
}

function formatTime(value) {
  if (!value) return ''
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : String(value)
}

onMounted(loadFavorites)
</script>

<style scoped>
.favorites__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.favorites__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.favorites__count {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.favorites__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.favorites__item {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.favorites__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0 var(--space-2);
}

.favorites__time {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.favorites__remove {
  min-height: 44px;
  font-size: var(--font-size-aux);
}

.favorites__skeleton {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.favorites__skeleton-title {
  width: 60%;
}

.favorites__skeleton-meta {
  width: 35%;
  margin-top: var(--space-3);
}

@media (max-width: 767px) {
  .favorites__head {
    margin-bottom: var(--space-4);
  }

  .favorites__skeleton {
    padding: var(--space-4);
  }
}
</style>
