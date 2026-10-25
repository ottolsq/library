<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/auth'

interface RegisterFormModel {
  username: string
  password: string
  confirmPassword: string
  readerName: string
  studentId: string
  phone: string
}

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterFormModel>({
  username: '',
  password: '',
  confirmPassword: '',
  readerName: '',
  studentId: '',
  phone: '',
})

/** 与后端 RegisterDTO 的校验规则保持一致 */
const rules: FormRules<RegisterFormModel> = {
  username: [
    { required: true, whitespace: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度应为 3–32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6–64 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  readerName: [
    { required: true, whitespace: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名不能超过 50 字', trigger: 'blur' },
  ],
  studentId: [
    { required: true, whitespace: true, message: '请输入学号', trigger: 'blur' },
    { max: 30, message: '学号不能超过 30 位', trigger: 'blur' },
  ],
  phone: [
    {
      validator: (_rule, value, callback) => {
        if (value && !/^1[3-9]\d{9}$/.test(value)) {
          callback(new Error('请输入 11 位手机号'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || loading.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      readerName: form.readerName.trim(),
      studentId: form.studentId.trim(),
      phone: form.phone.trim() || undefined,
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({
      path: '/login',
      query: { registered: '1', username: form.username.trim() },
    })
  } catch {
    // 请求层已统一展示后端返回的错误信息
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <div class="register-shell">
      <header class="register-brand">
        <div>
          <p class="brand-mark">藏書閣</p>
          <p class="brand-sub">LIBRARY MANAGEMENT</p>
        </div>
        <p class="brand-note">
          「让每一本书<br />都能被找到。」
        </p>
      </header>

      <section class="register-card">
        <span class="card-rule"></span>
        <p class="card-kicker">读者注册</p>
        <h1>创建你的读者账号</h1>
        <p class="card-desc">
          注册后默认角色为读者，可借 5 本图书；管理员账号由系统分配。
        </p>

        <el-form
          ref="formRef"
          class="register-form"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              placeholder="3–32 位，用于登录"
              autocomplete="username"
              clearable
            />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="6–64 位"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
                autocomplete="new-password"
              />
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="姓名" prop="readerName">
              <el-input
                v-model="form.readerName"
                placeholder="读者姓名"
                maxlength="50"
              />
            </el-form-item>
            <el-form-item label="学号" prop="studentId">
              <el-input
                v-model="form.studentId"
                placeholder="学号"
                maxlength="30"
              />
            </el-form-item>
          </div>

          <el-form-item label="手机号（选填）" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="11 位手机号"
              maxlength="11"
            />
          </el-form-item>

          <el-button
            class="register-submit"
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="loading"
          >
            {{ loading ? '注册中…' : '注 册' }}
          </el-button>
        </el-form>

        <p class="register-foot">
          已有账号？
          <router-link to="/login">返回登录</router-link>
        </p>
      </section>
    </div>
  </main>
</template>

<style scoped>
.register-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(circle at 12% 18%, rgba(180, 137, 74, 0.12), transparent 32%),
    radial-gradient(circle at 88% 82%, rgba(31, 58, 95, 0.08), transparent 34%),
    var(--color-paper);
}

.register-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 36px;
  align-items: start;
  width: min(100%, 900px);
}

.register-brand {
  display: flex;
  flex-direction: column;
  gap: 40px;
  padding: 24px 0 0 8px;
}

.brand-mark {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 34px;
  letter-spacing: 0.2em;
  color: var(--color-primary);
}

.brand-sub {
  margin: 10px 0 0;
  font-size: 10px;
  letter-spacing: 0.3em;
  color: var(--color-accent);
}

.brand-note {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 20px;
  line-height: 1.8;
  color: var(--color-text-muted);
}

.register-card {
  padding: 40px 44px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.card-rule {
  display: block;
  width: 48px;
  height: 3px;
  margin-bottom: 24px;
  background: var(--color-accent);
}

.card-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--color-accent);
}

.register-card h1 {
  margin: 10px 0 8px;
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
  color: var(--color-text);
}

.card-desc {
  margin: 0 0 28px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.register-submit {
  width: 100%;
  margin-top: 8px;
}

.register-foot {
  margin: 22px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}

.register-foot a {
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
}

.register-foot a:hover {
  color: var(--color-accent);
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 820px) {
  .register-page {
    padding: 20px;
  }

  .register-shell {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .register-brand {
    flex-direction: row;
    align-items: baseline;
    justify-content: space-between;
    padding: 0 4px;
  }

  .brand-note {
    font-size: 14px;
    text-align: right;
  }

  .register-card {
    padding: 28px 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

</style>
