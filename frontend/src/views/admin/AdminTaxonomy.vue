<template>
  <section ref="topRef" class="taxonomy">
    <el-breadcrumb separator="/" class="taxonomy__breadcrumb">
      <el-breadcrumb-item>管理端</el-breadcrumb-item>
      <el-breadcrumb-item>分类与标签</el-breadcrumb-item>
    </el-breadcrumb>

    <header class="taxonomy__head">
      <h1 class="taxonomy__title">分类与标签</h1>
    </header>

    <el-tabs v-if="isMobile" v-model="mobileTab" class="taxonomy__mobile-tabs">
      <el-tab-pane label="分类" name="categories" />
      <el-tab-pane label="标签" name="tags" />
    </el-tabs>

    <div class="taxonomy__layout" :class="{ 'taxonomy__layout--mobile': isMobile }">
      <section v-show="!isMobile || mobileTab === 'categories'" class="taxonomy__card">
        <header class="taxonomy__card-head">
          <h2 class="taxonomy__card-title">分类</h2>
          <p class="taxonomy__card-hint">被题目引用的分类不能删除，可改为停用</p>
        </header>

        <div class="taxonomy__add-row">
          <el-input
            v-model="categoryForm.name"
            class="taxonomy__add-name"
            placeholder="分类名（≤30 字）"
            maxlength="30"
          />
          <el-input
            v-model="categoryForm.description"
            class="taxonomy__add-desc"
            placeholder="说明（可选）"
            maxlength="200"
          />
          <el-input
            v-model="categoryForm.sort"
            class="taxonomy__add-sort"
            placeholder="排序"
            maxlength="4"
          />
          <el-button
            class="taxonomy__add-button"
            type="primary"
            :loading="creatingCategory"
            @click="createCategory"
          >
            新增分类
          </el-button>
        </div>

        <div v-if="loading" class="taxonomy__skeleton" aria-busy="true">
          <el-skeleton :rows="4" animated />
        </div>
        <ErrorState v-else-if="loadError" :message="loadError" @retry="loadAll" />
        <template v-else>
          <el-table :data="categories" class="taxonomy__table" row-key="id">
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <div class="taxonomy__status">
                  <el-switch
                    :model-value="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    :loading="actingId === row.id"
                    @change="(value) => toggleCategoryStatus(row, value)"
                  />
                  <el-tag v-if="row.status === 0" type="info" size="small" effect="plain">
                    已停用
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="题目数" width="130">
              <template #default="{ row }">
                <span>{{ row.questionCount }}</span>
                <span class="taxonomy__sub">（发布 {{ row.publishedCount }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div class="taxonomy__actions">
                  <el-button link type="primary" class="taxonomy__action" @click="openCategoryEdit(row)">
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    class="taxonomy__action"
                    :loading="actingId === row.id"
                    @click="deleteCategory(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="categories.length === 0" title="还没有分类" description="先新增一个分类，再录入题目" />
        </template>
      </section>

      <section v-show="!isMobile || mobileTab === 'tags'" class="taxonomy__card">
        <header class="taxonomy__card-head">
          <h2 class="taxonomy__card-title">标签</h2>
          <p class="taxonomy__card-hint">被题目引用的标签需先解除关联再删除</p>
        </header>

        <div class="taxonomy__add-row">
          <el-input
            v-model="tagForm.name"
            class="taxonomy__add-name"
            placeholder="标签名（≤20 字）"
            maxlength="20"
          />
          <el-button
            class="taxonomy__add-button"
            type="primary"
            :loading="creatingTag"
            @click="createTag"
          >
            新增标签
          </el-button>
        </div>

        <div v-if="loading" class="taxonomy__skeleton" aria-busy="true">
          <el-skeleton :rows="4" animated />
        </div>
        <ErrorState v-else-if="loadError" :message="loadError" @retry="loadAll" />
        <template v-else>
          <el-table :data="tags" class="taxonomy__table" row-key="id">
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="questionCount" label="题目数" width="100" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div class="taxonomy__actions">
                  <el-button link type="primary" class="taxonomy__action" @click="editTag(row)">
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    class="taxonomy__action"
                    :loading="actingId === row.id"
                    @click="deleteTag(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="tags.length === 0" title="还没有标签" description="先新增标签，编辑题目时可挂载" />
        </template>
      </section>
    </div>

    <el-dialog v-model="categoryDialogVisible" title="编辑分类" :width="isMobile ? '88%' : '420px'">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="categoryEdit.name" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="categoryEdit.description" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="categoryEdit.sort" maxlength="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="categoryEdit.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="taxonomy__dialog-footer">
          <el-button class="taxonomy__add-button" @click="categoryDialogVisible = false">取消</el-button>
          <el-button
            class="taxonomy__add-button"
            type="primary"
            :loading="savingCategory"
            @click="saveCategoryEdit"
          >
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  createAdminCategory,
  createAdminTag,
  deleteAdminCategory,
  deleteAdminTag,
  getAdminCategories,
  updateAdminCategory,
  updateAdminTag
} from '@/api/admin'
import { getTags } from '@/api/taxonomy'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'

const isMobile = useMediaQuery('(max-width: 767px)')
const mobileTab = ref('categories')
const topRef = ref(null)

const categories = ref([])
const tags = ref([])
const loading = ref(true)
const loadError = ref('')
const actingId = ref('')

const categoryForm = ref({ name: '', description: '', sort: '' })
const tagForm = ref({ name: '' })
const creatingCategory = ref(false)
const creatingTag = ref(false)

const categoryDialogVisible = ref(false)
const categoryEdit = ref({ id: null, name: '', description: '', sort: '', status: 1 })
const savingCategory = ref(false)

