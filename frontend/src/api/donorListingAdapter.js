import { formatPickupSlot } from './listingAdapter'

// Whole days from a YYYY-MM-DD date to today, in local time (positive once the date has passed)
function daysSince(dateKey, now) {
  const [year, month, day] = dateKey.split('-').map(Number)
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((today - new Date(year, month - 1, day)) / 86400000)
}

// Display only: a listing stays LIVE on the server after its collect-by date
function expiryLabel(collectBy, now) {
  const days = collectBy ? daysSince(collectBy, now) : 0
  if (days <= 0) return { expired: false, expiredLabel: '' }
  return { expired: true, expiredLabel: days === 1 ? 'Expired yesterday' : `Expired ${days} days ago` }
}

// `now` is an option, not a second argument, so `listings.map(toListingRow)` stays safe
// (map passes the array index as the second argument)
export function toListingRow(listing, { now = new Date() } = {}) {
  const timeslots = [...(listing.timeslots || [])]
    .sort((first, second) => first.startTime.localeCompare(second.startTime))
  const firstSlot = timeslots[0]
  const status = listing.status || 'Draft'

  return {
    id: listing.listingId,
    name: listing.name,
    category: listing.category,
    quantity: listing.quantity,
    collectionRate: `${listing.completedRequests ?? 0}/${listing.approvedRequests ?? 0}`,
    available: listing.availableQuantity ?? null,
    deadline: listing.collectionDeadline || listing.expiryDate || '—',
    collectionDeadline: listing.collectionDeadline,
    expiryDate: listing.expiryDate,
    maxDailyCapacity: listing.maxDailyCapacity,
    categoryId: listing.categoryId,
    addressLine: listing.addressLine || '',
    notes: listing.description || '',
    photoUrl: listing.photoUrl || '',
    autoApproval: listing.isAutoApproved ?? true,
    requirePickupCode: listing.usePickupCode ?? false,
    hideContact: listing.hideContact ?? true,
    slotCapacity: listing.slotCapacity ?? firstSlot?.maxRecipients ?? 4,
    suburb: listing.suburb || '',
    stateRegion: listing.region || '',
    tags: listing.tags || [],
    timeslots: listing.timeslots || [],
    slots: firstSlot ? formatPickupSlot(firstSlot) : 'Not yet published',
    booked: firstSlot ? firstSlot.maxRecipients - firstSlot.remainingSlot : '—',
    status,
    statusClass: status.toLowerCase() === 'live' ? 'status-live' : 'status-draft',
    // Same date the "Collect by" column shows
    ...expiryLabel(listing.collectionDeadline || listing.expiryDate, now),
  }
}


// The donor's own address in the same shape as a listing row's address fields
export function toDonorAddress(settings) {
  if (!settings?.addressLine) return null
  return { addressLine: settings.addressLine, suburb: settings.suburb || '', stateRegion: settings.region || '' }
}

export function formatAddress(address) {
  return [address.addressLine, address.suburb, address.stateRegion].filter(Boolean).join(', ')
}

export function isSameAddress(first, second) {
  const normalise = value => (value || '').trim().toLowerCase()
  return ['addressLine', 'suburb', 'stateRegion'].every(key => normalise(first?.[key]) === normalise(second?.[key]))
}
