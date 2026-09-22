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
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
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

// Fetch all pages so the existing client-side filters and six-item pagination
// continue to operate on the full collection, including donor-name searches.
export async function getAllListings(signal) {
  const items = []
  let page = 0
  let totalPages
  do {
    const result = await apiRequest(`/listings?page=${page}&size=10`, { signal })
    if (!Array.isArray(result?.content) || !Number.isInteger(result?.page?.totalPages)
        || result.page.totalPages < 0) throw new Error('Invalid listings response from the API.')
    items.push(...result.content)
    totalPages = result.page.totalPages
    page++
  } while (page < totalPages)
  return items
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
