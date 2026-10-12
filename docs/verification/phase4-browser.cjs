const { chromium } = require(process.env.PHASE4_PLAYWRIGHT || 'C:/Users/harvey/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const base = process.env.PHASE4_FRONTEND || 'http://127.0.0.1:5173'
const api = process.env.PHASE4_API || 'http://localhost:8080'
const output = process.env.PHASE4_RESULTS || path.join(__dirname, 'phase4-browser-results')
const checks = []
const errors = []
const title = `Phase4 browser ${Date.now()}`
const future = new Date(Date.now() + 5 * 86400000)
const date = future.toISOString().slice(0, 10)
const payload = { title, description: 'Browser acceptance event', location: 'Student activity center', startTime: `${date}T14:00:00`, endTime: `${date}T16:00:00`, registrationDeadline: `${date}T12:00:00`, maxParticipants: 3 }
let browser

async function run() {
  fs.mkdirSync(output, { recursive: true })
  browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  // Forward requests to the selected running backend without fabricating responses.
  if (process.env.PHASE4_NATIVE_PROXY !== 'true') await context.route(url => url.pathname.startsWith('/api/'), async route => {
    const request = route.request()
    const response = await context.request.fetch(api + new URL(request.url()).pathname + new URL(request.url()).search, {
      method: request.method(), headers: request.headers(), data: request.postDataBuffer() || undefined,
    })
    await route.fulfill({ response })
  })
  const page = await context.newPage()
  page.on('pageerror', error => errors.push(error.message))
  const item = label => page.locator('.el-form-item').filter({ has: page.locator('.el-form-item__label', { hasText: new RegExp(`^${label}$`) }) })
  const input = label => item(label).locator('input').first()
  async function login(username, password) {
    await page.goto(`${base}/login`)
    await input('用户名').fill(username)
    await input('密码').fill(password)
    await page.getByRole('button', { name: '登录', exact: true }).click()
    await page.waitForURL(`${base}/`)
  }
  async function screenshot(name) {
    await page.screenshot({ path: path.join(output, `${name}.png`), fullPage: true })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `${name}: horizontal page overflow`)
  }
  async function fillPayload(data) {
    await input('活动标题').fill(data.title)
    await item('活动介绍').locator('textarea').fill(data.description)
    await input('活动地点').fill(data.location)
    for (const [label, key] of [['开始时间', 'startTime'], ['结束时间', 'endTime'], ['报名截止时间', 'registrationDeadline']]) {
      await input(label).fill(data[key].replace('T', ' '))
      await input(label).press('Tab')
    }
    await input('人数上限').fill(String(data.maxParticipants))
    await input('人数上限').press('Tab')
  }
  await login('organizer1', 'Org123456')
  checks.push('Organizer login via UI')
  await page.getByRole('button', { name: '活动管理', exact: true }).click()
  await page.waitForURL('**/manage/events')
  await page.getByRole('button', { name: '新建活动' }).click()
  await page.getByRole('button', { name: '保存活动' }).click()
  await page.getByText('请输入活动标题', { exact: true }).waitFor()
  checks.push('Empty form rejected')
  await fillPayload({ ...payload, registrationDeadline: `${date}T15:00:00` })
  await page.getByRole('button', { name: '保存活动' }).click()
  await page.getByText('报名截止时间必须早于开始时间', { exact: true }).first().waitFor()
  checks.push('Invalid time order rejected')
  await fillPayload(payload)
  await screenshot('desktop-form')
  await page.getByRole('button', { name: '保存活动' }).click()
  await page.waitForURL('**/manage/events')
  let row = page.locator('.el-table__body tr').filter({ hasText: title })
  await row.first().waitFor()
  checks.push('Create saves through API and appears in management')
  await screenshot('desktop-management')
  await row.getByRole('button', { name: '编辑活动', exact: true }).click()
  await page.waitForURL('**/edit')
  await page.waitForFunction(() => document.querySelector('.el-form input')?.value.startsWith('Phase4 browser'))
  await input('活动标题').fill(`${title} edited`)
  await input('活动地点').fill('Library auditorium')
  await page.getByRole('button', { name: '保存活动' }).click()
  await page.waitForURL('**/manage/events')
  row = page.locator('.el-table__body tr').filter({ hasText: `${title} edited` })
  await row.getByRole('button', { name: '查看详情', exact: true }).click()
  await page.getByRole('heading', { name: `${title} edited` }).waitFor()
  await page.getByText('Library auditorium', { exact: true }).waitFor()
  const detailUrl = page.url()
  const id = Number(detailUrl.split('/').pop())
  checks.push('Edit persists and detail displays updated fields')
  await screenshot('desktop-detail')
  await page.getByRole('button', { name: '活动大厅', exact: true }).click()
  await page.locator('.event-card').filter({ hasText: `${title} edited` }).waitFor()
  checks.push('Hall uses backend event data')
  await screenshot('desktop-hall')
  await page.setViewportSize({ width: 390, height: 844 })
  await screenshot('mobile-hall')
  await page.goto(detailUrl)
  await page.getByRole('heading', { name: `${title} edited` }).waitFor()
  await screenshot('mobile-detail')
  await page.goto(`${base}/manage/events/${id}/edit`)
  await input('活动标题').waitFor()
  await page.waitForFunction(() => document.querySelector('.el-form input')?.value.startsWith('Phase4 browser'))
  await screenshot('mobile-form')
  await page.goto(`${base}/manage/events`)
  await page.locator('.el-table__body tr').filter({ hasText: title }).first().waitFor()
  await screenshot('mobile-management')
  checks.push('Desktop/mobile layouts have no horizontal page overflow')
  await page.setViewportSize({ width: 1440, height: 1000 })
  row = page.locator('.el-table__body tr').filter({ hasText: title })
  await row.getByRole('button', { name: '删除活动', exact: true }).click()
  await page.getByRole('dialog').waitFor()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await row.first().waitFor()
  checks.push('Delete confirmation cancel preserves event')
  await row.getByRole('button', { name: '删除活动', exact: true }).click()
  await page.getByRole('button', { name: '确认删除', exact: true }).click()
  await row.waitFor({ state: 'hidden' })
  checks.push('Confirmed deletion removes event from list')
  const token = await page.evaluate(() => localStorage.getItem('campus_event_token'))
  const detailAfterDelete = await context.request.get(`${api}/api/events/${id}`, { headers: { Authorization: `Bearer ${token}` } })
  assert.equal(detailAfterDelete.status(), 404)
  checks.push('Deleted event detail returns 404')
  await page.goto(`${base}/`)
  await page.getByRole('button', { name: '退出登录' }).click()
  await login('studentA', 'Stu123456')
  assert.equal(await page.getByRole('button', { name: '活动管理', exact: true }).count(), 0)
  for (const route of ['/manage/events', '/manage/events/new', `/manage/events/${id}/edit`]) {
    await page.goto(base + route)
    await page.waitForURL('**/events')
  }
  checks.push('Student management entry hidden; all three management routes blocked')
  const studentToken = await page.evaluate(() => localStorage.getItem('campus_event_token'))
  for (const [method, suffix] of [['POST', ''], ['PUT', `/${id}`], ['DELETE', `/${id}`]]) {
    const result = await context.request.fetch(`${api}/api/events${suffix}`, { method, headers: { Authorization: `Bearer ${studentToken}` }, data: method === 'DELETE' ? undefined : payload })
    assert.equal(result.status(), 403)
    assert.equal((await result.json()).code, 40300)
  }
  checks.push('Student create/update/delete rejected by real backend')
  await page.goto(`${base}/`)
  await page.getByRole('button', { name: '退出登录' }).click()
  await login('admin', 'Admin123456')
  await page.getByRole('button', { name: '活动管理', exact: true }).click()
  await page.waitForURL('**/manage/events')
  checks.push('Admin can enter management')
  assert.deepEqual(errors, [])
  checks.push('No browser page errors')
}

run().then(() => {
  const result = { date: new Date().toISOString(), frontend: base, backend: api, nativeProxy: process.env.PHASE4_NATIVE_PROXY === 'true', passed: true, checks, errors }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result))
}).catch(error => {
  console.error(error)
  process.exitCode = 1
}).finally(async () => { if (browser) await browser.close() })
