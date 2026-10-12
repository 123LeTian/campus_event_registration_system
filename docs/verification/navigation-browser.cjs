const playwright = process.env.NAV_PLAYWRIGHT || 'C:/Users/harvey/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'
const { chromium } = require(playwright)
const { expect } = require(playwright + '/test')
const assert = require('node:assert/strict')

const base = process.env.NAV_FRONTEND || 'http://localhost:5173'
// Run against an isolated backend with the test profile and H2 test data.
const api = process.env.NAV_API || 'http://127.0.0.1:18080'

async function run() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    async function login(username, password) {
      const response = await context.request.post(`${api}/api/auth/login`, { data: { username, password } })
      assert.equal(response.status(), 200)
      return (await response.json()).data
    }
    const organizer = await login('organizer1', 'Org123456')
    const student = await login('studentA', 'Stu123456')
    const admin = await login('admin', 'Admin123456')
    async function request(method, path, session, data) {
      return context.request.fetch(api + path, {
        method, headers: { Authorization: `Bearer ${session.token}` }, data,
      })
    }
    const start = new Date(Date.now() + 5 * 86400000)
    const time = offset => new Date(start.getTime() + offset).toISOString().slice(0, 19)
    const payload = {
      title: 'Navigation event', copywriting: 'Navigation copy', description: 'Navigation introduction',
      location: 'Campus hall', startTime: time(0), endTime: time(7200000),
      registrationDeadline: time(-86400000), maxParticipants: 30,
    }
    async function create(title, session = organizer) {
      const response = await request('POST', '/api/events', session, { ...payload, title })
      assert.equal(response.status(), 200)
      return (await response.json()).data
    }
    for (let index = 0; index < 14; index++) await create(`Navigation event ${index}`)
    await context.addInitScript(session => {
      localStorage.setItem('campus_event_token', session.token)
      localStorage.setItem('campus_event_user', JSON.stringify(session.user))
    }, organizer)
    // Forward browser calls to the real test backend, without mocking responses.
    await context.route(url => url.pathname.startsWith('/api/'), async route => {
      const original = route.request()
      const url = new URL(original.url())
      const response = await context.request.fetch(api + url.pathname + url.search, {
        method: original.method(), headers: original.headers(), data: original.postDataBuffer() || undefined,
      })
      await route.fulfill({ response })
    })
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', error => errors.push(error.message))
    const path = () => page.url().slice(base.length)
    const heading = () => page.locator('.page-heading')
    const back = label => heading().getByRole('button', { name: label, exact: true })
    const item = label => page.locator('.el-form-item').filter({ has: page.locator('.el-form-item__label', { hasText: new RegExp(`^${label}$`) }) })
    const input = label => item(label).locator('input').first()
    const detailEdit = () => page.locator('.registration-box').getByRole('button', { name: '编辑活动', exact: true })
    async function waitPath(target) { await expect(page).toHaveURL(base + target) }
    async function waitForm() { await expect(input('活动标题')).toBeEnabled() }
    async function openManageDetail() {
      await page.getByRole('button', { name: '查看详情', exact: true }).first().click()
      await expect(back('返回活动管理')).toBeVisible()
      await expect(detailEdit()).toBeEnabled()
    }

    await page.goto(`${base}/manage/events`)
    await page.getByPlaceholder('搜索标题、地点、文案或介绍').fill('Navigation')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await page.getByText('报名中', { exact: true }).click()
    await expect(page.locator('.el-pagination .btn-next')).toBeEnabled()
    await page.locator('.el-pagination .btn-next').click()
    await expect(page).toHaveURL(/page=2/)
    await expect(page.getByRole('button', { name: '查看详情', exact: true }).first()).toBeVisible()
    const management = path()
    await openManageDetail()
    const detail = path()
    const eventId = new URL(page.url()).pathname.split('/').pop()
    await page.reload()
    await back('返回活动管理').click()
    await waitPath(management)
    await expect(page.getByPlaceholder('搜索标题、地点、文案或介绍')).toHaveValue('Navigation')
    await expect(page.locator('.el-pagination .is-active')).toHaveText('2')
    console.log('PASS management -> detail -> refresh -> management: search, status and page retained')

    await openManageDetail()
    await detailEdit().click()
    await waitForm()
    await back('返回活动详情').click()
    await waitPath(detail)
    await detailEdit().click()
    await waitForm()
    await page.getByRole('button', { name: '取消', exact: true }).click()
    await waitPath(detail)
    await detailEdit().click()
    await waitForm()
    await input('活动标题').fill('Navigation updated through details')
    await input('活动文案').fill('Updated copywriting')
    await item('活动介绍').locator('textarea').fill('Updated introduction')
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await waitPath(detail)
    await expect(heading().locator('h1')).toHaveText('Navigation updated through details')
    await expect(page.locator('.detail-copywriting')).toHaveText('Updated copywriting')
    await expect(page.locator('.detail-description')).toHaveText('Updated introduction')
    await page.goBack()
    await waitPath(management)
    console.log('PASS detail -> edit: back, cancel and successful save return to fresh details; browser Back skips edit')

    await page.getByRole('button', { name: '编辑活动', exact: true }).first().click()
    await waitForm()
    await expect(back('返回活动管理')).toBeVisible()
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await waitPath(management)
    await page.getByRole('button', { name: '新建活动', exact: true }).click()
    await page.waitForURL(url => url.pathname.endsWith('/new'))
    await page.reload()
    await waitForm()
    await back('返回活动管理').click()
    await waitPath(management)
    console.log('PASS management -> direct edit/new: returns to the original management list')

    await page.goto(`${base}/events?keyword=Navigation&status=REGISTRATION_OPEN&page=2`)
    await expect(page.locator('.event-card').first()).toBeVisible()
    const hall = path()
    await page.getByRole('button', { name: '查看详情', exact: true }).first().click()
    await expect(back('返回活动大厅')).toBeVisible()
    await expect(detailEdit()).toBeEnabled()
    const hallDetail = path()
    await detailEdit().click()
    await page.waitForURL(url => url.pathname.endsWith('/edit'))
    await page.reload()
    await waitForm()
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await waitPath(hallDetail)
    await back('返回活动大厅').click()
    await waitPath(hall)
    await expect(page.locator('.el-pagination .is-active')).toHaveText('2')
    console.log('PASS hall -> detail -> edit -> refresh -> save -> detail -> hall: full source chain retained')

    const registered = await request('POST', `/api/events/${eventId}/registrations`, student)
    assert.equal(registered.status(), 200)
    await page.goto(base + management)
    await page.getByRole('button', { name: '查看名单', exact: true }).first().click()
    await page.locator('.el-segmented').getByText('有效报名', { exact: true }).click()
    await expect(page.getByRole('button', { name: '查看活动', exact: true }).first()).toBeVisible()
    const roster = path()
    await page.getByRole('button', { name: '查看活动', exact: true }).first().click()
    await expect(back('返回报名名单')).toBeVisible()
    await detailEdit().click()
    await waitForm()
    await page.getByRole('button', { name: '取消', exact: true }).click()
    await back('返回报名名单').click()
    await waitPath(roster)
    await back('返回活动管理').click()
    await waitPath(management)
    console.log('PASS management -> roster -> detail -> edit: all levels return to their source')

    await page.getByRole('button', { name: '新建活动', exact: true }).click()
    await waitForm()
    await input('活动标题').fill('Navigation published from form')
    await input('活动地点').fill(payload.location)
    await input('活动文案').fill(payload.copywriting)
    await item('活动介绍').locator('textarea').fill(payload.description)
    for (const [label, key] of [['开始时间', 'startTime'], ['结束时间', 'endTime'], ['报名截止时间', 'registrationDeadline']]) {
      await input(label).fill(payload[key].replace('T', ' '))
      await input(label).press('Tab')
    }
    await page.getByRole('button', { name: '发布活动', exact: true }).click()
    await waitPath(management)
    console.log('PASS new activity publishes through the backend and returns to its source list')

    await page.goto(`${base}/manage/events/${eventId}/edit`)
    await waitForm()
    await back('返回活动详情').click()
    await expect(page).toHaveURL(`${base}/events/${eventId}`)
    for (const source of ['https://example.com', '//example.com', '/login', `/events/${eventId}`]) {
      await page.goto(`${base}/events/${eventId}?returnTo=${encodeURIComponent(source)}`)
      await expect(back('返回活动大厅')).toBeVisible()
    }
    console.log('PASS direct links have fallbacks; external, login and self-referential return targets rejected')

    const conflict = await create('Navigation save conflict')
    await page.goto(`${base}/manage/events/${conflict.id}/edit`)
    await waitForm()
    await input('活动标题').fill('Navigation unsaved changes')
    const editPath = path()
    const ended = await request('PUT', `/api/events/${conflict.id}`, admin, {
      ...payload, title: conflict.title, startTime: '2000-01-02T00:00:00',
      endTime: '2000-01-03T00:00:00', registrationDeadline: '2000-01-01T00:00:00',
    })
    assert.equal(ended.status(), 200)
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await expect(page.getByText('活动已结束，不能编辑', { exact: true })).toBeVisible()
    await waitPath(editPath)
    await expect(input('活动标题')).toHaveValue('Navigation unsaved changes')
    console.log('PASS real backend save conflict keeps edit page and unsaved input')

    await page.evaluate(session => {
      localStorage.setItem('campus_event_token', session.token)
      localStorage.setItem('campus_event_user', JSON.stringify(session.user))
    }, student)
    // Override the organizer init script for subsequent documents in this page.
    await page.addInitScript(session => {
      localStorage.setItem('campus_event_token', session.token)
      localStorage.setItem('campus_event_user', JSON.stringify(session.user))
    }, student)
    await page.goto(`${base}/my/registrations?status=ACTIVE`)
    await page.getByRole('button', { name: '查看详情', exact: true }).first().click()
    await expect(back('返回我的报名')).toBeVisible()
    await page.reload()
    await back('返回我的报名').click()
    await expect(page).toHaveURL(`${base}/my/registrations?status=ACTIVE`)
    await page.goto(`${base}/events/${eventId}?returnTo=${encodeURIComponent('/manage/events')}`)
    await expect(back('返回活动大厅')).toBeVisible()
    await page.goto(`${base}/manage/events/${eventId}/edit`)
    await expect(page).toHaveURL(`${base}/events`)
    console.log('PASS student registration sources retained; management return injection and edit route denied')
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto(`${base}/events/${eventId}?returnTo=${encodeURIComponent('/my/registrations?status=ACTIVE')}`)
    await expect(back('返回我的报名')).toBeVisible()
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    assert.deepEqual(errors, [])
    console.log('PASS mobile return button has no overflow; no page errors')
  } finally {
    await browser.close()
  }
}

run().catch(error => { console.error(error); process.exitCode = 1 })
