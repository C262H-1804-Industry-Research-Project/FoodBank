import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearMyLocation, getDonorReports, getListingsPage, getMyLocation, saveMyLocation, storeSession } from './client'

// A fetch stand-in that records each request and answers with `body` (or an empty body)
function mockFetch(body, status = 200) {
  // A 204 must have no body at all, so `undefined` sends none
  const fetchMock = vi.fn(async () => new Response(body === undefined ? null : JSON.stringify(body), { status }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const emptyPage = { content: [], page: { totalPages: 0, totalElements: 0 } }

function requestedUrl(fetchMock) {
  return new URL(fetchMock.mock.calls[0][0], 'http://localhost')
}

beforeEach(() => {
  localStorage.clear()
})

describe('getListingsPage', () => {
  it('sends the location so the API can sort by distance', async () => {
    const fetchMock = mockFetch(emptyPage)

    await getListingsPage({ latitude: -34.9285, longitude: 138.6007 })

    const params = requestedUrl(fetchMock).searchParams
    expect(params.get('latitude')).toBe('-34.9285')
    expect(params.get('longitude')).toBe('138.6007')
  })

  it('leaves the location out unless both coordinates are numbers', async () => {
    const fetchMock = mockFetch(emptyPage)

    await getListingsPage({ latitude: -34.9, longitude: undefined })
    await getListingsPage({ latitude: null, longitude: null })

    for (const [url] of fetchMock.mock.calls) {
      const params = new URL(url, 'http://localhost').searchParams
      expect(params.has('latitude')).toBe(false)
      expect(params.has('longitude')).toBe(false)
    }
  })

  it('sends each selected suburb as Suburb|STATE, or just the name without a state', async () => {
    const fetchMock = mockFetch(emptyPage)

    await getListingsPage({
      suburbs: [{ suburb: 'Glenelg', region: 'SA' }, { suburb: 'Richmond', region: '' }],
    })

    expect(requestedUrl(fetchMock).searchParams.getAll('suburbs')).toEqual(['Glenelg|SA', 'Richmond'])
  })

  it('returns the listings with their distances', async () => {
    mockFetch({
      content: [{ listingId: 1, foodName: 'Bread', distanceKm: 9.8 }],
      page: { totalPages: 1, totalElements: 1 },
    })

    const result = await getListingsPage()

    expect(result.items[0].distanceKm).toBe(9.8)
    expect(result.totalElements).toBe(1)
  })

  it('rejects a malformed response', async () => {
    mockFetch({ items: [] })

    await expect(getListingsPage()).rejects.toThrow('Invalid listings response')
  })
})

describe('recipient location', () => {
  beforeEach(() => {
    storeSession({ accessToken: 'abc', tokenType: 'Bearer', user: { role: 'recipient' } })
  })

  it('returns null when no location is saved', async () => {
    mockFetch(undefined, 204)

    await expect(getMyLocation()).resolves.toBeNull()
  })

  it('returns the saved location', async () => {
    const saved = { addressLine: '1 King William St', suburb: 'Adelaide', region: 'SA', latitude: -34.9, longitude: 138.6 }
    const fetchMock = mockFetch(saved)

    await expect(getMyLocation()).resolves.toEqual(saved)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/recipients/me/location')
  })

  it('saves with PUT, the JSON body and the login token', async () => {
    const location = { addressLine: '1 King William St', suburb: 'Adelaide', region: 'SA', latitude: -34.9, longitude: 138.6 }
    const fetchMock = mockFetch(location)

    await saveMyLocation(location)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/recipients/me/location')
    expect(options.method).toBe('PUT')
    expect(JSON.parse(options.body)).toEqual(location)
    expect(options.headers.Authorization).toBe('Bearer abc')
    expect(options.headers['Content-Type']).toBe('application/json')
  })

  it('clears with DELETE', async () => {
    const fetchMock = mockFetch(undefined, 204)

    await clearMyLocation()

    expect(fetchMock.mock.calls[0][1].method).toBe('DELETE')
  })

  it('surfaces the API error message', async () => {
    mockFetch({ error: 'addressLine or suburb is required' }, 400)

    await expect(saveMyLocation({})).rejects.toThrow('addressLine or suburb is required')
  })
})

describe('getDonorReports', () => {
  it('asks for the first 10 reports by default, with no filters', async () => {
    const fetchMock = mockFetch(emptyPage)

    await getDonorReports(3)

    const url = requestedUrl(fetchMock)
    expect(url.pathname).toMatch(/\/donors\/3\/reports$/)
    expect(Object.fromEntries(url.searchParams)).toEqual({ page: '0', size: '10' })
  })

  it('sends the page, status and trimmed search', async () => {
    const fetchMock = mockFetch(emptyPage)

    await getDonorReports(3, { page: 2, status: 'RESOLVED', search: '  bread ' })

    const params = requestedUrl(fetchMock).searchParams
    expect(params.get('page')).toBe('2')
    expect(params.get('status')).toBe('RESOLVED')
    expect(params.get('q')).toBe('bread')
  })

  it('returns the reports with the page totals', async () => {
    mockFetch({ content: [{ reportId: 1 }], page: { totalPages: 4, totalElements: 31 } })

    await expect(getDonorReports(3)).resolves.toEqual({ items: [{ reportId: 1 }], totalElements: 31, totalPages: 4 })
  })
})
