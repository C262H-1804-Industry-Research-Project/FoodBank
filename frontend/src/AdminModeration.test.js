import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MisuseReports from './views/admin/MisuseReports.vue'
import AdminIndex from './views/admin/AdminIndex.vue'
import { fetchAdminListings } from './api/adminModeration'
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
const report = { reportId: 7, reportedId: 9, reporterId: 2, reportedName: 'Recipient', reporterName: 'Donor', foodName: 'Bread', status: 'PENDING', reason: 'No show', banCount: 0 }
const response = (data, status = 200) => new Response(JSON.stringify(data), { status })
let wrapper
beforeEach(() => {
  localStorage.clear(); wrapper = null
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  vi.spyOn(window, 'alert').mockImplementation(() => {})
})
afterEach(() => { wrapper?.unmount(); vi.unstubAllGlobals() })
function mockApi(process = async body => ({ ...report, ...body })) {
  const fetchMock = vi.fn(async (url, options = {}) => {
    if (url === '/api/settings/ban-days') return response(options.body ? JSON.parse(options.body) : { first: 3, second: 7, third: 30 })
    if (url === '/api/admin/reports') return response([report])
    return response(await process(JSON.parse(options.body)))
  })
  vi.stubGlobal('fetch', fetchMock); return fetchMock
}
async function openReport() {
  wrapper = mount(MisuseReports); await flushPromises()
  await wrapper.get('.btn-process').trigger('click')
}
it('loads real reports and posts temporary ban with policy duration', async () => {
  const fetchMock = mockApi(); await openReport()
  expect(wrapper.text()).toContain('Recipient')
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/admin/reports/7')
  expect(JSON.parse(options.body)).toMatchObject({ status: 'RESOLVED', action: 'BAN', banDays: 3 })
  expect(wrapper.get('.modal-backdrop').classes()).not.toContain('show')
})
it.each([['warning','WARNING','RESOLVED'],['deactivate','DEACTIVATE','RESOLVED'],['review','REVIEW','IN_REVIEW']])('posts %s as real report action', async (value, action, status) => {
  const fetchMock = mockApi(); await openReport()
  await wrapper.get('.modal-card select').setValue(value)
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/admin/reports/7')
  expect(JSON.parse(options.body)).toMatchObject({ action, status })
})
it('retains process form and pending row when server rejects action', async () => {
  mockApi(async () => { throw new Error('Offline') }); await openReport()
  await wrapper.get('textarea').setValue('Keep note')
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  expect(wrapper.find('.modal-card').exists()).toBe(true)
  expect(wrapper.get('textarea').element.value).toBe('Keep note')
  expect(wrapper.text()).toContain('Cannot connect')
  expect(wrapper.text()).toContain('Pending')
})
it('blocks invalid duration without sending process request', async () => {
  const fetchMock = mockApi(); await openReport()
  await wrapper.get('.modal-card input').setValue('0')
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  expect(fetchMock.mock.calls.some(([url]) => url === '/api/admin/reports/7')).toBe(false)
})
it('confirms dismissal and persists policy', async () => {
  const fetchMock = mockApi(); wrapper = mount(MisuseReports); await flushPromises()
  await wrapper.get('.btn-dismiss').trigger('click'); await flushPromises()
  expect(JSON.parse(fetchMock.mock.calls.find(([url]) => url === '/api/admin/reports/7')[1].body)).toMatchObject({ status: 'DISMISSED', action: 'DISMISS' })
  await wrapper.get('.pw-card input').setValue('5'); await wrapper.get('.btn-save-policy').trigger('click'); await flushPromises()
  expect(JSON.parse(fetchMock.mock.calls.find(([url, opt]) => url === '/api/settings/ban-days' && opt.method === 'PUT')[1].body).first).toBe(5)
})
it('blocks duplicate processing while waiting', async () => {
  let resolve
  const fetchMock = mockApi(() => new Promise(done => { resolve = done })); await openReport()
  await wrapper.get('.btn-danger-confirm').trigger('click'); await wrapper.get('.btn-danger-confirm').trigger('click')
  expect(fetchMock.mock.calls.filter(([url]) => url === '/api/admin/reports/7')).toHaveLength(1)
  resolve({ ...report, status: 'RESOLVED' }); await flushPromises()
})
it('retrieves every server page with real listing fields', async () => {
  vi.stubGlobal('fetch', vi.fn(async url => response({ content: [{ listing: { listingId: url.includes('page=0') ? 1 : 2, foodName: 'Food', timeslots: [], tags: [] }, status: 'LIVE', quantity: 4 }], page: { totalPages: 2 } })))
  const rows = await fetchAdminListings()
  expect(rows.map(row => row.id)).toEqual([1, 2]); expect(rows[0].quantity).toBe('4 units')
})
it('keeps listing on failed ban and marks successful ban', async () => {
  let fail = true
  const listing = { listing: { listingId: 1, foodName: 'Real food', donorName: 'Donor', timeslots: [], tags: [] }, status: 'LIVE', quantity: 4 }
  vi.stubGlobal('fetch', vi.fn(async url => {
    if (url.includes('/ban')) return fail ? response({ error: 'Denied' },403) : response({ status: 'BANNED' })
    return response({ content: [listing], page: { totalPages: 1 } })
  }))
  wrapper = mount(AdminIndex, { global: { stubs: { AccountManagement: true, MisuseReports: true } } }); await flushPromises()
  await wrapper.get('.btn-ban').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('Real food'); expect(window.alert).toHaveBeenCalledWith('Denied')
  fail = false; listing.status = 'BANNED'
  await wrapper.get('.btn-ban').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('BANNED'); expect(wrapper.get('.btn-ban').element.disabled).toBe(true)
})

