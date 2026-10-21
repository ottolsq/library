<script setup lang="ts">
import { joinMenuPath } from '@/utils/menu'
import type { MenuNode } from '@/api/types'
import AppIcon from './AppIcon.vue'

defineOptions({ name: 'SidebarMenu' })

const props = withDefaults(
  defineProps<{
    nodes: MenuNode[]
    basePath?: string
  }>(),
  { basePath: '' },
)

function fullPath(node: MenuNode): string {
  return joinMenuPath(props.basePath, node.path)
}
</script>

<template>
  <template v-for="node in nodes" :key="node.menuId">
    <el-sub-menu
      v-if="node.menuType === 'M' && node.children.length > 0"
      :index="`dir-${node.menuId}`"
    >
      <template #title>
        <el-icon><AppIcon :name="node.icon" /></el-icon>
        <span>{{ node.menuName }}</span>
      </template>
      <SidebarMenu :nodes="node.children" :base-path="fullPath(node)" />
    </el-sub-menu>

    <el-menu-item v-else-if="node.menuType === 'C'" :index="fullPath(node)">
      <el-icon><AppIcon :name="node.icon" /></el-icon>
      <template #title>{{ node.menuName }}</template>
    </el-menu-item>
  </template>
</template>
