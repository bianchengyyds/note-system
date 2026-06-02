import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserInfo  } from '@/utils/auth'
import AdminPanel from '@/views/admin/adminPanel.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/share/:shareKey',
    name: 'PublicNote',
    component: () => import('@/views/share/PublicNote.vue'),
    meta: { title: '分享笔记' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/invitations',
    name: 'PendingInvitations',
    component: () => import('@/views/kb/MyInvitations.vue'),
    meta: { title: '我的邀请', requiresAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/home/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'kb',
        name: 'KbList',
        component: () => import('@/views/kb/KbList.vue'),
        meta: { title: '我的知识库' },
      },
      {
        path: 'kb/:kbId',
        name: 'KbDetail',
        component: () => import('@/views/kb/KbDetail.vue'),
        meta: { title: '知识库' }
      },
      // ✅ 笔记编辑 / 查看也放置在 MainLayout 中，共享侧边栏
      {
        path: 'note/:docId',
        name: 'NoteEditor',
        component: () => import('@/views/note/NoteEditor.vue'),
        meta: { title: '笔记' }
      },
      {
        path: 'trash',
        name: 'Trash',
        component: () => import('@/views/trash/Trash.vue'),
        meta: { title: '回收站', requiresAuth: true }
      },
      {
        path: 'search',
        name: 'SearchResult',
        component: () => import('@/views/search/SearchResult.vue'),
        meta: { title: '搜索结果', requiresAuth: true }
      },
      {
        path: 'note/:docId/versions',
        name: 'NoteVersions',
        component: () => import('@/views/note/NoteVersions.vue'),
        meta: { title: '版本历史', requiresAuth: true }
      },
      {
        path: 'test-tiptap',
        name: 'TestTiptap',
        component: () => import('@/views/TestTiptap.vue'),
        meta: { title: 'Tiptap编辑器测试', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/user/Favorites.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'browse-history',
        name: 'BrowseHistory',
        component: () => import('@/views/user/BrowseHistory.vue'),
        meta: { title: '浏览历史', requiresAuth: true }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/admin/UserList.vue'),
        meta: { title: '用户管理', requiresAuth: true,role:'ADMIN' }
      },  




    ]
  },
  {
  path: '/admin',
  component: () => import('@/views/admin/AdminPanel.vue'),  // ✅ 改为 AdminPanel
  meta: { requiresAuth: true, role: 'ADMIN' },
  redirect: '/admin/statistics',
  children: [
    {
      path: 'statistics',
      name: 'AdminStatistics',
      component: () => import('@/views/admin/Statistics.vue'),
      meta: { title: '数据统计' }
    },
    {
      path: 'users',
      name: 'AdminUsers',
      component: () => import('@/views/admin/Users.vue'),
      meta: { title: '用户管理' }
    },
    {
      path: 'notes',
      name: 'AdminNotes',
      component: () => import('@/views/admin/NoteManage.vue'),
      meta: { title: '笔记管理' }
    },
    {
      path: 'notes/view/:docId',
      name: 'AdminNoteView',
      component: () => import('@/views/admin/NoteView.vue'),
      meta: { title: '查看笔记' }
    },
    {
      path: 'kbs',
      name: 'AdminKbs',
      component: () => import('@/views/admin/KbManage.vue'),
      meta: { title: '知识库管理' }
    },
    {
      path: 'logs',
      name: 'AdminLogs',
      component: () => import('@/views/admin/Logs.vue'),
      meta: { title: '操作日志' }
    }
    ]
   }
 

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局守卫
router.beforeEach((to, from) => {
  document.title = to.meta.title ? `${to.meta.title} - 笔记系统` : '笔记系统'
  const token = getToken()

  // 公共页面（无需登录）
  const publicPaths = ['/login', '/register', '/share']
  const isPublicPath = publicPaths.some(p => to.path.startsWith(p))

  // 需要登录但无 token → 跳转登录
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录但访问登录/注册页 → 根据角色跳转首页
  if ((to.path === '/login' || to.path === '/register') && token) {
    const userInfo = getUserInfo()
    return userInfo?.role === 'ADMIN' ? '/admin/statistics' : '/dashboard'
  }

  // 角色隔离：管理员只能访问后台，普通用户只能访问前台
  const userInfo = getUserInfo()
  if (token && userInfo && !isPublicPath) {
    const isAdmin = userInfo.role === 'ADMIN'
    const isAdminRoute = to.path.startsWith('/admin')
    // 管理员允许访问笔记查看页面和知识库详情页面
    const isAllowedRoute = to.path.startsWith('/note/') || to.path.startsWith('/kb/')

    if (isAdmin && !isAdminRoute && !isAllowedRoute) {
      // 管理员访问非后台页面（除笔记查看和知识库详情外） → 重定向到后台首页
      return '/admin/statistics'
    }

    if (!isAdmin && isAdminRoute) {
      // 普通用户访问后台页面 → 重定向到前台首页
      return '/dashboard'
    }
  }

  // ✅ 角色鉴权：仅对有 meta.role 的路由生效
  if (to.meta.role) {
    const userInfo = getUserInfo()
    if (!userInfo || userInfo.role !== to.meta.role) {
      // 角色不符，跳转到首页
      return '/dashboard'
    }
  }

  return true
})

export default router