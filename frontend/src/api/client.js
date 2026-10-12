const baseUrl = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')
const authStorageKey = 'second-table-auth'

export function getStoredSession() {
  try {
    const value = localStorage.getItem(authStorageKey)
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

export function storeSession(session) {
  localStorage.setItem(authStorageKey, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(authStorageKey)
}

export async function apiRequest(path, { method = 'GET', body, signal } = {}) {
  const timeout = AbortSignal.timeout(15000)
  const session = getStoredSession()
  let response
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(session?.accessToken
          ? { Authorization: `${session.tokenType || 'Bearer'} ${session.accessToken}` }
          : {}),
        ...(body === undefined || body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(body === undefined ? {} : { body: body instanceof FormData ? body : JSON.stringify(body) }),
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error(timeout.aborted ? 'The API request timed out.' : 'Cannot connect to the donation API.')
  }
  const text = await response.text()
  let data
  try { data = text ? JSON.parse(text) : null } catch {
    throw new Error(`Unexpected API response (HTTP ${response.status}).`)
  }
  if (!response.ok) throw new Error(data?.error || data?.message || `API request failed (HTTP ${response.status}).`)
  return data
}

export async function uploadPhoto(file, signal) {
  const body = new FormData()
  body.append('file', file)
  const result = await apiRequest('/uploads/photo', { method: 'POST', body, signal })
  const photoUrl = typeof result === 'string'
    ? result
    : result?.photoUrl || result?.url || result?.imageUrl || result?.location
  if (typeof photoUrl !== 'string' || !photoUrl.trim()) {
    throw new Error('The photo upload response did not include a photo URL.')
  }
  return photoUrl
}

export function login(credentials) {
  return apiRequest('/auth/login', { method: 'POST', body: credentials })
}

export function registerDonor(donor) {
  return apiRequest('/auth/register-donors', { method: 'POST', body: donor })
}

export function registerRecipient(recipient) {
  return apiRequest('/auth/register-recipients', { method: 'POST', body: recipient })
}

export function logout() {
  return apiRequest('/auth/logout', { method: 'POST' })
}

// The signed-in recipient's saved location, used to show distance to listings (null if none saved)
export function getMyLocation(signal) {
  return apiRequest('/recipients/me/location', { signal })
}

export function saveMyLocation(location) {
  return apiRequest('/recipients/me/location', { method: 'PUT', body: location })
}

export function clearMyLocation() {
  return apiRequest('/recipients/me/location', { method: 'DELETE' })
}

// Fetch one page of live listings. `page` is zero-based, matching Spring's PageRequest.
export async function getListingsPage({
  page = 0, size = 6, foodName, categoryId, tagIds = [], suburbs = [], latitude, longitude,
} = {}, signal) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (foodName?.trim()) params.set('foodName', foodName.trim())
  if (categoryId != null && categoryId !== '') params.set('categoryId', String(categoryId))
  tagIds.forEach(tagId => params.append('tagIds', String(tagId)))
  // Each selected suburb is sent as "Suburb|REGION"; listings in any of them match
  suburbs.forEach(({ suburb, region }) => params.append('suburbs', region ? `${suburb}|${region}` : suburb))
  // With a location, the backend sorts nearest-first and returns each listing's distanceKm
  if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
    params.set('latitude', String(latitude))
    params.set('longitude', String(longitude))
  }

  const result = await apiRequest(`/listings?${params}`, { signal })

  if (!Array.isArray(result?.content) || !Number.isInteger(result?.page?.totalPages)
      || !Number.isInteger(result?.page?.totalElements)) throw new Error('Invalid listings response from the API.')
  return { items: result.content, totalElements: result.page.totalElements, totalPages: result.page.totalPages }
}

export async function getDonorSettings(donorId, signal) {
  const settings = await apiRequest(`/donors/${donorId}/settings`, { signal })
  if (settings?.donorId == null) throw new Error('Invalid donor settings response from the API.')
  return settings
}

export async function getDonorListings(donorId, signal) {
  if (!Number.isInteger(donorId) || donorId <= 0) {
    throw new Error('The signed-in donor is missing an ID.')
  }

  const listings = await apiRequest(`/donors/${donorId}/listings`, { signal })
  if (!Array.isArray(listings)) throw new Error('Invalid donor listings response from the API.')
  return listings
}

export async function getListingCatalogue(signal) {
  const [categories, tags] = await Promise.all([
    apiRequest('/listings/categories', { signal }),
    apiRequest('/listings/tags', { signal }),
  ])
  if (!Array.isArray(categories) || !Array.isArray(tags)) {
    throw new Error('Invalid listing catalogue response from the API.')
  }
  return { categories, tags }
}

export function createDonorListing(donorId, listing, signal) {
  return apiRequest(`/donors/${donorId}/listings`, {
    method: 'POST',
    body: listing,
    signal,
  })
}

export function updateDonorListing(donorId, listingId, listing, signal) {
  return apiRequest(`/donors/${donorId}/listings/${listingId}`, {
    method: 'PUT', body: listing, signal,
  })
}

export function deleteDonorListing(donorId, listingId, signal) {
  return apiRequest(`/donors/${donorId}/listings/${listingId}`, {
    method: 'DELETE', signal,
  })
}

export async function getReportableRequests(donorId, signal) {
  if (!Number.isInteger(donorId) || donorId <= 0) {
    throw new Error('The signed-in donor is missing an ID.')
  }

  const requests = await apiRequest(`/donors/${donorId}/requests/reportable`, { signal })
  if (!Array.isArray(requests)) throw new Error('Invalid reportable requests response from the API.')
  return requests
}

export function submitReport(report, signal) {
  return apiRequest('/reports', { method: 'POST', body: report, signal })
}

// status is a backend status code such as 'PENDING'; leave it empty for every report
export async function getDonorReports(donorId, { status = '' } = {}, signal) {
  if (!Number.isInteger(donorId) || donorId <= 0) {
    throw new Error('The signed-in donor is missing an ID.')
  }

  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  const reports = await apiRequest(`/donors/${donorId}/reports${query}`, { signal })
  if (!Array.isArray(reports)) throw new Error('Invalid reports response from the API.')
  return reports
}

export async function getReportStatuses(signal) {
  const statuses = await apiRequest('/reports/statuses', { signal })
  if (!Array.isArray(statuses)) throw new Error('Invalid report statuses response from the API.')
  return statuses
}
