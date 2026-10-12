import { apiRequest } from './client'

export async function fetchAdminListings() {
  const rows = []
  let page = 0
  while (true) {
    const result = await apiRequest(`/admin/listings?page=${page}&size=100`)
    if (!Array.isArray(result?.content) || !Number.isInteger(result?.page?.totalPages)) throw new Error('Invalid listing response')
    rows.push(...result.content.map(({ listing: item, status, quantity }) => ({
      id: item.listingId, name: item.foodName, donor: item.donorName || 'Unknown donor', category: item.category,
      quantity: `${quantity ?? 0} units`, suburb: item.suburb, stateRegion: item.region, tags: item.tags || [],
      status, photoUrl: item.photoUrl, emoji: '🍽️', timeslots: item.timeslots || [], description: item.description,
      timeType: pickupDay(item.timeslots || []),
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
export async function fetchAdminAccounts() {
  const [donors, recipients] = await Promise.all([apiRequest('/donors'), apiRequest('/recipients')])
  if (!Array.isArray(donors) || !Array.isArray(recipients)) throw new Error('Invalid account response')
  return [
    ...donors.map(item => adminAccountRow(item, 'Donor', item.organisation)),
    ...recipients.map(item => adminAccountRow(item, 'Recipient', item.recipient)),
  ]
}
function adminAccountRow(item, role, name) {
  const status = { ACTIVE: 'Active', BANNED: 'Deactivated', UNDER_REVIEW: 'Under review' }[item.status] || item.status
  return { userId: item.userId, name: name || `User #${item.userId}`, role, email: item.email || '',
    phone: item.phone || '', location: [item.suburb, item.region].filter(Boolean).join(', '), status, reportsAgainst: item.reportsAgainst, banTimes: item.banTimes,
    banDaysRemaining: item.banDaysRemaining }
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
