import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 按钮级权限指令：v-perm="'user:add'"
 *
 * <p>前端隐藏按钮只是体验优化，不是安全防线；真正的权限校验由后端
 * {@code @PreAuthorize} 与路径级鉴权完成。
 */
function apply(el: HTMLElement, permission: string | undefined): void {
  const auth = useAuthStore()
  el.style.display = !permission || auth.hasPermission(permission) ? '' : 'none'
}

export const permissionDirective: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    apply(el, binding.value)
  },
  updated(el, binding) {
    apply(el, binding.value)
  },
}
