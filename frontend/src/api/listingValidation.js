export const MAX_LISTING_DESCRIPTION_LENGTH = 2000

export function validateListingDescription(description) {
  if ((description || '').length > MAX_LISTING_DESCRIPTION_LENGTH) {
    throw new Error(`Storage instructions / description must be ${MAX_LISTING_DESCRIPTION_LENGTH} characters or fewer.`)
  }
}
