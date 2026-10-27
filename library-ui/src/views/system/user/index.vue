<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchCard from '@/components/SearchCard.vue'
import {
  createUser,
  deleteUser,
  listUsers,
  resetPassword,
  updateUser,
  updateUserStatus,
} from '@/api/user'
import type {
  UserCreatePayload,
  UserListItem,
  UserStatus,
  UserUpdatePayload,
} from '@/api/types'
import { ROLE_OPTIONS, USER_STATUS_OPTIONS, roleLabel, userStatusLabel } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

interface UserFormModel {
  username: string
  password: string
  role: number
  status: UserStatus
}

const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.userId ?? null)

const loading = ref(false)
const rows = ref<UserListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    const page = await listUsers({ pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = page.items
    total.value = page.total
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange(): void {
  pageNum.value = 1
  void loadUsers()
}

onMounted(() => {
  void loadUsers()
})

/* ========== 新增 / 编辑 ========== */
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增用户' : '编辑用户'))

const form = reactive<UserFormModel>({
  username: '',
  password: '',
  role: 1,
  status: 'ENABLED',
})

const rules = computed<FormRules<UserFormModel>>(() => ({
  username: [
    { required: true, whitespace: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度应为 3–32 位', trigger: 'blur' },
  ],
  password:
    dialogMode.value === 'create'
      ? [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 64, message: '密码长度应为 6–64 位', trigger: 'blur' },
        ]
      : [],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, { username: '', password: '', role: 1, status: 'ENABLED' })
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: UserListItem): void {
  dialogMode.value = 'edit'
  editingId.value = row.userId
  Object.assign(form, {
    username: row.username,
    password: '',
    role: row.role,
    status: row.status,
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
    if (dialogMode.value === 'create') {
      const payload: UserCreatePayload = {
        username: form.username.trim(),
        password: form.password,
        role: form.role,
        status: form.status,
      }
      await createUser(payload)
    } else if (editingId.value !== null) {
      const payload: UserUpdatePayload = {
        username: form.username.trim(),
        role: form.role,
        status: form.status,
      }
      await updateUser(editingId.value, payload)
    }
    ElMessage.success(dialogMode.value === 'create' ? '新增用户成功' : '修改用户成功')
    dialogVisible.value = false
    await loadUsers()
  } catch {
    // 请求层已统一提示
  } finally {
    dialogLoading.value = false
  }
}

/* ========== 重置密码 ========== */
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ newPassword: '' })
let passwordUserId: number | null = null

const passwordRules: FormRules<{ newPassword: string }> = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6–64 位', trigger: 'blur' },
  ],
}

function openResetPassword(row: UserListItem): void {
  passwordUserId = row.userId
  passwordForm.newPassword = ''
  passwordDialogVisible.value = true
  void nextTick(() => passwordFormRef.value?.clearValidate())
}

async function submitResetPassword(): Promise<void> {
  if (!passwordFormRef.value || passwordLoading.value || passwordUserId === null) {
    return
  }
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  passwordLoading.value = true
  try {
    await resetPassword(passwordUserId, passwordForm.newPassword)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } catch {
    // 请求层已统一提示
  } finally {
    passwordLoading.value = false
  }
}

/* ========== 状态 / 删除 ========== */
async function toggleStatus(row: UserListItem): Promise<void> {
  const nextStatus = row.status === 'ENABLED' ? 0 : 1
  const action = nextStatus === 0 ? '禁用' : '启用'
  const confirmed = await ElMessageBox.confirm(
    `确认${action}账号「${row.username}」？`,
    '状态变更',
    { type: 'warning', confirmButtonText: action, cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await updateUserStatus(row.userId, nextStatus)
    ElMessage.success(`${action}成功`)
    await loadUsers()
  } catch {
    // 请求层已统一提示
  }
}

async function removeUser(row: UserListItem): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除账号「${row.username}」？该操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteUser(row.userId)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch {
    // 请求层已统一提示
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="用户管理" description="管理系统登录账号、角色与启用状态">
      <el-button v-perm="'user:add'" type="primary" @click="openCreate">
        ＋ 新增用户
      </el-button>
    </PageHeader>

    <SearchCard>
      <span class="search-meta">共 {{ total }} 个账号</span>
      <el-button :loading="loading" @click="loadUsers">刷新数据</el-button>
    </SearchCard>

    <section class="card table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无用户数据">
        <el-table-column prop="username" label="账号" min-width="150" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">{{ roleLabel(row.role) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ENABLED' ? 'success' : 'danger'"
              effect="light"
              round
            >
              {{ userStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="row.userId !== currentUserId"
                v-perm="'user:edit'"
                link
                type="primary"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="row.userId !== currentUserId"
                v-perm="'user:resetPwd'"
                link
                type="primary"
                @click="openResetPassword(row)"
              >
                重置密码
              </el-button>
              <el-button
                v-if="row.userId !== currentUserId"
                v-perm="'user:status'"
                link
                type="warning"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </el-button>
              <el-button
                v-if="row.userId !== currentUserId"
                v-perm="'user:delete'"
                link
                type="danger"
                @click="removeUser(row)"
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
          @current-change="() => loadUsers()"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="3–32 位" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6–64 位"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select
            v-model="form.role"
            placeholder="请选择角色"
            :disabled="dialogMode === 'edit' && editingId === currentUserId"
          >
            <el-option
              v-for="option in ROLE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <p
          v-if="dialogMode === 'edit' && editingId === currentUserId"
          class="form-hint"
        >
          不能修改当前登录账号的角色。
        </p>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="form.status"
            placeholder="请选择状态"
            :disabled="dialogMode === 'edit' && editingId === currentUserId"
          >
            <el-option
              v-for="option in USER_STATUS_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="submitForm">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordDialogVisible"
      title="重置密码"
      width="420px"
      destroy-on-close
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="6–64 位"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitResetPassword">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.form-hint {
  margin: -10px 0 16px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
