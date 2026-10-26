import { mount, flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import AccountManagement from './AccountManagement.vue'

it('renders donor and recipient collection rates from the accounts API', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({
    content: [
      { userId: 1, name: 'Donor kitchen', role: 'DONOR', status: 'ACTIVE', completedCollections: 2, totalRequests: 6 },
      { userId: 2, name: 'Recipient pantry', role: 'RECIPIENT', status: 'ACTIVE', completedCollections: 3, totalRequests: 7 },
      { userId: 3, name: 'New account', role: 'DONOR', status: 'ACTIVE', completedCollections: 0, totalRequests: 0 },
    ], page: { totalPages: 1, totalElements: 3 },
  }), { status: 200 })))
  const wrapper = mount(AccountManagement)
  try {
    await flushPromises()
    expect(wrapper.findAll('th').map(cell => cell.text())).toContain('Collection rate')
    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('2/6')
    expect(rows[1].text()).toContain('3/7')
    expect(rows[2].text()).toContain('0/0')
  } finally {
    wrapper.unmount()
  }
})
