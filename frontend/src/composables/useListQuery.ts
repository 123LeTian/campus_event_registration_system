import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

interface ListQuery {
  page: number
  pageSize: number
  status: string
  keyword?: string
}

export function useListQuery<T extends ListQuery>(defaults: T, statuses: readonly string[]) {
  const route = useRoute()
  const router = useRouter()
  const status = typeof route.query.status === 'string' ? route.query.status : defaults.status

  function positiveInteger(value: unknown, fallback: number) {
    const number = typeof value === 'string' && /^[1-9]\d*$/.test(value) ? Number(value) : NaN
    return Number.isSafeInteger(number) ? number : fallback
  }

  const query = reactive({
    ...defaults,
    page: positiveInteger(route.query.page, defaults.page),
    pageSize: Math.min(50, positiveInteger(route.query.pageSize, defaults.pageSize)),
    status: statuses.includes(status) ? status : defaults.status,
    ...('keyword' in defaults ? { keyword: typeof route.query.keyword === 'string' ? route.query.keyword : defaults.keyword } : {}),
  }) as T

  function syncQuery() {
    const target = {
      query: {
        ...route.query,
        page: query.page === defaults.page ? undefined : String(query.page),
        pageSize: query.pageSize === defaults.pageSize ? undefined : String(query.pageSize),
        status: query.status === defaults.status ? undefined : query.status,
        ...('keyword' in defaults ? { keyword: query.keyword || undefined } : {}),
      },
    }
    if (router.resolve(target).fullPath === route.fullPath) return Promise.resolve()
    return router.replace(target)
  }

  return { query, syncQuery }
}
