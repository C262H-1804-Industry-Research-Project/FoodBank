import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// The real form, with only the API client replaced
let api

async function mountForm() {
  vi.resetModules()
  api = {
    getListingCatalogue: vi.fn(async () => ({ categories: [{ id: 1, name: 'Bakery' }], tags: [] })),
    createDonorListing: vi.fn(async (donorId, request) => ({ listingId: 9, status: request.status })),
  }
  vi.doMock('../../api/client', async importOriginal => ({ ...(await importOriginal()), ...api }))
  const { default: NewListing } = await import('./NewListing.vue')
  const wrapper = mount(NewListing, { props: { donorId: 5 }, attachTo: document.body })
  await flushPromises()
  await wrapper.get('input[placeholder="e.g. Sourdough loaves"]').setValue('Bread')
  await wrapper.get('input[placeholder="e.g. 12"]').setValue('10')
  return wrapper
}

const button = (wrapper, text) => wrapper.findAll('button').find(b => b.text() === text)

async function addSlot(wrapper) {
  const [startTime, endTime] = wrapper.findAll('.timeslot-form input[type="time"]')
  await wrapper.get('.timeslot-form input[type="date"]').setValue('2099-01-01')
  await startTime.setValue('10:00')
  await endTime.setValue('11:00')
}

afterEach(() => {
  vi.restoreAllMocks()
  document.body.innerHTML = ''
})

describe('New listing pickup timeslots', () => {
  it('blocks publishing without a timeslot and says why', async () => {
    const wrapper = await mountForm()
    await button(wrapper, 'Publish listing').trigger('click')
    await flushPromises()
    expect(api.createDonorListing).not.toHaveBeenCalled()
    expect(wrapper.get('.timeslot-error').text()).toContain('Add at least one pickup timeslot before publishing')
    expect(wrapper.get('.submit-message.error').text()).toContain('Add at least one pickup timeslot before publishing')
  })

  it("reminds the donor to click 'Add timeslot' when a slot was entered but not added", async () => {
    const wrapper = await mountForm()
    await addSlot(wrapper)
    await button(wrapper, 'Publish listing').trigger('click')
    await flushPromises()
    expect(api.createDonorListing).not.toHaveBeenCalled()
    expect(wrapper.get('.timeslot-error').text()).toContain("Click 'Add timeslot' before publishing")
  })

  it('publishes once a timeslot is added', async () => {
    const wrapper = await mountForm()
    await addSlot(wrapper)
    await button(wrapper, 'Add timeslot').trigger('click')
    await button(wrapper, 'Publish listing').trigger('click')
    await flushPromises()
    expect(api.createDonorListing).toHaveBeenCalledTimes(1)
    expect(api.createDonorListing.mock.calls[0][1]).toMatchObject({
      status: 'LIVE',
      timeslots: [{ startTime: '2099-01-01T10:00', endTime: '2099-01-01T11:00' }],
    })
  })

  it('still saves a draft without a timeslot', async () => {
    const wrapper = await mountForm()
    await button(wrapper, 'Save as draft').trigger('click')
    await flushPromises()
    expect(api.createDonorListing).toHaveBeenCalledTimes(1)
    expect(api.createDonorListing.mock.calls[0][1]).toMatchObject({ status: 'DRAFT', timeslots: [] })
  })
})
