<script setup lang="ts">
import { computed, type Component } from 'vue'
import {
  Document,
  Grid,
  List,
  Reading,
  Setting,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'

/**
 * 菜单 icon 短名 → Element Plus 图标。
 * 后端 sys_menu.icon 存的是短名，新增图标时在这里补映射即可。
 */
const ICONS: Record<string, Component> = {
  setting: Setting,
  user: User,
  peoples: UserFilled,
  book: Reading,
  borrow: Tickets,
  documentation: Document,
  list: List,
}

const props = defineProps<{ name?: string | null }>()

const icon = computed(() => (props.name ? ICONS[props.name] ?? Grid : Grid))
</script>

<template>
  <component :is="icon" />
</template>
