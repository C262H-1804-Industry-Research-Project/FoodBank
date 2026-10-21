import { formatPickupSlot } from './listingAdapter'

export function toListingRow(listing) {
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
