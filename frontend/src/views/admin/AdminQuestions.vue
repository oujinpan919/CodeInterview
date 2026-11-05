<template>
  <section ref="topRef" class="admin-questions">
    <el-breadcrumb separator="/" class="admin-questions__breadcrumb">
      <el-breadcrumb-item>管理端</el-breadcrumb-item>
      <el-breadcrumb-item>题目管理</el-breadcrumb-item>
    </el-breadcrumb>

    <header class="admin-questions__head">
      <h1 class="admin-questions__title">题目管理</h1>
      <div class="admin-questions__head-actions">
        <el-button class="admin-questions__import" @click="importVisible = true">
          批量导入
        </el-button>
        <el-button
          class="admin-questions__create"
          type="primary"
          @click="goCreate"
        >
          新建题目
        </el-button>
      </div>
    </header>

    <section class="admin-questions__filters">
      <el-radio-group v-model="statusFilter" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="0">草稿</el-radio-button>
        <el-radio-button value="1">发布</el-radio-button>
        <el-radio-button value="2">下架</el-radio-button>
      </el-radio-group>
      <el-select
        v-model="categoryFilter"
        class="admin-questions__category"
        placeholder="全部分类"
        clearable
        @change="handleFilterChange"
      >
        <el-option
          v-for="category in categories"
          :key="category.id"
          :label="category.status === 1 ? category.name : `${category.name}（已停用）`"
          :value="category.id"
        />
      </el-select>
      <el-input
        v-model="searchInput"
        class="admin-questions__search"
        placeholder="搜索标题"
        maxlength="30"
        clearable
      />
    </section>

    <div v-if="loading && items.length === 0" class="admin-questions__skeleton" aria-busy="true">
      <el-skeleton :rows="5" animated />
    </div>

    <ErrorState
      v-else-if="loadError && items.length === 0"
      :message="loadError"
      @retry="loadList"
    />

    <EmptyState
      v-else-if="items.length === 0"
      title="还没有题目"
      description="新建一道题，保存草稿或直接发布"
      action-text="新建题目"
      @action="goCreate"
    />

    <template v-else>
      <el-table
        v-if="!isMobile"
        :data="items"
        class="admin-questions__table"
        row-key="id"
        :show-overflow-tooltip="true"
      >
        <el-table-column prop="title" label="标题" min-width="280" />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <DifficultyTag :value="row.difficulty" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="admin-questions__actions">
              <el-button class="admin-questions__action" link type="primary" @click="goEdit(row)">
                编辑
              </el-button>
              <el-button
                v-if="row.status !== 1"
                class="admin-questions__action"
                link
                type="success"
                :loading="actingId === row.id"
                @click="publishRow(row)"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 1"
                class="admin-questions__action"
                link
                type="warning"
                :loading="actingId === row.id"
                @click="offlineRow(row)"
              >
                下架
              </el-button>
              <el-button
                class="admin-questions__action"
                link
                type="danger"
                :loading="actingId === row.id"
                @click="deleteRow(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <ul v-else class="admin-questions__cards">
        <li v-for="row in items" :key="row.id" class="admin-questions__card">
          <p class="admin-questions__card-title">{{ row.title }}</p>
          <div class="admin-questions__card-meta">
            <DifficultyTag :value="row.difficulty" />
            <el-tag :type="statusType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
            <span class="admin-questions__card-text">{{ row.categoryName }}</span>
          </div>
          <p class="admin-questions__card-time">{{ row.updatedAt }}</p>
          <div class="admin-questions__card-actions">
            <el-button class="admin-questions__action" @click="goEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 1"
              class="admin-questions__action"
              type="success"
              plain
              :loading="actingId === row.id"
              @click="publishRow(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 1"
              class="admin-questions__action"
              type="warning"
              plain
              :loading="actingId === row.id"
              @click="offlineRow(row)"
            >
              下架
            </el-button>
            <el-button
              class="admin-questions__action"
              type="danger"
              plain
              :loading="actingId === row.id"
              @click="deleteRow(row)"
            >
              删除
            </el-button>
          </div>
        </li>
      </ul>

      <PageBar :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
    </template>

    <JsonImportDialog v-model="importVisible" @success="handleImportSuccess" />
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDebounceFn, useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  deleteAdminQuestion,
  getAdminCategories,
  getAdminQuestions,
  offlineAdminQuestion,
  publishAdminQuestion
} from '@/api/admin'
import DifficultyTag from '@/components/DifficultyTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'
import JsonImportDialog from '@/components/admin/JsonImportDialog.vue'

const PAGE_SIZE = 10
const STATUS_LABELS = { 0: '草稿', 1: '发布', 2: '下架' }
const STATUS_TYPES = { 0: 'info', 1: 'success', 2: 'warning' }

const router = useRouter()
const isMobile = useMediaQuery('(max-width: 767px)')
const topRef = ref(null)

