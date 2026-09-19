const baseUrl = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')

export async function apiRequest(path, { signal } = {}) {
  const timeout = AbortSignal.timeout(15000)
  let response
  try {
    response = await fetch(`${baseUrl}${path}`, {
      headers: { Accept: 'application/json' },
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
