import { describe, expect, it, vi } from 'vitest'
import { toPickupAddress } from './mapbox'

// mapbox.js reads the token when it loads, so load a fresh copy after setting it
async function loadMapboxWithToken(token) {
  vi.stubEnv('VITE_MAPBOX_TOKEN', token)
  vi.resetModules()
  return import('./mapbox')
}

describe('toPickupAddress', () => {
  it('splits a street address into street, suburb and state', () => {
    const address = toPickupAddress('16 Moseley Street', {
      address: { name: '16 Moseley Street' },
      locality: { name: 'Glenelg' },
      region: { region_code: 'AU-SA' },
      postcode: { name: '5045' },
    })

    expect(address).toEqual({ addressLine: '16 Moseley Street', suburb: 'Glenelg', stateRegion: 'SA', postcode: '5045' })
  })

  it('uses the feature itself as the suburb for a suburb result', () => {
    const address = toPickupAddress('Richmond', { region: { region_code: 'VIC' } }, 'locality')

    expect(address.suburb).toBe('Richmond')
    expect(address.stateRegion).toBe('VIC')
  })

  it('falls back to the place name when there is no locality', () => {
    expect(toPickupAddress('1 Main Rd', { place: { name: 'Victor Harbor' } }).suburb).toBe('Victor Harbor')
  })
})

describe('Mapbox lookups', () => {
  it('are disabled without a token and never call Mapbox', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const mapbox = await loadMapboxWithToken('')

    expect(mapbox.isAddressLookupEnabled).toBe(false)
    await expect(mapbox.suggestAddresses('glenelg', 'session')).resolves.toEqual([])
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('ask for Australian results of the requested types', async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({ suggestions: [{ name: 'Glenelg' }] })))
    vi.stubGlobal('fetch', fetchMock)
    const mapbox = await loadMapboxWithToken('pk.test')

    const results = await mapbox.suggestAddresses('glenelg', 'session-1', undefined, 'locality,place')

    expect(results).toEqual([{ name: 'Glenelg' }])
    const url = new URL(fetchMock.mock.calls[0][0])
    expect(url.pathname).toBe('/search/searchbox/v1/suggest')
    expect(url.searchParams.get('country')).toBe('au')
    expect(url.searchParams.get('types')).toBe('locality,place')
    expect(url.searchParams.get('session_token')).toBe('session-1')
    expect(url.searchParams.get('access_token')).toBe('pk.test')
  })

  it('read coordinates from a retrieved address', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({
      features: [{
        geometry: { coordinates: [138.5136, -34.9806] },
        properties: {
          name: '16 Moseley Street',
          full_address: '16 Moseley Street, Glenelg SA 5045',
          feature_type: 'address',
          context: { locality: { name: 'Glenelg' }, region: { region_code: 'SA' } },
        },
      }],
    }))))
    const mapbox = await loadMapboxWithToken('pk.test')

    const address = await mapbox.retrieveAddress({ mapbox_id: 'abc' }, 'session-1')

    expect(address).toMatchObject({
      addressLine: '16 Moseley Street',
      suburb: 'Glenelg',
      stateRegion: 'SA',
      label: '16 Moseley Street, Glenelg SA 5045',
      latitude: -34.9806,
      longitude: 138.5136,
    })
  })

  it('report a friendly message when Mapbox is rate limiting', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('{}', { status: 429 })))
    const mapbox = await loadMapboxWithToken('pk.test')

    await expect(mapbox.suggestAddresses('glenelg', 'session')).rejects.toThrow('busy')
  })
})