const items = ref([])
const total = ref(0)
const page = ref(1)
const statusFilter = ref('')
const categoryFilter = ref('')
const searchInput = ref('')
const categories = ref([])
const loading = ref(false)
const loadError = ref('')
const actingId = ref('')
const importVisible = ref(false)

let loadSeq = 0

function statusLabel(value) {
  return STATUS_LABELS[value] || '—'
}

function statusType(value) {
  return STATUS_TYPES[value] || 'info'
}

async function loadList() {
  const seq = ++loadSeq
  loading.value = true
  const params = { page: page.value, pageSize: PAGE_SIZE }
  if (statusFilter.value !== '') params.status = statusFilter.value
  if (categoryFilter.value) params.categoryId = categoryFilter.value
  if (searchInput.value.trim()) params.keyword = searchInput.value.trim()
  try {
    const data = await getAdminQuestions(params)
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

async function loadCategories() {
  try {
    // 管理端分类接口含停用分类（04 A3.9 #35），筛旧题时也能看到
    const data = await getAdminCategories()
    categories.value = Array.isArray(data) ? data : []
  } catch {
    categories.value = []
  }
}

function handleImportSuccess() {
  loadList()
  loadCategories()
}

function handleFilterChange() {
  page.value = 1
  loadList()
}

const commitSearch = useDebounceFn(() => {
  page.value = 1
  loadList()
}, 300)

watch(searchInput, () => {
  commitSearch()
})

function handlePageChange(next) {
  page.value = next
  loadList()
  // 04 B5.2：切页后滚动到内容区顶部（scrollIntoView）
  if (topRef.value) topRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goCreate() {
  router.push({ name: 'admin-question-edit' })
}

function goEdit(row) {
  router.push({ name: 'admin-question-edit', query: { id: row.id } })
}

async function confirmAction(message, title, confirmText) {
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: confirmText,
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}

async function publishRow(row) {
  if (actingId.value) return
  const ok = await confirmAction('发布后前台立即可见，确定发布吗？', '发布题目', '发布')
  if (!ok) return
  actingId.value = row.id
  try {
    await publishAdminQuestion(row.id)
    row.status = 1
    ElMessage.success('已发布，前台立即可见')
    loadList()
  } catch (error) {
    ElMessage.error((error && error.message) || '发布失败，请重试')
  } finally {
    actingId.value = ''
  }
}

async function offlineRow(row) {
  if (actingId.value) return
  const ok = await confirmAction(
    '下架后前台详情不可访问，收藏/错题列表中置灰显示，确定下架吗？',
    '下架题目',
    '下架'
  )
  if (!ok) return
  actingId.value = row.id
  try {
    await offlineAdminQuestion(row.id)
    row.status = 2
    ElMessage.success('已下架')
    loadList()
  } catch (error) {
    ElMessage.error((error && error.message) || '下架失败，请重试')
  } finally {
    actingId.value = ''
  }
}

async function deleteRow(row) {
  if (actingId.value) return
  const ok = await confirmAction(
    '删除后前台将不可见，已收藏用户也看不到此题。确定删除吗？',
    '删除题目',
    '删除'
  )
  if (!ok) return
  actingId.value = row.id
  try {
    await deleteAdminQuestion(row.id)
    items.value = items.value.filter((item) => item.id !== row.id)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已删除')
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      loadList()
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '删除失败，请重试')
  } finally {
    actingId.value = ''
  }
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>

<style scoped>
.admin-questions__breadcrumb {
  margin-bottom: var(--space-4);
}

.admin-questions__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.admin-questions__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.admin-questions__create {
  min-height: 44px;
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}

.admin-questions__import {
  min-height: 44px;
  margin-right: var(--space-2);
  border-radius: var(--radius-button);
}

.admin-questions__head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.admin-questions__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.admin-questions__category {
  width: 180px;
}

.admin-questions__search {
  width: 260px;
}

.admin-questions__skeleton {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.admin-questions__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.admin-questions__action {
  min-height: 44px;
  margin-left: 0;
}

.admin-questions__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.admin-questions__card {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.admin-questions__card-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.admin-questions__card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.admin-questions__card-time {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.admin-questions__card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.admin-questions__card-actions .admin-questions__action {
  flex: 1 1 0;
  min-width: 0;
  margin-left: 0;
}

@media (max-width: 767px) {
  /* B2：手机上只显示面包屑最后一级 */
  .admin-questions__breadcrumb :deep(.el-breadcrumb__item:first-child) {
    display: none;
  }

  .admin-questions__head-actions {
    width: 100%;
  }

  .admin-questions__head-actions .admin-questions__import,
  .admin-questions__head-actions .admin-questions__create {
    flex: 1 1 0;
    min-width: 0;
    margin: 0;
  }

  .admin-questions__filters {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: var(--space-3);
  }

  .admin-questions__category,
  .admin-questions__search {
    flex: 0 0 auto;
    width: 45vw;
  }

  .admin-questions__filters :deep(.el-radio-button__inner) {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
    padding: 0 var(--space-3);
  }
}
</style>
