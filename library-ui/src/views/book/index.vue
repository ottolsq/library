<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchCard from '@/components/SearchCard.vue'
import { createBook, deleteBook, listBooks, updateBook } from '@/api/book'
import type { Book, BookPayload } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

interface BookFormModel {
  isbn: string
  bookName: string
  author: string
  publisher: string
  category: string
  totalNum: number
  availableNum: number
}

const auth = useAuthStore()
const canOperate = computed(
  () => auth.hasPermission('book:edit') || auth.hasPermission('book:remove'),
)

const loading = ref(false)
const rows = ref<Book[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadBooks(): Promise<void> {
  loading.value = true
  try {
    const page = await listBooks({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
    })
    rows.value = page.items
    total.value = page.total
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search(): void {
  pageNum.value = 1
  void loadBooks()
}

function resetSearch(): void {
  keyword.value = ''
  pageNum.value = 1
  void loadBooks()
}

function handleSizeChange(): void {
  pageNum.value = 1
  void loadBooks()
}

function stockTagType(available: number): 'success' | 'warning' | 'danger' {
  if (available <= 0) {
    return 'danger'
  }
  return available <= 2 ? 'warning' : 'success'
}

onMounted(() => {
  void loadBooks()
})

/* ========== 新增 / 编辑 ========== */
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增图书' : '编辑图书'))

const form = reactive<BookFormModel>({
  isbn: '',
  bookName: '',
  author: '',
  publisher: '',
  category: '',
  totalNum: 1,
  availableNum: 1,
})

const rules: FormRules<BookFormModel> = {
  isbn: [
    { required: true, whitespace: true, message: '请输入 ISBN', trigger: 'blur' },
    { max: 30, message: 'ISBN 长度不能超过 30 位', trigger: 'blur' },
  ],
  bookName: [
    { required: true, whitespace: true, message: '请输入图书名称', trigger: 'blur' },
    { max: 200, message: '图书名称不能超过 200 字', trigger: 'blur' },
  ],
  author: [
    { required: true, whitespace: true, message: '请输入作者', trigger: 'blur' },
    { max: 100, message: '作者不能超过 100 字', trigger: 'blur' },
  ],
  publisher: [{ max: 100, message: '出版社不能超过 100 字', trigger: 'blur' }],
  category: [{ max: 50, message: '分类不能超过 50 字', trigger: 'blur' }],
  totalNum: [
    { required: true, message: '请输入总册数', trigger: 'change' },
    { type: 'number', min: 0, message: '总册数不能为负数', trigger: 'change' },
  ],
  availableNum: [
    { required: true, message: '请输入可借库存', trigger: 'change' },
    { type: 'number', min: 0, message: '可借库存不能为负数', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (typeof value === 'number' && value > form.totalNum) {
          callback(new Error('可借库存不能大于总册数'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    isbn: '',
    bookName: '',
    author: '',
    publisher: '',
    category: '',
    totalNum: 1,
    availableNum: 1,
  })
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: Book): void {
  dialogMode.value = 'edit'
  editingId.value = row.bookId
  Object.assign(form, {
    isbn: row.isbn,
    bookName: row.bookName,
    author: row.author,
    publisher: row.publisher ?? '',
    category: row.category ?? '',
    totalNum: row.totalNum,
    availableNum: row.availableNum,
  })
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function submitForm(): Promise<void> {
  if (!formRef.value || dialogLoading.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  dialogLoading.value = true
  try {
    const payload: BookPayload = {
      isbn: form.isbn.trim(),
      bookName: form.bookName.trim(),
      author: form.author.trim(),
      publisher: form.publisher.trim(),
      category: form.category.trim(),
      totalNum: form.totalNum,
      availableNum: form.availableNum,
    }
    if (dialogMode.value === 'create') {
      await createBook(payload)
    } else if (editingId.value !== null) {
      await updateBook(editingId.value, payload)
    }
    ElMessage.success(dialogMode.value === 'create' ? '新增图书成功' : '修改图书成功')
    dialogVisible.value = false
    await loadBooks()
  } catch {
    // 请求层已统一提示
  } finally {
    dialogLoading.value = false
  }
}

async function removeBook(row: Book): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除《${row.bookName}》？该操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteBook(row.bookId)
    ElMessage.success('删除成功')
    await loadBooks()
  } catch {
    // 请求层已统一提示
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="图书列表" description="管理馆藏图书的基础信息与可借库存">
      <el-button v-perm="'book:add'" type="primary" @click="openCreate">
        ＋ 新增图书
      </el-button>
    </PageHeader>

    <SearchCard>
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="书名 / 作者 / ISBN"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchCard>

    <section class="card table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无图书数据">
        <el-table-column prop="isbn" label="ISBN" min-width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.isbn }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bookName" label="书名" min-width="200" />
        <el-table-column prop="author" label="作者" min-width="150" />
        <el-table-column prop="publisher" label="出版社" min-width="140" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="totalNum" label="总册数" width="90" />
        <el-table-column label="可借" width="90">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row.availableNum)" effect="light" round>
              {{ row.availableNum }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canOperate" label="操作" width="150">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-perm="'book:edit'" link type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-perm="'book:remove'" link type="danger" @click="removeBook(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="() => loadBooks()"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="图书唯一标识" maxlength="30" />
        </el-form-item>
        <el-form-item label="书名" prop="bookName">
          <el-input v-model="form.bookName" placeholder="不超过 200 字" maxlength="200" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="不超过 100 字" maxlength="100" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="出版社" prop="publisher">
            <el-input v-model="form.publisher" maxlength="100" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-input v-model="form.category" maxlength="50" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="总册数" prop="totalNum">
            <el-input-number v-model="form.totalNum" :min="0" :max="9999" />
          </el-form-item>
          <el-form-item label="可借库存" prop="availableNum">
            <el-input-number v-model="form.availableNum" :min="0" :max="9999" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="submitForm">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-input {
  width: 260px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.mono {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
