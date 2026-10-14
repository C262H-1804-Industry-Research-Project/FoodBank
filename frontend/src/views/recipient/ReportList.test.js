import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ReportList from './ReportList.vue'

// Page-level tests: the real ReportList and API client, with only fetch replaced.

const report = {
  reportId: 55, listingName: 'Sourdough loaves', donorName: 'Green Leaf Café', reason: 'Food quality issue',
  details: '  Mouldy  ', status: 'IN_REVIEW', createdAt: '2026-10-15T12:30:45', evidenceImageUrl: '/uploads/mould.png',
}
const response = (data, status = 200) => new Response(JSON.stringify(data), { status })
const page = (content, totalElements = content.length) =>
  response({ content, page: { size: 10, number: 0, totalElements, totalPages: Math.ceil(totalElements / 10) } })
let wrapper

beforeEach(() => { localStorage.clear(); wrapper = null })
afterEach(() => { wrapper?.unmount(); vi.unstubAllGlobals(); vi.useRealTimers() })

function mockApi(reports = () => page([report])) {
  const fetchMock = vi.fn(async url => {
    if (url === '/api/reports/statuses') return response(['PENDING', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'])
    if (url.startsWith('/api/recipients/me/reports')) return reports()
    throw new Error(`Unexpected request ${url}`)
  })
  vi.stubGlobal('fetch', fetchMock); return fetchMock
}
const reportCalls = fetchMock => fetchMock.mock.calls.map(([url]) => url).filter(url => url.startsWith('/api/recipients/me/reports'))

it('shows the recipient\'s reports from the API', async () => {
  const fetchMock = mockApi()
  wrapper = mount(ReportList); await flushPromises()
  expect(reportCalls(fetchMock)).toEqual(['/api/recipients/me/reports?page=0&size=10'])
  const cells = wrapper.findAll('tbody td').map(td => td.text())
  expect(cells[0]).toBe('Sourdough loaves — Green Leaf Café')
  expect(cells[1]).toBe('Food quality issue')
  expect(wrapper.get('.attach-thumb').attributes('src')).toBe('/uploads/mould.png')
  expect(cells[3]).toBe('Mouldy')
  expect(cells[4]).toBe('2026-10-15 12:30')
  expect(cells[5]).toBe('Being Reviewed')
  expect(wrapper.text()).toContain('of 1 reports')
  expect(wrapper.findAll('select option').map(o => o.text())).toEqual(['All statuses', 'Under Review', 'Being Reviewed', 'Resolved', 'Dismissed'])
})

it('sends status, search and page to the server', async () => {
  vi.useFakeTimers()
  const fetchMock = mockApi(() => page([report], 25))
  wrapper = mount(ReportList); await flushPromises()
  await wrapper.findAll('.page-number-btn')[1].trigger('click'); await flushPromises()
  expect(reportCalls(fetchMock).at(-1)).toBe('/api/recipients/me/reports?page=1&size=10')
  await wrapper.get('select').setValue('RESOLVED'); await flushPromises()
  expect(reportCalls(fetchMock).at(-1)).toBe('/api/recipients/me/reports?page=0&size=10&status=RESOLVED')
  await wrapper.get('.search-input').setValue(' mould ')
  vi.advanceTimersByTime(300); await flushPromises()
  expect(reportCalls(fetchMock).at(-1)).toBe('/api/recipients/me/reports?page=0&size=10&status=RESOLVED&q=mould')
})

it('shows an empty message when there are no reports', async () => {
  mockApi(() => page([]))
  wrapper = mount(ReportList); await flushPromises()
  expect(wrapper.get('.empty-note').text()).toBe('You have not submitted any reports yet.')
})

it('shows a load error and retries', async () => {
  let fail = true
  mockApi(() => fail ? response({ error: 'Server down' }, 500) : page([report]))
  wrapper = mount(ReportList); await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('Server down')
  fail = false
  await wrapper.get('.btn-link').trigger('click'); await flushPromises()
  expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  expect(wrapper.findAll('tbody tr')).toHaveLength(1)
})
