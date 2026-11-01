export const MAX_LISTING_DESCRIPTION_LENGTH = 2000

export function validateListingDescription(description) {
  if ((description || '').length > MAX_LISTING_DESCRIPTION_LENGTH) {
    throw new Error(`Storage instructions / description must be ${MAX_LISTING_DESCRIPTION_LENGTH} characters or fewer.`)
  }
}

// Recipients can only request a listing through a pickup timeslot, so publishing needs one.
// Returns the message to show, or '' when the listing can be published.
export function publishTimeslotError(timeslots, slotDraft = {}) {
  if (timeslots?.length) return ''
  if (slotDraft.date || slotDraft.startTime || slotDraft.endTime) {
    return "You entered a pickup timeslot but didn't add it. Click 'Add timeslot' before publishing."
  }
  return "Add at least one pickup timeslot before publishing. Recipients can't request this listing without one."
}
