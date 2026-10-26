import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// The real page and adapter, with only the API client replaced by an in-memory donor
let stored
let api

const listing = id => ({ listingId: id, name: `Listing ${id}`, status: 'LIVE', timeslots: [] })

async function mountPage(count) {
  vi.resetModules()
  // Newest first, like the backend
  stored = Array.from({ length: count }, (_, index) => listing(count - index))
  api = {
    getDonorListingsPage: vi.fn(async (donorId, { page, size }) => ({
      items: stored.slice(page * size, page * size + size),
      totalElements: stored.length,
      totalPages: Math.ceil(stored.length / size),
    })),
    deleteDonorListing: vi.fn(async (donorId, id) => { stored = stored.filter(row => row.listingId !== id) }),
  }
  vi.doMock('../../api/client', async importOriginal => ({ ...(await importOriginal()), ...api }))
  const { default: MyListings } = await import('./MyListings.vue')
  const wrapper = mount(MyListings, { props: { donorId: 5 } })
  await flushPromises()
  return wrapper
}

const names = wrapper => wrapper.findAll('tbody tr td:first-child').map(cell => cell.text())

beforeEach(() => vi.spyOn(window, 'confirm').mockReturnValue(true))
afterEach(() => vi.restoreAllMocks())

describe('My listings server pagination', () => {
  it('asks the API for one page at a time', async () => {
    const wrapper = await mountPage(7)
    expect(api.getDonorListingsPage).toHaveBeenLastCalledWith(5, { page: 0, size: 5 }, expect.anything())
    expect(names(wrapper)).toEqual(['Listing 7', 'Listing 6', 'Listing 5', 'Listing 4', 'Listing 3'])
    expect(wrapper.get('.pagination-info').text()).toBe('Showing 1–5 of 7 listings')

    await wrapper.findAll('.page-btn')[1].trigger('click')
    await flushPromises()
    expect(api.getDonorListingsPage).toHaveBeenLastCalledWith(5, { page: 1, size: 5 }, expect.anything())
    expect(names(wrapper)).toEqual(['Listing 2', 'Listing 1'])
    expect(wrapper.get('.pagination-info').text()).toBe('Showing 6–7 of 7 listings')
    expect(wrapper.findAll('.page-btn')[1].element.disabled).toBe(true)
  })

  it('moves back a page after cancelling the only listing on the last page', async () => {
    const wrapper = await mountPage(6)
    await wrapper.findAll('.page-number-btn')[1].trigger('click')
    await flushPromises()
    expect(names(wrapper)).toEqual(['Listing 1'])

    await wrapper.get('.btn-danger').trigger('click')
    await flushPromises()
    expect(api.deleteDonorListing).toHaveBeenCalledWith(5, 1)
    expect(wrapper.get('.page-number-btn.active').text()).toBe('1')
    expect(names(wrapper)).toHaveLength(5)
  })

  it('shows the empty message and no pagination when the donor has no listings', async () => {
    const wrapper = await mountPage(0)
    expect(wrapper.find('.pagination-bar').exists()).toBe(false)
    expect(wrapper.text()).toContain('No active or pending listings found')
  })

  it('shows a retry option when the page fails to load', async () => {
    vi.resetModules()
    vi.doMock('../../api/client', async importOriginal => ({
      ...(await importOriginal()),
      getDonorListingsPage: vi.fn(async () => { throw new Error('Server unavailable') }),
    }))
    const { default: MyListings } = await import('./MyListings.vue')
    const wrapper = mount(MyListings, { props: { donorId: 5 } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('Server unavailable')
    expect(wrapper.text()).not.toContain('No active or pending listings found')
  })
})
