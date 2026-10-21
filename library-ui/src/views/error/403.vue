<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <p class="error-page__code">403</p>
    <h1>没有访问权限</h1>
    <p class="error-page__desc">
      当前账号未获得该页面的访问权限。如需开通，请联系系统管理员。
    </p>
    <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  place-content: center;
  justify-items: center;
  gap: 12px;
  padding: var(--space-page);
  text-align: center;
  background: var(--color-paper);
}

.error-page__code {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 64px;
  color: var(--color-accent);
}

.error-page__desc {
  max-width: 420px;
  margin: 0 0 12px;
  color: var(--color-text-muted);
}
</style>