it('shows load failure and retries real reports without demo rows', async () => {
  let fail = true
  vi.stubGlobal('fetch', vi.fn(async url => {
    if (url === '/api/settings/ban-days') return response({ first: 3, second: 7, third: 30 })
    return fail ? response({ error: 'Load failed' },500) : response([report])
  }))
  wrapper = mount(MisuseReports); await flushPromises()
  expect(wrapper.text()).toContain('Load failed'); expect(wrapper.text()).not.toContain('Community Kitchen East')
  fail = false
  await wrapper.get('[role="alert"] button').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('Recipient'); expect(wrapper.text()).not.toContain('Load failed')
})
it('does not send dismissal when confirmation is cancelled', async () => {
  const fetchMock = mockApi(); wrapper = mount(MisuseReports); await flushPromises()
  window.confirm.mockReturnValue(false)
  await wrapper.get('.btn-dismiss').trigger('click'); await flushPromises()
  expect(fetchMock.mock.calls.some(([url]) => url === '/api/admin/reports/7')).toBe(false)
})
it('loads real donor and recipient accounts into user management', async () => {
  const fetchMock = vi.fn(async url => {
    if (url === '/api/donors') return response([{ donorId: 1, userId: 7, organisation: 'Green Leaf Café', email: 'green@cafe.com', phone: '0400', suburb: 'Norwood', region: 'SA', status: 'ACTIVE' }])
    if (url === '/api/recipients') return response([{ recipientId: 2, userId: 8, recipient: 'Kitchen East', email: 'k@east.org', phone: null, status: 'BANNED' }])
    return response({ content: [], page: { totalPages: 0 } })
  })
  vi.stubGlobal('fetch', fetchMock)
  wrapper = mount(AdminIndex); await flushPromises()
  await wrapper.findAll('.sb-item').find(item => item.text() === 'User management').trigger('click'); await flushPromises()
  const rows = wrapper.findAll('tbody tr')
  expect(rows).toHaveLength(2)
  expect(rows[0].text()).toContain('Green Leaf Café'); expect(rows[0].text()).toContain('green@cafe.com'); expect(rows[0].text()).toContain('Active')
  expect(rows[0].get('.user-org').text()).toBe('Norwood, SA'); expect(rows[1].find('.user-org').exists()).toBe(false)
  expect(rows[1].text()).toContain('Kitchen East'); expect(rows[1].text()).toContain('Deactivated'); expect(rows[1].text()).toContain('—')
})
function mockAccounts(recipientStatus) {
  let status = recipientStatus
  const fetchMock = vi.fn(async (url, options = {}) => {
    if (url === '/api/donors') return response([])
    if (url === '/api/recipients') return response([{ recipientId: 2, userId: 8, recipient: 'Kitchen East', email: 'k@east.org', status }])
    if (url === '/api/admin/users/8/ban') { status = 'BANNED'; return response({ userStatus: 'BANNED' }, 201) }
    if (url === '/api/admin/users/8/unban') { status = 'ACTIVE'; return response({ userStatus: 'ACTIVE' }) }
    return response({ content: [], page: { totalPages: 0 } })
  })
  vi.stubGlobal('fetch', fetchMock); return fetchMock
}
async function openUsers() {
  wrapper = mount(AdminIndex); await flushPromises()
  await wrapper.findAll('.sb-item').find(item => item.text() === 'User management').trigger('click'); await flushPromises()
}
it('deactivates an account permanently with a reason from the modal', async () => {
  const fetchMock = mockAccounts('ACTIVE'); await openUsers()
  await wrapper.get('.btn-deactivate').trigger('click')
  expect(wrapper.get('.modal-backdrop').classes()).toContain('show')
  expect(wrapper.get('.btn-danger-confirm').attributes('disabled')).toBeDefined()
  await wrapper.get('.modal-card textarea').setValue('Repeated no-shows')
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/admin/users/8/ban')
  expect(JSON.parse(options.body)).toEqual({ reason: 'Repeated no-shows', banType: 'PERMANENT' })
  expect(wrapper.get('.modal-backdrop').classes()).not.toContain('show')
  expect(wrapper.get('tbody tr').text()).toContain('Deactivated')
})
it('keeps the deactivate modal open when the request fails', async () => {
  vi.stubGlobal('fetch', vi.fn(async url => {
    if (url === '/api/recipients') return response([{ userId: 8, recipient: 'Kitchen East', status: 'ACTIVE' }])
    if (url === '/api/admin/users/8/ban') return response({ error: 'User is already banned' }, 409)
    return response(url === '/api/donors' ? [] : { content: [], page: { totalPages: 0 } })
  }))
  await openUsers()
  await wrapper.get('.btn-deactivate').trigger('click')
  await wrapper.get('.modal-card textarea').setValue('Fraud')
  await wrapper.get('.btn-danger-confirm').trigger('click'); await flushPromises()
  expect(window.alert).toHaveBeenCalled()
  expect(wrapper.get('.modal-backdrop').classes()).toContain('show')
})
it('reactivates a deactivated account', async () => {
  const fetchMock = mockAccounts('BANNED'); await openUsers()
  await wrapper.get('.btn-reactivate').trigger('click'); await flushPromises()
  expect(fetchMock.mock.calls.some(([url, options]) => url === '/api/admin/users/8/unban' && options.method === 'POST')).toBe(true)
  expect(wrapper.get('tbody tr').text()).toContain('Active')
})
