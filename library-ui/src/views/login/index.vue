<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { LoginPayload } from '@/api/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<LoginPayload>({
  username: '',
  password: '',
})

/** 与后端 LoginDTO 的校验规则保持一致 */
const rules: FormRules<LoginPayload> = {
  username: [
    { required: true, whitespace: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度应为 3–32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6–64 位', trigger: 'blur' },
  ],
}

/** 登录后回跳地址：只接受站内绝对路径，避免开放重定向 */
function getRedirectTarget(): string {
  const redirect = route.query.redirect
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    !redirect.startsWith('//') &&
    !redirect.startsWith('/\\')
  ) {
    return redirect
  }
  return '/'
}

async function handleSubmit(): Promise<void> {
  if (loading.value || !formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    await router.replace(getRedirectTarget())
  } catch {
    // 请求层已统一展示后端返回的错误信息
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 已有令牌（例如刷新页面）则先恢复档案与菜单，再直接跳走
  if (!auth.token) {
    return
  }
  const restored = await auth.ensureSession()
  if (restored) {
    await router.replace(getRedirectTarget())
  }
})
</script>

<template>
  <main class="login-page">
    <section class="login-brand" aria-label="藏書閣图书管理系统">
      <div class="brand-head">
        <p class="brand-mark">藏書閣</p>
        <p class="brand-sub">LIBRARY MANAGEMENT</p>
      </div>

      <div class="brand-quote">
        <h1>让每一本书<br />都能被找到。</h1>
        <span class="brand-rule"></span>
        <ul class="brand-meta">
          <li>馆藏管理</li>
          <li>借阅流转</li>
          <li>读者档案</li>
        </ul>
      </div>

      <p class="brand-foot">EST. 2026 · 图书管理系统</p>
    </section>

    <section class="login-panel">
      <p class="login-mobile-brand">藏書閣</p>

      <div class="login-card">
        <header class="login-head">
          <p class="login-kicker">欢迎回来</p>
          <h2>登录藏書閣</h2>
          <p class="login-desc">使用管理员或读者账号继续</p>
        </header>

        <el-form
          ref="formRef"
          class="login-form"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="请输入账号"
              autocomplete="username"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </el-form-item>

          <el-button
            class="login-submit"
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="loading"
          >
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form>

        <p class="login-hint">账号由系统管理员分配</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 56% 44%;
  min-height: 100vh;
  background: var(--color-paper);
}

/* ========== 左侧品牌区 ========== */
.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 56px 56px 40px;
  color: var(--color-paper);
  background: var(--color-primary-deep);
}

.login-brand::before {
  position: absolute;
  right: -180px;
  bottom: -160px;
  width: 520px;
  height: 520px;
  content: '';
  border: 1px solid rgba(180, 137, 74, 0.22);
  border-radius: 50%;
}

.login-brand::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background: repeating-linear-gradient(
    90deg,
    transparent 0 47px,
    rgba(246, 242, 234, 0.05) 47px 48px
  );
}

.login-brand > * {
  position: relative;
  z-index: 1;
}

.brand-mark {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 40px;
  letter-spacing: 0.2em;
}

.brand-sub {
  margin: 10px 0 0;
  font-size: 11px;
  letter-spacing: 0.32em;
  color: var(--color-accent);
}

.brand-quote h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(38px, 4.2vw, 58px);
  font-weight: 600;
  line-height: 1.42;
  color: var(--color-paper);
}

.brand-rule {
  display: block;
  width: 64px;
  height: 2px;
  margin: 30px 0;
  background: var(--color-accent);
}

.brand-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0;
  margin: 0;
  font-size: 13px;
  color: #aebccb;
  list-style: none;
}

.brand-meta li + li::before {
  display: inline-block;
  width: 4px;
  height: 4px;
  margin-right: 14px;
  content: '';
  vertical-align: middle;
  background: var(--color-accent);
}

.brand-foot {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.2em;
  color: #8194a8;
}

/* ========== 右侧登录区 ========== */
.login-panel {
  display: grid;
  place-items: center;
  padding: 48px;
  background: var(--color-surface);
}

.login-mobile-brand {
  display: none;
}

.login-card {
  width: min(380px, 100%);
  animation: login-rise 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.login-head {
  margin-bottom: 30px;
}

.login-kicker {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 0.18em;
  color: var(--color-accent);
}

.login-head h2 {
  margin: 0 0 10px;
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
}

.login-desc {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-muted);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 13px;
  color: var(--color-text);
}

.login-form :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 1px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-control);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b9ae9c inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 3px rgba(31, 58, 95, 0.1);
}

.login-form :deep(.el-form-item__error) {
  font-size: 12px;
}

.login-submit {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 0.2em;
}

.login-hint {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}

@keyframes login-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-brand {
    display: none;
  }

  .login-panel {
    padding: 32px 24px;
  }

  .login-mobile-brand {
    display: block;
    align-self: start;
    margin: 0 0 24px;
    font-family: var(--font-serif);
    font-size: 26px;
    letter-spacing: 0.18em;
    color: var(--color-primary);
  }

  .login-panel {
    grid-template-rows: auto 1fr;
    align-items: start;
    justify-items: center;
  }

  .login-card {
    margin-top: 6vh;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }
}
</style>
