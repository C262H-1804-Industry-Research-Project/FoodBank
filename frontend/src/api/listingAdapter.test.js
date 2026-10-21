import { describe, expect, it } from 'vitest'
import { formatDistance, toListingCard } from './listingAdapter'

describe('formatDistance', () => {
  it('returns nothing when there is no distance', () => {
    expect(formatDistance(null)).toBe('')
    expect(formatDistance(undefined)).toBe('')
    expect(formatDistance(Number.NaN)).toBe('')
  })

  it('shows short distances in metres, rounded to 50 m', () => {
    expect(formatDistance(0.43)).toBe('450 m away')
    expect(formatDistance(0.9)).toBe('900 m away')
  })

  it('never shows less than 50 m', () => {
    expect(formatDistance(0)).toBe('50 m away')
    expect(formatDistance(0.01)).toBe('50 m away')
  })

  it('shows one decimal place under 10 km', () => {
    expect(formatDistance(0.99)).toBe('1.0 km away')
    expect(formatDistance(9.84)).toBe('9.8 km away')
  })

  it('shows whole kilometres from 10 km', () => {
    expect(formatDistance(9.96)).toBe('10 km away')
    expect(formatDistance(69.4)).toBe('69 km away')
  })
})

describe('toListingCard distance', () => {
  const row = { listingId: 1, foodName: 'Bread', suburb: 'Glenelg', region: 'SA', timeslots: [] }

  it('keeps the distance from the API', () => {
    expect(toListingCard({ ...row, distanceKm: 9.8 }).distanceKm).toBe(9.8)
  })

  it('uses null when the API has no distance', () => {
    expect(toListingCard({ ...row, distanceKm: null }).distanceKm).toBeNull()
    expect(toListingCard(row).distanceKm).toBeNull()
    expect(toListingCard({ ...row, distanceKm: '9.8' }).distanceKm).toBeNull()
  })

  it('maps suburb and state for the card', () => {
    const card = toListingCard(row)
    expect(card.suburb).toBe('Glenelg')
    expect(card.stateRegion).toBe('SA')
  })
})

describe('toListingCard stock', () => {
  const row = availableQuantity => ({ listingId: 1, foodName: 'Bread', timeslots: [], availableQuantity })

  it('shows what is left and marks a listing with nothing left as fully reserved', () => {
    expect(toListingCard(row(8))).toMatchObject({ available: 8, fullyReserved: false })
    expect(toListingCard(row(0))).toMatchObject({ available: 0, fullyReserved: true })
  })

  it('treats a missing amount as unknown rather than fully reserved', () => {
    expect(toListingCard(row(undefined))).toMatchObject({ available: null, fullyReserved: false })
  })
})
