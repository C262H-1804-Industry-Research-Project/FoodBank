import { describe, expect, it } from 'vitest'
import { toListingRow } from './donorListingAdapter'

describe('donor collection rate', () => {
  it('displays completed applications divided by approved applications', () => {
    expect(toListingRow({ completedRequests: 1, approvedRequests: 3 }).collectionRate).toBe('1/3')
    expect(toListingRow({ completedRequests: 0, approvedRequests: 0 }).collectionRate).toBe('0/0')
  })
  it('retains collection deadlines for the edit form', () => {
    const row = toListingRow({ collectionDeadline: '2026-10-18', completedRequests: 1, approvedRequests: 3 })
    expect(row.collectionDeadline).toBe('2026-10-18')
    expect(row.deadline).toBe('2026-10-18')
  })
})
