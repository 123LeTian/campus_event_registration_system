import { createRouter, createWebHistory } from 'vue-router'

import { ElMessage } from 'element-plus/es/components/message/index'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import type { UserRole } from '@/types/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { left: 0, top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { publicOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { publicOnly: true },
    },
    {
      path: '/events',
      name: 'events',
      component: () => import('@/views/EventHallView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/events/:id',
      name: 'event-detail',
      component: () => import('@/views/EventDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/manage/events',
      name: 'manage-events',
      component: () => import('@/views/ManageEventsView.vue'),
      meta: { requiresAuth: true, roles: ['ORGANIZER', 'ADMIN'] as UserRole[] },
    },
    {
      path: '/manage/events/new',
      name: 'event-new',
      component: () => import('@/views/EventFormView.vue'),
      meta: { requiresAuth: true, roles: ['ORGANIZER', 'ADMIN'] as UserRole[] },
    },
    {
      path: '/manage/events/:id/edit',
      name: 'event-edit',
      component: () => import('@/views/EventFormView.vue'),
      meta: { requiresAuth: true, roles: ['ORGANIZER', 'ADMIN'] as UserRole[] },
    },
    {
      path: '/manage/events/:id/registrations',
      name: 'event-registrations',
      component: () => import('@/views/RegistrationListView.vue'),
      meta: { requiresAuth: true, roles: ['ORGANIZER', 'ADMIN'] as UserRole[] },
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('@/views/StatisticsView.vue'),
      meta: { requiresAuth: true, roles: ['ORGANIZER', 'ADMIN'] as UserRole[] },
    },
    {
      path: '/my/registrations',
      name: 'my-registrations',
      component: () => import('@/views/MyRegistrationsView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as UserRole[] },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.publicOnly && authStore.isLoggedIn) {
    return { path: '/' }
  }
  const roles = to.meta.roles as UserRole[] | undefined
  if (roles && (!authStore.user || !roles.includes(authStore.user.role))) {
    ElMessage.error('无权限访问该页面')
    return { path: '/events' }
  }
  return true

})

export default router
