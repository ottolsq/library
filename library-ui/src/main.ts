import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './router/guard'
import { permissionDirective } from './utils/permission'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(ElementPlus, { locale: zhCn })
app.directive('perm', permissionDirective)

// 必须在 app.use(router) 之前注册，首次导航才会经过守卫
setupRouterGuard()
app.use(router)

app.mount('#app')
