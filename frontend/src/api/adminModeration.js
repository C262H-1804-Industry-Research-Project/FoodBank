import { apiRequest } from './client'

export async function fetchAdminListings() {
  const rows = []
  let page = 0
  while (true) {
    const result = await apiRequest(`/admin/listings?page=${page}&size=100`)
    if (!Array.isArray(result?.content) || !Number.isInteger(result?.page?.totalPages)) throw new Error('Invalid listing response')
    rows.push(...result.content.map(({ listing: item, status, quantity, completedRequests, approvedRequests }) => ({
      id: item.listingId, name: item.foodName, donor: item.donorName || 'Unknown donor', category: item.category,
      quantity: `${quantity ?? 0} units`, suburb: item.suburb, stateRegion: item.region, tags: item.tags || [],
      status, photoUrl: item.photoUrl, emoji: '🍽️', timeslots: item.timeslots || [], description: item.description,
      timeType: pickupDay(item.timeslots || []),
      collectionRate: `${completedRequests ?? 0}/${approvedRequests ?? 0}`,
    })))
    page++
    if (page >= result.page.totalPages) return rows
  }
}
function pickupDay(slots) {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const tomorrow = new Date(today); tomorrow.setDate(today.getDate() + 1)
  if (slots.some(slot => new Date(slot.startTime).toDateString() === today.toDateString())) return 'today'
  if (slots.some(slot => new Date(slot.startTime).toDateString() === tomorrow.toDateString())) return 'tomorrow'
  if (slots.some(slot => [0, 6].includes(new Date(slot.startTime).getDay()) && new Date(slot.startTime) >= today)) return 'weekend'
  return ''
}
// page is 0-based. role is DONOR or RECIPIENT, status is ACTIVE, UNDER_REVIEW or BANNED, and
// search matches the user ID, name, email or suburb; leave any of them empty to not filter by it.
export async function fetchAdminAccounts({ page = 0, size = 10, role, status, search } = {}, signal) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (role) params.set('role', role)
  if (status) params.set('status', status)
  if (search?.trim()) params.set('q', search.trim())
  const result = await apiRequest(`/admin/accounts?${params}`, { signal })
  if (!Array.isArray(result?.content) || !Number.isInteger(result?.page?.totalPages)
      || !Number.isInteger(result?.page?.totalElements)) throw new Error('Invalid account response')
  return { items: result.content.map(adminAccountRow), totalElements: result.page.totalElements, totalPages: result.page.totalPages }
}
function adminAccountRow(item) {
  const status = { ACTIVE: 'Active', BANNED: 'Deactivated', UNDER_REVIEW: 'Under review' }[item.status] || item.status
  const role = { DONOR: 'Donor', RECIPIENT: 'Recipient' }[item.role] || item.role
  return { userId: item.userId, name: item.name || `User #${item.userId}`, role, email: item.email || '',
    phone: item.phone || '', location: [item.suburb, item.region].filter(Boolean).join(', '), status,
    reportsAgainst: item.reportsAgainst, banTimes: item.banTimes, banDaysRemaining: item.banDaysRemaining,
    collectionRate: `${item.completedCollections ?? 0}/${item.totalRequests ?? 0}` }
}
export function deactivateUser(userId, reason) {
  return apiRequest(`/admin/users/${userId}/ban`, { method: 'POST', body: { reason, banType: 'PERMANENT' } })
}
export function reactivateUser(userId) { return apiRequest(`/admin/users/${userId}/unban`, { method: 'POST' }) }
export function banFoodListing(id) { return apiRequest(`/admin/listings/${id}/ban`, { method: 'POST' }) }
export function fetchAdminReports() { return apiRequest('/admin/reports') }
export function processAdminReport(id, body) { return apiRequest(`/admin/reports/${id}`, { method: 'PATCH', body }) }
export function fetchBanPolicy() { return apiRequest('/settings/ban-days') }
export function saveBanPolicy(body) { return apiRequest('/settings/ban-days', { method: 'PUT', body }) }
export function adminReportRow(item) {
  const status = { PENDING: 'Pending', IN_REVIEW: 'In review', RESOLVED: 'Resolved', DISMISSED: 'Dismissed' }[item.status] || item.status
  return { ...item, id: item.reportId, reportedUser: item.reportedName || `User #${item.reportedId}`,
    reportedRole: item.reportedRole || 'User', reporter: item.reporterName || `User #${item.reporterId}`,
    foodName: item.foodName || (item.listingId ? `Listing #${item.listingId}` : 'No related listing'),
    reason: item.reason || '', timeslot: item.actualTransactionTime || '', offenceCount: item.banCount || 0, status }
}
