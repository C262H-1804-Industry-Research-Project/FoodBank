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
    deadline: listing.collectionDeadline || listing.expiryDate || '—',
    collectionDeadline: listing.collectionDeadline,
    expiryDate: listing.expiryDate,
    categoryId: listing.categoryId,
    addressLine: listing.addressLine || '',
    notes: listing.description || '',
    autoApproval: listing.isAutoApproved ?? true,
    requirePickupCode: listing.usePickupCode ?? false,
    hideContact: listing.hideContact ?? true,
    slotCapacity: listing.slotCapacity ?? firstSlot?.maxRecipients ?? 4,
    suburb: listing.suburb || '',
    stateRegion: listing.region || '',
    slots: firstSlot ? formatPickupSlot(firstSlot) : 'Not yet published',
    booked: firstSlot ? firstSlot.maxRecipients - firstSlot.remainingSlot : '—',
    status,
    statusClass: status.toLowerCase() === 'live' ? 'status-live' : 'status-draft',
  }
}

