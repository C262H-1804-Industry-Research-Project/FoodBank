import { apiRequest } from './client'
import { formatPickupSlot } from './listingAdapter'

async function requestList(path, signal) {
  const result = await apiRequest(path, { signal })
  if (!Array.isArray(result)) throw new Error('The pickup requests API returned an invalid list.')
  return result
}

export const fetchRecipientRequests = signal => requestList('/requests', signal)
export function fetchDonorRequests(donorId, signal) {
  if (!Number.isInteger(donorId) || donorId < 1) throw new Error('Please sign in with a donor account.')
  return requestList(`/donors/${donorId}/requests`, signal)
}
export const createPickupRequest = body => apiRequest('/requests', { method: 'POST', body })
export const cancelPickupRequest = id => apiRequest(`/requests/${id}`, { method: 'DELETE' })
export function changePickupRequest(donorId, id, action) {
  if (!['approve', 'reject', 'complete'].includes(action)) throw new Error('Invalid pickup request action.')
  return apiRequest(`/donors/${donorId}/requests/${id}/${action}`, { method: 'PATCH' })
}

export function isPickupOpen(slot, now = Date.now()) {
  return Number.isInteger(slot?.timeslotId) && slot.timeslotId > 0
    && slot.remainingSlot > 0 && new Date(slot.startTime).getTime() > now
}

export function canCancelPickup(row, now = Date.now()) {
  return ['PENDING', 'APPROVED'].includes(row.statusCode)
    && new Date(row.pickupStart).getTime() > now
}

// Why a request's Cancel button is disabled, or null when it can be cancelled
export function cancelBlockedReason(row, now = Date.now()) {
  if (canCancelPickup(row, now)) return null
  if (row.statusCode === 'CANCELLED') return 'Already cancelled'
  if (row.statusCode === 'REJECTED') return row.status === 'Not reviewed in time' ? 'Not reviewed before pickup' : 'Request was declined'
  if (row.statusCode === 'COMPLETED') return 'Already collected'
  if (row.statusCode === 'EXPIRED') return 'Pickup window has ended'
  if (['PENDING', 'APPROVED'].includes(row.statusCode)) return 'Pickup time has passed'
  return 'This request can no longer be cancelled'
}

const autoRejected = r => r.status === 'REJECTED' && r.rejectionReason === 'NOT_REVIEWED_IN_TIME'

function checked(request) {
  if (!Number.isInteger(request?.requestId) || typeof request.status !== 'string') {
    throw new Error('The pickup requests API returned an invalid request.')
  }
  return request
}

export function recipientRequestRow(response) {
  const r = checked(response)
  const approved = r.status === 'APPROVED'
  return {
    id: r.requestId,
    foodItem: `${r.listingName || '—'} (${r.quantityRequested} units)`,
    donor: r.donorName || '—',
    pickupSlot: formatPickupSlot({ startTime: r.timeslotStart, endTime: r.timeslotEnd }),
    pickupStart: r.timeslotStart,
    statusCode: r.status,
    status: autoRejected(r) ? 'Not reviewed in time'
      : { PENDING: 'Requested', APPROVED: 'Reserved', REJECTED: 'Declined', CANCELLED: 'Cancelled', COMPLETED: 'Collected', EXPIRED: 'Expired' }[r.status] || r.status,
    statusClass: approved || r.status === 'COMPLETED' ? 'status-reserved' : r.status === 'PENDING' ? 'status-requested' : 'status-expired',
    address: approved ? r.pickupAddress : null,
    code: approved ? r.pickupCode : null,
  }
}

export function donorRequestRow(response) {
  const r = checked(response)
  return {
    id: r.requestId,
    foodName: r.listingName || '—',
    totalStock: r.listingQuantity == null ? '—'
      : r.availableQuantity == null ? `${r.listingQuantity} units`
      : `${r.availableQuantity} of ${r.listingQuantity} units`,
    recipientOrg: r.recipientOrg || r.recipientName || '—',
    contactName: r.recipientName || '—',
    phone: r.recipientPhone || '—',
    quantity: `${r.quantityRequested} units`,
    slot: formatPickupSlot({ startTime: r.timeslotStart, endTime: r.timeslotEnd }),
    pickupCode: r.pickupCode || null,
    handoverBy: r.handoverDeadline ? r.handoverDeadline.split('T')[1].slice(0, 5) : null,
    notes: r.note || '',
    status: autoRejected(r) ? 'Auto-rejected'
      : { PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Declined', CANCELLED: 'Cancelled', COMPLETED: 'Completed', EXPIRED: 'Expired' }[r.status] || r.status,
    statusClass: { PENDING: 'status-pending', APPROVED: 'status-approved', COMPLETED: 'status-completed', EXPIRED: 'status-expired' }[r.status] || 'status-declined',
  }
}
