import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Page-level tests: the real BrowseDonations, SuburbPicker and listing adapter,
// with only the network layer (API client and Mapbox) replaced.

const ADELAIDE = { addressLine: '1 King William St', suburb: 'Adelaide', region: 'SA', latitude: -34.9285, longitude: 138.6007 }

const listingRow = (id, name, distanceKm) => ({
  listingId: id, foodName: name, category: 'Bakery', suburb: 'Glenelg', region: 'SA', distanceKm, timeslots: [],
})

let api

async function mountPage({ role = 'recipient', savedLocation = null, locationError = null, lookupEnabled = true, rows = [] } = {}) {
  vi.resetModules()
  api = {
    getStoredSession: vi.fn(() => (role ? { accessToken: 'abc', user: { role } } : null)),
    getMyLocation: vi.fn(async () => {
      if (locationError) throw locationError
      return savedLocation
    }),
    saveMyLocation: vi.fn(async location => location),
    clearMyLocation: vi.fn(async () => null),
    getListingCatalogue: vi.fn(async () => ({ categories: [], tags: [] })),
    getListingsPage: vi.fn(async () => ({ items: rows, totalElements: rows.length, totalPages: rows.length ? 1 : 0 })),
  }
  vi.doMock('../../api/client', async importOriginal => ({ ...(await importOriginal()), ...api }))
  vi.doMock('../../api/mapbox', async importOriginal => ({
    ...(await importOriginal()),
    isAddressLookupEnabled: lookupEnabled,
    suggestAddresses: vi.fn(async () => []),
  }))
  const { default: BrowseDonations } = await import('./BrowseDonations.vue')
  const { default: AddressAutocomplete } = await import('../../components/AddressAutocomplete.vue')
  const { default: SuburbPicker } = await import('../../components/SuburbPicker.vue')
  const wrapper = mount(BrowseDonations)
  await flushPromises()
  return { wrapper, AddressAutocomplete, SuburbPicker }
}

// The filters passed to the most recent listings request
const lastListingsQuery = () => api.getListingsPage.mock.calls.at(-1)[0]

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true })
})

afterEach(() => {
  vi.useRealTimers()
  vi.doUnmock('../../api/client')
  vi.doUnmock('../../api/mapbox')
})

