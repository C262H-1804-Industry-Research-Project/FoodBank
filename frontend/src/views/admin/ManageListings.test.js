import { mount } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import ManageListings from './ManageListings.vue'
import { fetchAdminListings } from '../../api/adminModeration'

it('renders listing collection rates from all API pages and preserves them when filtering', async () => {
  const fetchMock = vi.fn(async url => new Response(JSON.stringify({
    content: url.includes('page=0') ? [{
      listing: { listingId: 1, foodName: 'Milk', donorName: 'Kitchen', category: 'Dairy' },
      status: 'LIVE', quantity: 10, completedRequests: 1, approvedRequests: 3,
    }] : [{
      listing: { listingId: 2, foodName: 'Bread', donorName: 'Bakery', category: 'Bakery' },
      status: 'LIVE', quantity: 4, completedRequests: 0, approvedRequests: 0,
    }],
    page: { totalPages: 2 },
  }), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)
  const listings = await fetchAdminListings()
  const wrapper = mount(ManageListings, { props: { listings } })
  try {
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('th').map(th => th.text())).toContain('Collection rate')
    expect(wrapper.findAll('tbody tr')[0].text()).toContain('1/3')
    expect(wrapper.findAll('tbody tr')[1].text()).toContain('0/0')
    await wrapper.find('select').setValue('Dairy')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.find('tbody').text()).toContain('1/3')
    expect(wrapper.find('tbody').text()).not.toContain('Bread')
  } finally {
    wrapper.unmount()
  }
})
