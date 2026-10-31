import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import NewListing from './NewListing.vue'
import MyListings from './MyListings.vue'

const api = vi.hoisted(() => ({
  getListingCatalogue: vi.fn(),
  createDonorListing: vi.fn(),
  getDonorListingsPage: vi.fn(),
  updateDonorListing: vi.fn(),
  deleteDonorListing: vi.fn(),
  uploadPhoto: vi.fn(),
}))
vi.mock('../../api/client', () => api)

const listing = {
  listingId: 12, name: 'Bread', category: 'Bakery', categoryId: 1,
  quantity: 10, maxDailyCapacity: 4, description: '', status: 'LIVE',
  addressLine: '1 Test Street', suburb: 'Adelaide', region: 'SA',
  slotCapacity: 1, isAutoApproved: false, usePickupCode: false,
  hideContact: false, tags: [], timeslots: [],
}
let wrapper

beforeEach(() => {
  Object.values(api).forEach(mock => mock.mockReset())
  api.getListingCatalogue.mockResolvedValue({ categories: [{ id: 1, name: 'Bakery' }], tags: [] })
  api.getDonorListingsPage.mockResolvedValue({ items: [{ ...listing }], totalElements: 1, totalPages: 1 })
  api.createDonorListing.mockResolvedValue({ listingId: 13, status: 'LIVE' })
  api.updateDonorListing.mockResolvedValue({ ...listing })
})
afterEach(() => wrapper?.unmount())

async function openForm(editing) {
  wrapper = mount(editing ? MyListings : NewListing, {
    props: { donorId: 5 }, global: { stubs: { AddressAutocomplete: true } },
  })
  await flushPromises()
  if (editing) {
    await button('Edit').trigger('click')
    await flushPromises()
  } else {
    await wrapper.get('input[placeholder="e.g. Sourdough loaves"]').setValue('Bread')
    await wrapper.get('input[placeholder="e.g. 12"]').setValue('10')
  }
}

function button(label) {
  return wrapper.findAll('button').find(item => item.text() === label)
}

async function submit(editing, label = 'Publish listing') {
  if (editing) {
    await button(label).trigger('click')
  } else {
    await button(label).trigger('click')
    await wrapper.get('form').trigger('submit')
  }
  await flushPromises()
}

describe.each([false, true])('listing description (editing=%s)', editing => {
  it.each([300, 1000, 2000])('shows the limit and submits a %i-character description', async length => {
    await openForm(editing)
    const description = 'a'.repeat(length)
    await wrapper.get('textarea').setValue(description)
    expect(wrapper.text()).toContain(`${length} / 2000 characters`)
    expect(wrapper.get('textarea').attributes('aria-invalid')).toBe('false')

    await submit(editing)

    if (editing) {
      expect(api.updateDonorListing).toHaveBeenCalledWith(5, 12, expect.objectContaining({ description }))
    } else {
      expect(api.createDonorListing).toHaveBeenCalledWith(5, expect.objectContaining({ description }))
      expect(wrapper.emitted('add-listing')).toHaveLength(1)
    }
  })

  it.each(['Publish listing', 'Save as draft'])('blocks overlong descriptions for %s with a length error', async label => {
    await openForm(editing)
    await wrapper.get('textarea').setValue('a'.repeat(2001))
    expect(wrapper.text()).toContain('2001 / 2000 characters')
    expect(wrapper.get('textarea').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('Storage instructions / description must be 2000 characters or fewer.')

    await submit(editing, label)

    expect(api.createDonorListing).not.toHaveBeenCalled()
    expect(api.updateDonorListing).not.toHaveBeenCalled()
    expect(api.uploadPhoto).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Unauthorized')
    expect(wrapper.text()).not.toContain('Unauthorised')
  })

  it('allows submission after correcting an overlong description', async () => {
    await openForm(editing)
    await wrapper.get('textarea').setValue('a'.repeat(2001))
    await submit(editing)
    await wrapper.get('textarea').setValue('Keep refrigerated.')
    expect(wrapper.get('textarea').attributes('aria-invalid')).toBe('false')
    await submit(editing)
    expect(editing ? api.updateDonorListing : api.createDonorListing).toHaveBeenCalledOnce()
  })
})
