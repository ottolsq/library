<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <p class="error-page__code">404</p>
    <h1>页面不存在</h1>
    <p class="error-page__desc">你访问的页面可能已被移除，或地址输入有误。</p>
    <el-button type="primary" @click="router.push('/')">返回首页</el-button>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100%;
  place-content: center;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.error-page__code {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 64px;
  color: var(--color-accent);
}

.error-page__desc {
  margin: 0 0 12px;
  color: var(--color-text-muted);
}
</style>
