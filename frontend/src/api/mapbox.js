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

// `types` limits results, e.g. "address" or "address,locality,place,postcode"
export async function suggestAddresses(query, sessionToken, signal, types = 'address') {
  if (!isAddressLookupEnabled) return []
  const result = await mapboxRequest('/suggest', {
    q: query.slice(0, 256),
    session_token: sessionToken,
    country: 'au',
    types,
    language: 'en',
    limit: '5',
    proximity: 'ip',
  }, signal)
  return Array.isArray(result?.suggestions) ? result.suggestions : []
}

// Turn Mapbox's address context into the listing's street / suburb / state fields
// For a suburb or city result, the suburb is the feature itself rather than part of its context
const suburbFeatureTypes = ['locality', 'place', 'neighborhood']

export function toPickupAddress(name, context = {}, featureType = 'address') {
  const regionCode = context.region?.region_code || ''
  return {
    addressLine: context.address?.name || name || '',
    suburb: (suburbFeatureTypes.includes(featureType) ? name : '')
      || context.locality?.name || context.place?.name || '',
    stateRegion: regionCode.replace(/^AU-/i, '').toUpperCase(),
    postcode: context.postcode?.name || '',
  }
}

export async function retrieveAddress(suggestion, sessionToken, signal) {
  const result = await mapboxRequest(`/retrieve/${encodeURIComponent(suggestion.mapbox_id)}`, {
    session_token: sessionToken,
  }, signal)
  const feature = result?.features?.[0]
  const properties = feature?.properties
  if (!properties) {
    return {
      ...toPickupAddress(suggestion.name, suggestion.context, suggestion.feature_type),
      label: suggestion.full_address || suggestion.name || '',
      latitude: null,
      longitude: null,
    }
  }
  // GeoJSON coordinates are [longitude, latitude]
  const [longitude = null, latitude = null] = feature.geometry?.coordinates || []
  return {
    ...toPickupAddress(properties.name, properties.context, properties.feature_type),
    label: properties.full_address || properties.name || '',
    latitude: properties.coordinates?.latitude ?? latitude,
    longitude: properties.coordinates?.longitude ?? longitude,
  }
}