async function loadAll() {
  loading.value = true
  loadError.value = ''
  try {
    const [categoryList, tagList] = await Promise.all([getAdminCategories(), getTags()])
    categories.value = Array.isArray(categoryList) ? categoryList : []
    tags.value = Array.isArray(tagList) ? tagList : []
  } catch (error) {
    loadError.value = (error && error.message) || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function createCategory() {
  const name = categoryForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入分类名')
    return
  }
  creatingCategory.value = true
  try {
    const sortValue = categoryForm.value.sort.trim()
    await createAdminCategory({
      name,
      description: categoryForm.value.description.trim(),
      sort: /^\d+$/.test(sortValue) ? sortValue * 1 : 0,
      status: 1
    })
    ElMessage.success('分类已创建')
    categoryForm.value = { name: '', description: '', sort: '' }
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '创建失败，请重试')
  } finally {
    creatingCategory.value = false
  }
}

async function toggleCategoryStatus(row, value) {
  if (actingId.value) return
  actingId.value = row.id
  try {
    await updateAdminCategory(row.id, {
      name: row.name,
      description: row.description,
      sort: row.sort,
      status: value
    })
    row.status = value
    ElMessage.success(value === 1 ? '分类已启用' : '分类已停用')
  } catch (error) {
    ElMessage.error((error && error.message) || '更新失败，请重试')
    await loadAll()
  } finally {
    actingId.value = ''
  }
}

function openCategoryEdit(row) {
  categoryEdit.value = {
    id: row.id,
    name: row.name,
    description: row.description,
    sort: String(row.sort),
    status: row.status
  }
  categoryDialogVisible.value = true
}

async function saveCategoryEdit() {
  if (!categoryEdit.value.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  savingCategory.value = true
  try {
    const sortValue = String(categoryEdit.value.sort).trim()
    await updateAdminCategory(categoryEdit.value.id, {
      name: categoryEdit.value.name.trim(),
      description: categoryEdit.value.description,
      sort: /^\d+$/.test(sortValue) ? sortValue * 1 : 0,
      status: categoryEdit.value.status
    })
    categoryDialogVisible.value = false
    ElMessage.success('分类已保存')
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '保存失败，请重试')
  } finally {
    savingCategory.value = false
  }
}

async function deleteCategory(row) {
  if (actingId.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？该分类下有 ${row.questionCount} 道题时不可删除。`,
      '删除分类',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actingId.value = row.id
  try {
    await deleteAdminCategory(row.id)
    ElMessage.success('分类已删除')
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '删除失败，请重试')
  } finally {
    actingId.value = ''
  }
}

async function createTag() {
  const name = tagForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入标签名')
    return
  }
  creatingTag.value = true
  try {
    await createAdminTag({ name })
    ElMessage.success('标签已创建')
    tagForm.value.name = ''
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '创建失败，请重试')
  } finally {
    creatingTag.value = false
  }
}

async function editTag(row) {
  let value
  try {
    const result = await ElMessageBox.prompt('修改标签名', '编辑标签', {
      inputValue: row.name,
      inputValidator: (input) => {
        const text = (input || '').trim()
        if (!text) return '标签名不能为空'
        if (text.length > 20) return '标签名不能超过 20 字'
        return true
      },
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      customClass: 'taxonomy__prompt'
    })
    value = result.value.trim()
  } catch {
    return
  }
  try {
    await updateAdminTag(row.id, { name: value })
    ElMessage.success('标签已保存')
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '保存失败，请重试')
  }
}

async function deleteTag(row) {
  if (actingId.value) return
  try {
    await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '删除标签', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  actingId.value = row.id
  try {
    await deleteAdminTag(row.id)
    ElMessage.success('标签已删除')
    await loadAll()
  } catch (error) {
    ElMessage.error((error && error.message) || '删除失败，请重试')
  } finally {
    actingId.value = ''
  }
}

onMounted(loadAll)
</script>

<style scoped>
.taxonomy__breadcrumb {
  margin-bottom: var(--space-4);
}

.taxonomy__head {
  margin-bottom: var(--space-4);
}

.taxonomy__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.taxonomy__layout {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: var(--space-4);
  align-items: start;
}

.taxonomy__layout--mobile {
  grid-template-columns: minmax(0, 1fr);
}

.taxonomy__card {
  min-width: 0;
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.taxonomy__card-head {
  margin-bottom: var(--space-3);
}

.taxonomy__card-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.taxonomy__card-hint {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.taxonomy__add-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.taxonomy__add-name {
  flex: 1 1 160px;
}

.taxonomy__add-desc {
  flex: 2 1 200px;
}

.taxonomy__add-sort {
  flex: 0 0 90px;
}

.taxonomy__add-button {
  min-height: 40px;
  border-radius: var(--radius-button);
}

.taxonomy__skeleton {
  padding: var(--space-3) 0;
}

.taxonomy__sub {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.taxonomy__status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.taxonomy__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.taxonomy__action {
  min-height: 44px;
  margin-left: 0;
}

.taxonomy__dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.taxonomy__mobile-tabs {
  margin-bottom: var(--space-2);
}

@media (max-width: 767px) {
  .taxonomy__breadcrumb :deep(.el-breadcrumb__item:first-child) {
    display: none;
  }

  .taxonomy__card {
    padding: var(--space-4);
  }

  .taxonomy__add-row {
    flex-direction: column;
  }

  .taxonomy__add-sort {
    flex: 1 1 auto;
  }

  .taxonomy__add-button {
    min-height: 44px;
  }
}
</style>
