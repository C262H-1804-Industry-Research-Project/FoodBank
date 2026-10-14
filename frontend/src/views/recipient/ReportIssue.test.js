import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ReportIssue from './ReportIssue.vue'

// Page-level tests: the real ReportIssue and API client, with only fetch replaced.

const collection = {
  requestId: 12, listingId: 4, listingName: 'Sourdough loaves', donorName: 'Green Leaf Café',
  timeslotStart: '2026-10-15T10:00', timeslotEnd: '2026-10-15T11:00', quantityRequested: 2,
}
const response = (data, status = 200) => new Response(JSON.stringify(data), { status })
let wrapper

beforeEach(() => {
  localStorage.clear(); wrapper = null
  vi.spyOn(window, 'alert').mockImplementation(() => {})
})
afterEach(() => { wrapper?.unmount(); vi.unstubAllGlobals(); vi.restoreAllMocks() })

function mockApi({ reportable = () => response([collection]), report = () => response({ reportId: 55, status: 'PENDING', createdAt: '2026-10-15T12:30:00' }, 201) } = {}) {
  const fetchMock = vi.fn(async url => {
    if (url === '/api/requests/reportable') return reportable()
    if (url === '/api/uploads/photo') return response({ url: '/uploads/evidence.png' })
    if (url === '/api/reports') return report()
    throw new Error(`Unexpected request ${url}`)
  })
  vi.stubGlobal('fetch', fetchMock); return fetchMock
}

async function mountPage() {
  wrapper = mount(ReportIssue); await flushPromises()
}

async function fillForm() {
  await wrapper.get('select').setValue(12)
  await wrapper.findAll('select')[1].setValue('Food quality issue')
  await wrapper.get('textarea').setValue('  Mouldy at the bottom  ')
}

async function attachPhoto(file) {
  const input = wrapper.get('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await vi.waitFor(() => expect(wrapper.find('.upload-file-name').exists()).toBe(true))
}

it('lists approved collections from the API', async () => {
  mockApi(); await mountPage()
  const options = wrapper.findAll('select')[0].findAll('option')
  expect(options).toHaveLength(2)
  expect(options[1].text()).toBe('Sourdough loaves — Green Leaf Café, 2026-10-15: 10:00 - 11:00')
})

it('uploads the photo and submits a FOOD_QUALITY report for the chosen collection', async () => {
  const fetchMock = mockApi(); await mountPage()
  await fillForm()
  await attachPhoto(new File(['img'], 'mould.png', { type: 'image/png' }))
  await wrapper.get('form').trigger('submit'); await flushPromises()

  const urls = fetchMock.mock.calls.map(([url]) => url)
  expect(urls.indexOf('/api/uploads/photo')).toBeLessThan(urls.indexOf('/api/reports'))
  const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/reports')
  expect(JSON.parse(options.body)).toEqual({
    reportType: 'FOOD_QUALITY', listingId: 4, pickupRequestId: 12, reason: 'Food quality issue',
    details: 'Mouldy at the bottom', evidenceImageUrl: '/uploads/evidence.png',
  })
  const [submitted] = wrapper.emitted('report-submitted')[0]
  expect(submitted).toMatchObject({ id: 55, reason: 'Food quality issue', submittedAt: '2026-10-15 12:30', photo: { url: '/uploads/evidence.png' } })
  expect(wrapper.get('select').element.value).toBe('')
})

it('submits without a photo when none is attached', async () => {
  const fetchMock = mockApi(); await mountPage()
  await fillForm()
  await wrapper.get('form').trigger('submit'); await flushPromises()
  expect(fetchMock.mock.calls.some(([url]) => url === '/api/uploads/photo')).toBe(false)
  const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/reports')
  expect(JSON.parse(options.body).evidenceImageUrl).toBeNull()
})

it('keeps the form filled in and shows the error when the server rejects the report', async () => {
  mockApi({ report: () => response({ error: 'Only approved pickup requests can be reported' }, 409) }); await mountPage()
  await fillForm()
  await wrapper.get('form').trigger('submit'); await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toBe('Only approved pickup requests can be reported')
  expect(wrapper.get('textarea').element.value).toBe('  Mouldy at the bottom  ')
  expect(wrapper.emitted('report-submitted')).toBeUndefined()
})

it('rejects photos over 5MB', async () => {
  mockApi(); await mountPage()
  const input = wrapper.get('input[type="file"]')
  const big = new File(['x'], 'big.png', { type: 'image/png' })
  Object.defineProperty(big, 'size', { value: 5 * 1024 * 1024 + 1 })
  Object.defineProperty(input.element, 'files', { value: [big], configurable: true })
  await input.trigger('change')
  expect(wrapper.get('[role="alert"]').text()).toBe('Photo must be 5MB or smaller.')
  expect(wrapper.find('.upload-file-name').exists()).toBe(false)
})

it('shows a load error with retry, and a hint when there is nothing to report', async () => {
  let fail = true
  mockApi({ reportable: () => fail ? response({ error: 'Server down' }, 500) : response([]) }); await mountPage()
  expect(wrapper.get('[role="alert"]').text()).toContain('Server down')
  fail = false
  await wrapper.get('.btn-link').trigger('click'); await flushPromises()
  expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  expect(wrapper.text()).toContain('You have no approved collections to report yet.')
})
