<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchCard from '@/components/SearchCard.vue'
import { createBorrow, deleteBorrow, listBorrows, returnBook } from '@/api/borrow'
import { listBooks } from '@/api/book'
import { listReaders } from '@/api/reader'
import type { Book, BorrowRecord, Reader } from '@/api/types'
import { BORROW_STATUS_OPTIONS, borrowStatusLabel, borrowStatusTagType } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

interface BorrowFormModel {
  readerId: number
  bookId: number
  shouldReturnTime: string
}

const auth = useAuthStore()
const isSuperAdmin = computed(() => auth.isSuperAdmin)
const canOperate = computed(
  () => auth.hasPermission('borrow:edit') || auth.hasPermission('borrow:remove'),
)

const loading = ref(false)
const rows = ref<BorrowRecord[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | ''>('')

async function loadBorrows(): Promise<void> {
  loading.value = true
  try {
    const page = await listBorrows({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
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
  void loadBorrows()
}

function resetSearch(): void {
  keyword.value = ''
  statusFilter.value = ''
  pageNum.value = 1
  void loadBorrows()
}

function handleSizeChange(): void {
  pageNum.value = 1
  void loadBorrows()
}

/* ========== 下拉选项 ========== */
const bookOptions = ref<Book[]>([])
const readerOptions = ref<Reader[]>([])

async function loadOptions(): Promise<void> {
  try {
    const page = await listBooks({ pageNum: 1, pageSize: 200 })
    bookOptions.value = page.items
  } catch {
    bookOptions.value = []
  }

  if (!auth.hasPermission('reader:query')) {
    readerOptions.value = []
    return
  }
  try {
    const page = await listReaders({ pageNum: 1, pageSize: 200 })
    readerOptions.value = page.items
  } catch {
    readerOptions.value = []
  }
}

onMounted(() => {
  void loadBorrows()
  void loadOptions()
})

/* ========== 新增借阅 ========== */
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<BorrowFormModel>({
  readerId: 0,
  bookId: 0,
  shouldReturnTime: '',
})

function disabledDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

const rules = computed<FormRules<BorrowFormModel>>(() => ({
  // 读者借书由服务端绑定本人档案，不需要选择读者
  readerId: isSuperAdmin.value
    ? [
        {
          validator: (_rule, value, callback) => {
            if (typeof value === 'number' && value > 0) {
              callback()
              return
            }
            callback(new Error('请选择读者'))
          },
          trigger: 'change',
        },
      ]
    : [],
  bookId: [
    {
      validator: (_rule, value, callback) => {
        if (typeof value === 'number' && value > 0) {
          callback()
          return
        }
        callback(new Error('请选择图书'))
      },
      trigger: 'change',
    },
  ],
  shouldReturnTime: [
    {
      // 允许留空，由服务端按默认借期生成；填写时必须是未来时间
      validator: (_rule, value, callback) => {
        if (value && new Date(value).getTime() <= Date.now()) {
          callback(new Error('应还时间必须晚于当前时间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}))

function openCreate(): void {
  Object.assign(form, {
    readerId: isSuperAdmin.value ? (readerOptions.value[0]?.readerId ?? 0) : 0,
    bookId: 0,
    shouldReturnTime: '',
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
    await createBorrow({
      // 读者不传 readerId 与应还时间，服务端强制本人档案与默认借期
      readerId: isSuperAdmin.value ? form.readerId : undefined,
      bookId: form.bookId,
      shouldReturnTime: isSuperAdmin.value
        ? form.shouldReturnTime || undefined
        : undefined,
    })
    ElMessage.success('新增借阅成功')
    dialogVisible.value = false
    await loadBorrows()
    await loadOptions()
  } catch {
    // 请求层已统一提示
  } finally {
    dialogLoading.value = false
  }
}

/* ========== 归还 / 删除 ========== */
async function handleReturn(row: BorrowRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认归还《${row.bookName}》？`,
    '归还确认',
    { type: 'warning', confirmButtonText: '归还', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await returnBook(row.borrowId)
    ElMessage.success('归还成功')
    await loadBorrows()
    await loadOptions()
  } catch {
    // 请求层已统一提示
  }
}

async function removeBorrow(row: BorrowRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除《${row.bookName}》的借阅记录？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteBorrow(row.borrowId)
    ElMessage.success('删除成功')
    await loadBorrows()
  } catch {
    // 请求层已统一提示
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="借阅记录" description="跟踪借出、归还与逾期状态">
      <el-button v-perm="'borrow:add'" type="primary" @click="openCreate">
        ＋ 新增借阅
      </el-button>
    </PageHeader>

    <SearchCard>
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="读者姓名 / 图书名称"
        clearable
        @keyup.enter="search"
      />
      <el-select v-model="statusFilter" class="status-select" placeholder="全部状态">
        <el-option label="全部状态" value="" />
        <el-option
          v-for="option in BORROW_STATUS_OPTIONS"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchCard>

    <section class="card table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无借阅记录">
        <el-table-column prop="readerName" label="读者" min-width="110" />
        <el-table-column prop="bookName" label="图书" min-width="180" />
        <el-table-column label="借出时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.shouldReturnTime) }}</template>
        </el-table-column>
        <el-table-column label="实际归还" width="150">
          <template #default="{ row }">{{ formatDateTime(row.actualReturnTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="borrowStatusTagType(row.borrowStatus)" effect="light" round>
              {{ borrowStatusLabel(row.borrowStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="逾期" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.overdueDays > 0" type="danger" effect="plain" round>
              {{ row.overdueDays }} 天
            </el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canOperate" label="操作" width="140">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="row.borrowStatus !== 1"
                v-perm="'borrow:edit'"
                link
                type="primary"
                @click="handleReturn(row)"
              >
                归还
              </el-button>
              <el-button
                v-if="row.borrowStatus === 1"
                v-perm="'borrow:remove'"
                link
                type="danger"
                @click="removeBorrow(row)"
              >
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
          @current-change="() => loadBorrows()"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="新增借阅" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item v-if="isSuperAdmin" label="读者" prop="readerId">
          <el-select
            v-if="readerOptions.length > 0"
            v-model="form.readerId"
            filterable
            placeholder="选择读者"
          >
            <el-option
              v-for="reader in readerOptions"
              :key="reader.readerId"
              :label="`${reader.readerName}（${reader.studentId}）`"
              :value="reader.readerId"
            />
          </el-select>
          <el-input-number
            v-else
            v-model="form.readerId"
            :min="1"
            :controls="false"
            placeholder="读者ID"
          />
        </el-form-item>
        <p v-if="isSuperAdmin && readerOptions.length === 0" class="form-hint">
          读者账号请联系管理员获取读者 ID。
        </p>
        <p v-if="!isSuperAdmin" class="form-hint">
          读者账号借书时按本人读者档案办理，无需选择读者。
        </p>

        <el-form-item label="图书" prop="bookId">
          <el-select v-model="form.bookId" filterable placeholder="选择图书">
            <el-option
              v-for="book in bookOptions"
              :key="book.bookId"
              :label="`${book.bookName}（可借 ${book.availableNum}）`"
              :value="book.bookId"
              :disabled="book.availableNum <= 0"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="isSuperAdmin" label="应还时间" prop="shouldReturnTime">
          <el-date-picker
            v-model="form.shouldReturnTime"
            type="datetime"
            placeholder="留空则默认借期 30 天"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <p v-if="isSuperAdmin" class="form-hint">
          不填写时由服务端按默认借期（30 天）生成。
        </p>
        <p v-else class="form-hint">
          读者借书默认借期 30 天，不能自行修改应还时间。
        </p>
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
  width: 240px;
}

.status-select {
  width: 140px;
}

.form-hint {
  margin: -10px 0 16px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.muted {
  color: var(--color-text-muted);
}
</style>
