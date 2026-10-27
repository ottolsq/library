<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchCard from '@/components/SearchCard.vue'
import { createReader, deleteReader, listReaders, updateReader } from '@/api/reader'
import { listUsers } from '@/api/user'
import type { Reader, ReaderPayload, UserListItem } from '@/api/types'
import { readerStatusLabel } from '@/utils/dict'

interface UserOption {
  userId: number
  username: string
}

interface ReaderFormModel {
  userId: number
  readerName: string
  studentId: string
  phone: string
  maxBorrowCnt: number
}

const loading = ref(false)
const rows = ref<Reader[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadReaders(): Promise<void> {
  loading.value = true
  try {
    const page = await listReaders({
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
  void loadReaders()
}

function resetSearch(): void {
  keyword.value = ''
  pageNum.value = 1
  void loadReaders()
}

function handleSizeChange(): void {
  pageNum.value = 1
  void loadReaders()
}

/* ========== 关联用户下拉 ========== */
/** 全部读者角色账号：编辑时用于回显已关联账号 */
const allReaderUsers = ref<UserOption[]>([])
/** 尚未关联读者档案的读者账号：新增时可选 */
const linkableUsers = ref<UserOption[]>([])

function toUserOptions(items: UserListItem[]): UserOption[] {
  return items.map((user) => ({ userId: user.userId, username: user.username }))
}

async function loadUserOptions(): Promise<void> {
  try {
    const [all, linkable] = await Promise.all([
      listUsers({ pageNum: 1, pageSize: 200, role: 1 }),
      listUsers({ pageNum: 1, pageSize: 200, role: 1, unlinked: true }),
    ])
    allReaderUsers.value = toUserOptions(all.items)
    linkableUsers.value = toUserOptions(
      linkable.items.filter((user) => user.status === 'ENABLED'),
    )
  } catch {
    allReaderUsers.value = []
    linkableUsers.value = []
  }
}

const selectableUsers = computed(() => {
  const source = dialogMode.value === 'create' ? linkableUsers.value : allReaderUsers.value
  if (form.userId > 0 && !source.some((user) => user.userId === form.userId)) {
    return [...source, { userId: form.userId, username: `用户 #${form.userId}` }]
  }
  return source
})

onMounted(() => {
  void loadReaders()
  void loadUserOptions()
})

/* ========== 新增 / 编辑 ========== */
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增读者' : '编辑读者'))

const form = reactive<ReaderFormModel>({
  userId: 0,
  readerName: '',
  studentId: '',
  phone: '',
  maxBorrowCnt: 5,
})

const rules: FormRules<ReaderFormModel> = {
  userId: [
    {
      validator: (_rule, value, callback) => {
        if (typeof value === 'number' && value > 0) {
          callback()
          return
        }
        callback(new Error('请选择关联用户'))
      },
      trigger: 'change',
    },
  ],
  readerName: [
    { required: true, whitespace: true, message: '请输入读者姓名', trigger: 'blur' },
    { max: 50, message: '姓名长度不能超过 50 字', trigger: 'blur' },
  ],
  studentId: [
    { required: true, whitespace: true, message: '请输入学号', trigger: 'blur' },
    { max: 30, message: '学号长度不能超过 30 位', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^$|^\d{5,20}$/, message: '联系电话应为 5–20 位数字', trigger: 'blur' },
  ],
  maxBorrowCnt: [
    { required: true, message: '请输入最大可借数量', trigger: 'change' },
    { type: 'number', min: 1, max: 99, message: '最大可借数量应为 1–99', trigger: 'change' },
  ],
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    userId: 0,
    readerName: '',
    studentId: '',
    phone: '',
    maxBorrowCnt: 5,
  })
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: Reader): void {
  dialogMode.value = 'edit'
  editingId.value = row.readerId
  Object.assign(form, {
    userId: row.userId,
    readerName: row.readerName,
    studentId: row.studentId,
    phone: row.phone ?? '',
    maxBorrowCnt: row.maxBorrowCnt,
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
    const payload: ReaderPayload = {
      userId: form.userId,
      readerName: form.readerName.trim(),
      studentId: form.studentId.trim(),
      phone: form.phone.trim() || undefined,
      maxBorrowCnt: form.maxBorrowCnt,
    }
    if (dialogMode.value === 'create') {
      await createReader(payload)
    } else if (editingId.value !== null) {
      await updateReader(editingId.value, payload)
    }
    ElMessage.success(dialogMode.value === 'create' ? '新增读者成功' : '修改读者成功')
    dialogVisible.value = false
    await Promise.all([loadReaders(), loadUserOptions()])
  } catch {
    // 请求层已统一提示
  } finally {
    dialogLoading.value = false
  }
}

async function removeReader(row: Reader): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除读者「${row.readerName}」？该操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteReader(row.readerId)
    ElMessage.success('删除成功')
    await Promise.all([loadReaders(), loadUserOptions()])
  } catch {
    // 请求层已统一提示
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="读者档案" description="维护读者基本信息与可借上限">
      <el-button v-perm="'reader:add'" type="primary" @click="openCreate">
        ＋ 新增读者
      </el-button>
    </PageHeader>

    <SearchCard>
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="姓名 / 学号 / 电话"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchCard>

    <section class="card table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无读者档案">
        <el-table-column prop="readerName" label="姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="140" />
        <el-table-column label="联系电话" min-width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="maxBorrowCnt" label="可借上限" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ readerStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-perm="'reader:edit'" link type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-perm="'reader:remove'" link type="danger" @click="removeReader(row)">
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
          @current-change="() => loadReaders()"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="关联用户" prop="userId">
          <el-select
            v-if="selectableUsers.length > 0"
            v-model="form.userId"
            filterable
            placeholder="选择系统用户"
          >
            <el-option
              v-for="user in selectableUsers"
              :key="user.userId"
              :label="`${user.username}（#${user.userId}）`"
              :value="user.userId"
            />
          </el-select>
          <el-input-number
            v-else
            v-model="form.userId"
            :min="1"
            :controls="false"
            placeholder="系统用户ID"
          />
        </el-form-item>
        <p v-if="dialogMode === 'create' && linkableUsers.length === 0" class="form-hint">
          暂无可关联的读者账号，请先在「用户管理」创建读者角色的系统账号。
        </p>
        <el-form-item label="读者姓名" prop="readerName">
          <el-input v-model="form.readerName" placeholder="不超过 50 字" maxlength="50" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="form.studentId" placeholder="业务唯一" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="5–20 位数字，可留空" maxlength="20" />
        </el-form-item>
        <el-form-item label="最大可借数量" prop="maxBorrowCnt">
          <el-input-number v-model="form.maxBorrowCnt" :min="1" :max="99" />
        </el-form-item>
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

.form-hint {
  margin: -10px 0 16px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
