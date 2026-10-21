<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import SidebarMenu from '@/components/SidebarMenu.vue'
import { useAuthStore } from '@/stores/auth'
import { filterMenuTree, findMenuTrail } from '@/utils/menu'
import { roleLabel } from '@/utils/dict'
import { resetDynamicRoutes } from '@/router/dynamic'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const loggingOut = ref(false)

const visibleMenus = computed(() => filterMenuTree(auth.menus))
const breadcrumb = computed(() => findMenuTrail(auth.menus, route.path))
const openMenus = computed(() =>
  breadcrumb.value
    .filter((node) => node.menuType === 'M')
    .map((node) => `dir-${node.menuId}`),
)
const displayName = computed(() => auth.nickname || auth.username || '未登录')
const initial = computed(() => displayName.value.slice(0, 1))
const roleName = computed(() =>
  roleLabel(auth.isSuperAdmin || auth.roles.includes('ROLE_ADMIN') ? 0 : 1),
)

function syncViewport(): void {
  if (window.innerWidth < 1100) {
    isCollapse.value = true
  }
}

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})

async function handleLogout(): Promise<void> {
  if (loggingOut.value) {
    return
  }
  loggingOut.value = true
  try {
    await auth.logout()
    await router.replace('/login')
    resetDynamicRoutes()
    ElMessage.success('已退出登录')
  } finally {
    loggingOut.value = false
  }
}

function handleCommand(command: unknown): void {
  if (command === 'logout') {
    void handleLogout()
  }
}
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <button
        class="icon-button"
        type="button"
        :aria-label="isCollapse ? '展开菜单' : '折叠菜单'"
        @click="isCollapse = !isCollapse"
      >
        <el-icon :size="18">
          <Expand v-if="isCollapse" />
          <Fold v-else />
        </el-icon>
      </button>

      <router-link class="brand" to="/">
        <span class="brand__mark">藏書閣</span>
        <span class="brand__sub">图书管理系统</span>
      </router-link>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="node in breadcrumb" :key="node.menuId">
          {{ node.menuName }}
        </el-breadcrumb-item>
      </el-breadcrumb>

      <span class="spacer"></span>

      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-chip" type="button">
          <span class="user-chip__avatar">{{ initial }}</span>
          <span class="user-chip__meta">
            <span class="user-chip__name">{{ displayName }}</span>
            <span class="user-chip__role">{{ roleName }}</span>
          </span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ roleName }} · {{ auth.username }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <div class="layout__body">
      <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapse }">
        <el-menu
          class="sidebar-menu"
          :default-active="route.path"
          :default-openeds="openMenus"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          unique-opened
          popper-class="sidebar-popup"
        >
          <SidebarMenu :nodes="visibleMenus" />
        </el-menu>
      </aside>

      <main class="layout-main">
        <div class="layout-main__inner">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-rows: var(--topbar-height) minmax(0, 1fr);
  height: 100vh;
  overflow: hidden;
  background: var(--color-paper);
}

/* ========== 顶部栏 ========== */
.topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: var(--color-text-muted);
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
}

.icon-button:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.icon-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: inherit;
  text-decoration: none;
  white-space: nowrap;
}

.brand__mark {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--color-primary);
}

.brand__sub {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.topbar :deep(.el-breadcrumb) {
  font-size: 13px;
}

.topbar :deep(.el-breadcrumb__inner) {
  font-weight: 400;
  color: var(--color-text-muted);
}

.topbar :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--color-text);
}

.spacer {
  flex: 1;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 4px 4px;
  color: var(--color-text);
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 999px;
}

.user-chip:hover {
  background: var(--color-paper);
  border-color: var(--color-border);
}

.user-chip:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.user-chip__avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  font-size: 13px;
  color: #fff;
  background: var(--color-primary);
  border-radius: 50%;
}

.user-chip__meta {
  display: grid;
  line-height: 1.2;
  text-align: left;
}

.user-chip__name {
  font-size: 13px;
}

.user-chip__role {
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ========== 主体 ========== */
.layout__body {
  display: flex;
  min-height: 0;
}

.sidebar {
  flex: 0 0 var(--sidebar-width);
  width: var(--sidebar-width);
  overflow: hidden;
  background: var(--color-primary-deep);
  transition:
    width 0.2s ease,
    flex-basis 0.2s ease;
}

.sidebar--collapsed {
  flex-basis: var(--sidebar-width-collapsed);
  width: var(--sidebar-width-collapsed);
}

.sidebar-menu {
  height: 100%;
  padding: 12px 8px;
  overflow-y: auto;
  background: transparent;
  border-right: none;

  --el-menu-bg-color: transparent;
  --el-menu-text-color: #aebccb;
  --el-menu-active-color: #fff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 2px 0;
  border-radius: 6px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(180, 137, 74, 0.16);
  box-shadow: inset 2px 0 0 var(--color-accent);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  color: #aebccb;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  color: #fff;
}

.layout-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}

.layout-main__inner {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: 24px;
}

/* ========== 页面切换 ========== */
.page-enter-active,
.page-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .topbar {
    gap: 8px;
    padding: 0 12px;
  }

  .brand__sub,
  .user-chip__meta,
  .topbar :deep(.el-breadcrumb) {
    display: none;
  }

  .layout-main__inner {
    padding: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .sidebar {
    transition: none;
  }

  .page-enter-active,
  .page-leave-active {
    transition: none;
  }
}
</style>
