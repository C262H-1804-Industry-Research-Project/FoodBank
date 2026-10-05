// Mapbox Search Box API: https://docs.mapbox.com/api/search/search-box/
// Use a public (pk.) token restricted to this site's URLs; it is visible in the browser.
const baseUrl = 'https://api.mapbox.com/search/searchbox/v1'
const accessToken = import.meta.env?.VITE_MAPBOX_TOKEN || ''

export const isAddressLookupEnabled = Boolean(accessToken)

async function mapboxRequest(path, params, signal) {
  const query = new URLSearchParams({ ...params, access_token: accessToken })
  const response = await fetch(`${baseUrl}${path}?${query}`, { signal })
  if (!response.ok) {
    throw new Error(response.status === 429
      ? 'Address lookup is busy. Keep typing or enter the address manually.'
      : 'Address lookup is unavailable. Enter the address manually.')
  }
  return response.json()
}

export async function suggestAddresses(query, sessionToken, signal) {
  if (!isAddressLookupEnabled) return []
  const result = await mapboxRequest('/suggest', {
    q: query.slice(0, 256),
    session_token: sessionToken,
    country: 'au',
    types: 'address',
    language: 'en',
    limit: '5',
    proximity: 'ip',
  }, signal)
  return Array.isArray(result?.suggestions) ? result.suggestions : []
}

// Turn Mapbox's address context into the listing's street / suburb / state fields
export function toPickupAddress(name, context = {}) {
  const regionCode = context.region?.region_code || ''
  return {
    addressLine: context.address?.name || name || '',
    suburb: context.locality?.name || context.place?.name || '',
    stateRegion: regionCode.replace(/^AU-/i, '').toUpperCase(),
    postcode: context.postcode?.name || '',
  }
}

export async function retrieveAddress(suggestion, sessionToken, signal) {
  const result = await mapboxRequest(`/retrieve/${encodeURIComponent(suggestion.mapbox_id)}`, {
    session_token: sessionToken,
  }, signal)
  const properties = result?.features?.[0]?.properties
  if (!properties) return toPickupAddress(suggestion.name, suggestion.context)
  return toPickupAddress(properties.name, properties.context)
}
