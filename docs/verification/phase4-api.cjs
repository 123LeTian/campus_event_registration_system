const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const base = process.env.PHASE4_API || 'http://localhost:8080'
const checks = []
const ids = []
const title = `2026校园科技文化节 API验收 ${Date.now()}`
const date = new Date(Date.now() + 5 * 86400000).toISOString().slice(0, 10)
const payload = { title, description: '校园科技交流与文化展示。', location: '大学生活动中心', startTime: `${date}T14:00:00`, endTime: `${date}T16:00:00`, registrationDeadline: `${date}T12:00:00`, maxParticipants: 3 }
async function request(method, url, token, data) {
  const response = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: data ? JSON.stringify(data) : undefined })
  return { status: response.status, body: await response.json() }
}
async function login(username, password) {
  const result = await request('POST', '/api/auth/login', null, { username, password })
  assert.equal(result.status, 200, `${username} login: ${JSON.stringify(result.body)}`)
  assert.equal(result.body.code, 0)
  return result.body.data
}
async function run() {
  const organizer = await login('organizer1', 'Org123456')
  const student = await login('studentA', 'Stu123456')
  const admin = await login('admin', 'Admin123456')
  checks.push('Three seeded roles login successfully')
  for (const url of ['/api/events', '/api/events/1']) assert.equal((await request('GET', url)).status, 401)
  checks.push('Anonymous list/detail rejected')
  const create = await request('POST', '/api/events', organizer.token, payload)
  assert.equal(create.status, 200)
  const event = create.body.data
  ids.push(event.id)
  assert.equal(event.title, title)
  assert.equal(event.organizerId, organizer.user.id)
  assert.equal(event.organizerName, organizer.user.name)
  assert.equal(event.remainingParticipants, 3)
  assert.equal(event.registrationRate, 0)
  assert.equal(event.displayStatus, 'REGISTRATION_OPEN')
  checks.push('Organizer creates Chinese event; complete computed fields returned')
  for (const [method, url] of [['POST', '/api/events'], ['PUT', `/api/events/${event.id}`], ['DELETE', `/api/events/${event.id}`]]) {
    const result = await request(method, url, student.token, method === 'DELETE' ? undefined : payload)
    assert.equal(result.status, 403)
    assert.equal(result.body.code, 40300)
  }
  checks.push('Student cannot create, edit, delete')
  const updated = { ...payload, title: title + ' 已编辑', location: '图书馆报告厅' }
  assert.equal((await request('PUT', `/api/events/${event.id}`, organizer.token, updated)).status, 200)
  const detail = await request('GET', `/api/events/${event.id}`, student.token)
  assert.equal(detail.body.data.location, updated.location)
  checks.push('Organizer edit persists and student reads updated detail')
  const adminCreated = await request('POST', '/api/events', admin.token, { ...payload, title: title + ' 管理员' })
  assert.equal(adminCreated.status, 200)
  const otherId = adminCreated.body.data.id
  ids.push(otherId)
  assert.equal((await request('PUT', `/api/events/${otherId}`, organizer.token, payload)).status, 403)
  assert.equal((await request('DELETE', `/api/events/${otherId}`, organizer.token)).status, 403)
  const mine = await request('GET', `/api/events?mine=true&keyword=${encodeURIComponent(title)}`, organizer.token)
  assert(mine.body.data.records.some(record => record.id === event.id))
  assert(mine.body.data.records.every(record => record.organizerId === organizer.user.id))
  const all = await request('GET', `/api/events?keyword=${encodeURIComponent(title)}`, admin.token)
  assert.equal(all.body.data.total, 2)
  checks.push('Organizer own-only management; foreign edits/deletes rejected; admin sees both')
  assert.equal((await request('PUT', `/api/events/${event.id}`, admin.token, updated)).status, 200)
  assert.equal((await request('DELETE', `/api/events/${event.id}`, admin.token)).status, 200)
  assert.equal((await request('DELETE', `/api/events/${otherId}`, admin.token)).status, 200)
  const list = await request('GET', `/api/events?keyword=${encodeURIComponent(title)}`, student.token)
  assert.equal(list.body.data.total, 0)
  for (const id of ids) assert.equal((await request('GET', `/api/events/${id}`, admin.token)).status, 404)
  checks.push('Admin edits/deletes others; deleted events absent from list and detail')
  const result = { date: new Date().toISOString(), backend: base, passed: true, eventIds: ids, title, checks }
  fs.writeFileSync(path.join(__dirname, 'phase4-api-results.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result))
}
run().catch(error => { console.error(error); process.exitCode = 1 })
