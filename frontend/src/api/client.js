const baseUrl = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')

export async function apiRequest(path, { method = 'GET', body, signal } = {}) {
  const timeout = AbortSignal.timeout(15000)
  let response
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
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