describe('BrowseDonations distance', () => {
  it('restores the saved location and loads listings from there', async () => {
    const { wrapper } = await mountPage({ savedLocation: ADELAIDE, rows: [listingRow(1, 'Bread', 9.84)] })

    expect(api.getMyLocation).toHaveBeenCalledTimes(1)
    // Loaded once, already with the saved coordinates (no first request without them)
    expect(api.getListingsPage).toHaveBeenCalledTimes(1)
    expect(lastListingsQuery()).toMatchObject({ latitude: -34.9285, longitude: 138.6007 })
    expect(wrapper.find('.lb-address').text()).toBe('1 King William St, Adelaide, SA')
    expect(wrapper.find('.lc-distance').text()).toContain('9.8 km away')
  })

  it('loads listings without a location when none is saved', async () => {
    const { wrapper } = await mountPage({ rows: [listingRow(1, 'Bread', null)] })

    expect(api.getListingsPage).toHaveBeenCalledTimes(1)
    expect(lastListingsQuery().latitude).toBeUndefined()
    expect(wrapper.find('.lc-distance').exists()).toBe(false)
    expect(wrapper.find('[aria-label="Your location"]').exists()).toBe(true)
  })

  it('still loads listings when the saved location cannot be fetched', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    const { wrapper } = await mountPage({ locationError: new Error('offline'), rows: [listingRow(1, 'Bread', null)] })

    expect(api.getListingsPage).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.lc-name').text()).toBe('Bread')
  })

  it('does not look up a saved location for donors or signed-out visitors', async () => {
    await mountPage({ role: 'donor' })
    expect(api.getMyLocation).not.toHaveBeenCalled()

    await mountPage({ role: null })
    expect(api.getMyLocation).not.toHaveBeenCalled()
    expect(api.getListingsPage).toHaveBeenCalledTimes(1)
  })

  it('saves a chosen address and reloads listings from it', async () => {
    const { wrapper, AddressAutocomplete } = await mountPage()

    wrapper.findComponent(AddressAutocomplete).vm.$emit('select', {
      label: '1 King William St, Adelaide SA 5000',
      addressLine: '1 King William St', suburb: 'Adelaide', stateRegion: 'SA',
      latitude: -34.9285, longitude: 138.6007,
    })
    await flushPromises()

    expect(api.saveMyLocation).toHaveBeenCalledWith(ADELAIDE)
    expect(lastListingsQuery()).toMatchObject({ latitude: -34.9285, longitude: 138.6007, page: 0 })
    expect(wrapper.find('.lb-address').text()).toBe('1 King William St, Adelaide SA 5000')
  })

  it('does not save the location for visitors who are not recipients', async () => {
    const { wrapper, AddressAutocomplete } = await mountPage({ role: null })

    wrapper.findComponent(AddressAutocomplete).vm.$emit('select', {
      addressLine: '1 King William St', suburb: 'Adelaide', stateRegion: 'SA', latitude: -34.9, longitude: 138.6,
    })
    await flushPromises()

    expect(api.saveMyLocation).not.toHaveBeenCalled()
    expect(lastListingsQuery()).toMatchObject({ latitude: -34.9, longitude: 138.6 })
  })

  it('clears the saved location and drops distances', async () => {
    const { wrapper } = await mountPage({ savedLocation: ADELAIDE })

    const clearButton = wrapper.findAll('.lb-link').find(button => button.text() === 'Clear')
    await clearButton.trigger('click')
    await flushPromises()

    expect(api.clearMyLocation).toHaveBeenCalledTimes(1)
    expect(lastListingsQuery().latitude).toBeUndefined()
  })

  it('hides the location box without a Mapbox token', async () => {
    const { wrapper } = await mountPage({ lookupEnabled: false })

    expect(wrapper.find('.location-bar').exists()).toBe(false)
  })
})

describe('BrowseDonations suburb filter', () => {
  it('filters listings by the selected suburb pills', async () => {
    const { wrapper, SuburbPicker } = await mountPage()

    wrapper.findComponent(SuburbPicker).vm.$emit('update:modelValue', [
      { suburb: 'Glenelg', region: 'SA' }, { suburb: 'Richmond', region: 'VIC' },
    ])
    await flushPromises()

    expect(lastListingsQuery().suburbs).toEqual([
      { suburb: 'Glenelg', region: 'SA' }, { suburb: 'Richmond', region: 'VIC' },
    ])
  })

  it('reset clears the suburb pills', async () => {
    const { wrapper, SuburbPicker } = await mountPage()
    wrapper.findComponent(SuburbPicker).vm.$emit('update:modelValue', [{ suburb: 'Glenelg', region: 'SA' }])
    await flushPromises()

    await wrapper.find('.btn-reset').trigger('click')
    await flushPromises()

    expect(lastListingsQuery().suburbs).toEqual([])
    expect(wrapper.find('.btn-reset').exists()).toBe(false)
  })
})

describe('stock on listing cards', () => {
  it('shows units left, and keeps fully reserved listings visible but not requestable', async () => {
    const { wrapper } = await mountPage({ rows: [
      { ...listingRow(1, 'Bread', null), availableQuantity: 4 },
      { ...listingRow(2, 'Milk', null), availableQuantity: 0 },
    ] })
    const cards = wrapper.findAll('.lc-foot')
    expect(cards).toHaveLength(2)
    expect(wrapper.text()).toContain('4 units left')
    expect(cards[0].get('button').element.disabled).toBe(false)
    expect(wrapper.find('.lc-reserved').text()).toBe('Unavailable')
    expect(cards[1].get('button').text()).toBe('Unavailable')
    expect(cards[1].get('button').element.disabled).toBe(true)
  })
})
