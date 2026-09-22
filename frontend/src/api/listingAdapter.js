const labels = {
  'no-pork': 'No pork', 'nut-free': 'Nut-free', vegetarian: 'Vegetarian', halal: 'Halal',
  'contains-gluten': 'Contains gluten', 'contains-nuts': 'Contains nuts', 'contains-pork': 'Contains pork',
}
const dayKey = date => `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`

export function formatPickupSlot(slot) {
  const [date = '', startTime = ''] = (slot.startTime || '').split('T')
  const [, endTime = ''] = (slot.endTime || '').split('T')
  return `${date}: ${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}`
}

export function toListingCard(item, now = new Date()) {
  if (item.listingId == null) throw new Error('The listings API is missing listingId. Update the matching backend DTO.')
  const slots = [...(item.timeslots || [])].sort((a, b) => a.startTime.localeCompare(b.startTime))
  const first = slots[0] ? new Date(slots[0].startTime) : null
  const tomorrow = new Date(now)
  tomorrow.setDate(now.getDate() + 1)
  const saturday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  saturday.setDate(saturday.getDate() + (now.getDay() === 0 ? -1 : 6 - now.getDay()))
  const monday = new Date(saturday)
  monday.setDate(monday.getDate() + 2)
  let timeType = ''
  if (first && !Number.isNaN(first.getTime())) {
    if (dayKey(first) === dayKey(now)) timeType = 'today'
    else if (dayKey(first) === dayKey(tomorrow)) timeType = 'tomorrow'
    else if (first >= saturday && first < monday) timeType = 'weekend'
  }

  return {
    id: item.listingId,
    name: item.foodName,
    category: item.category,
    notes: item.description || '',
    donor: item.donorName || '',
    suburb: item.suburb || '',
    stateRegion: item.region || '',
    capacity: item.maxDailyCapacity == null ? 'unlimited' : item.maxDailyCapacity,
    tags: (item.tags || []).map(tag => labels[tag.trim().toLowerCase().replace(/\s+/g, '-')] || tag),
    emoji: { Bakery: '🥖', Produce: '🥗', Pantry: '🥫', Dairy: '🥛' }[item.category] || '🍲',
    timeLeft: item.latestCollectionDate ? `Collect by ${item.latestCollectionDate}` : 'Available',
    pickupWindow: slots[0] ? formatPickupSlot(slots[0]) : '',
    timeslots: item.timeslots || [],
    timeType,
  }
}
