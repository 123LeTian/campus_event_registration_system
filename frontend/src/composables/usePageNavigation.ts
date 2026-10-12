import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/auth'

const returnLabels: Record<string, string> = {
  events: '返回活动大厅',
  'manage-events': '返回活动管理',
  'event-detail': '返回活动详情',
  'my-registrations': '返回我的报名',
  'event-registrations': '返回报名名单',
}

export function usePageNavigation(
  fallback: MaybeRefOrGetter<string> = '/events',
  allowedSources: string[] = [],
) {
  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()

  const returnTarget = computed(() => {
    const source = route.query.returnTo
    if (typeof source === 'string' && source.startsWith('/') && !/^\/\/|[\\\x00-\x1f]/.test(source)) {
      const target = router.resolve(source)
      const roles = target.meta.roles as UserRole[] | undefined
      if (allowedSources.includes(String(target.name)) && target.path !== route.path
        && (!roles || (authStore.user && roles.includes(authStore.user.role)))) {
        return target
      }
    }
    return router.resolve(toValue(fallback))
  })

  const returnLabel = computed(() => returnLabels[String(returnTarget.value.name)] || '返回上一页')

  function openPage(path: string) {
    return router.push({ path, query: { returnTo: route.fullPath } })
  }

  function goBack() {
    // Pop an existing source entry so returning never adds a detail/edit loop.
    if (router.options.history.state.back === returnTarget.value.fullPath) {
      router.back()
      return Promise.resolve()
    }
    return router.replace(returnTarget.value.fullPath)
  }

  return { openPage, goBack, returnLabel, returnTarget }
}
