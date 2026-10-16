import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DonorIndex from './DonorIndex.vue'
import { storeSession } from '../../api/client'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

let wrapper
const records = [
  { historyId: 1, foodName: 'Carrots', category: 'Produce', quantityCollected: 3,
    recipientName: 'Community Kitchen', collectedDatetime: '2026-10-16T12:00', status: 'COMPLETED' },
  { historyId: 2, foodName: 'Bread', category: 'Bakery', quantityCollected: 0,
    recipientName: 'Local Pantry', collectedDatetime: '2026-10-15T12:00', status: 'FAILED' },
]
const response = (body, status = 200) => new Response(JSON.stringify(body), { status })

beforeEach(() => {
  localStorage.clear()
  storeSession({ accessToken: 'test-token', user: { donorId: 2, role: 'donor' } })
})
afterEach(() => wrapper?.unmount())

async function openHistory(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  wrapper = mount(DonorIndex, { global: { stubs: {
    MyListings: true, NewListing: true, ApproveRequests: true, DonorReports: true, ReportList: true,
  } } })
  await wrapper.findAll('.sb-item')[3].trigger('click')
  await flushPromises()
}

describe('donor history API integration', () => {
  it('loads real outcomes with donor credentials and supports combined filters', async () => {
    const fetchMock = vi.fn(async url => response(url.endsWith('/collection-history') ? records
      : url.endsWith('/settings') ? { donorId: 2 } : []))
    await openHistory(fetchMock)
    expect(fetchMock).toHaveBeenCalledWith('/api/donors/2/collection-history', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer test-token' }),
    }))
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Successful (1)')
    expect(wrapper.text()).toContain('Failed (1)')
    await wrapper.findAll('.status-tabs button')[2].trigger('click')
    expect(wrapper.find('tbody').text()).toContain('Bread')
    expect(wrapper.find('tbody').text()).not.toContain('Carrots')
    await wrapper.find('.search-input').setValue('Local Pantry')
    await wrapper.find('select').setValue('Bakery')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.find('tbody').text()).toContain('2026-10-15 12:00')
  })

  it('displays API errors and reloads on retry and subsequent navigation', async () => {
    let fail = true
    const fetchMock = vi.fn(async url => {
      if (url.endsWith('/collection-history')) return fail
        ? response({ error: 'Forbidden' }, 403) : response(records)
      return response(url.endsWith('/settings') ? { donorId: 2 } : [])
    })
    await openHistory(fetchMock)
    expect(wrapper.find('[role="alert"]').text()).toContain('Forbidden')
    expect(wrapper.text()).not.toContain('No donation records')
    fail = false
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    await wrapper.findAll('.sb-item')[0].trigger('click')
    await wrapper.findAll('.sb-item')[3].trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/collection-history'))).toHaveLength(3)
  })
})
