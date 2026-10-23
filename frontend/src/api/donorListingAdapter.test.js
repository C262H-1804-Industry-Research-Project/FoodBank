import { describe, expect, it } from 'vitest'
import { toListingRow } from './donorListingAdapter'

describe('toListingRow expiry highlight', () => {
  const now = new Date(2026, 9, 21, 9, 0)
  const row = dates => toListingRow({ listingId: 1, name: 'Bread', status: 'LIVE', timeslots: [], ...dates }, { now })

  it('marks a listing expired once its collect-by date has passed', () => {
    expect(row({ collectionDeadline: '2026-10-20' })).toMatchObject({ expired: true, expiredLabel: 'Expired yesterday' })
    expect(row({ collectionDeadline: '2026-10-19' }).expiredLabel).toBe('Expired 2 days ago')
    expect(row({ collectionDeadline: '2026-10-21' })).toMatchObject({ expired: false, expiredLabel: '' })
  })

  it('falls back to the expiry date when there is no collection deadline', () => {
    expect(row({ expiryDate: '2026-10-19' }).expired).toBe(true)
    expect(row({ expiryDate: '2026-10-25' }).expired).toBe(false)
  })

  it('prefers the collection deadline and never marks undated listings', () => {
    expect(row({ collectionDeadline: '2026-10-22', expiryDate: '2026-10-19' }).expired).toBe(false)
    expect(row({}).expired).toBe(false)
  })

  it('counts whole days across a daylight-saving change', () => {
    // Adelaide clocks went forward on 2026-10-04
    expect(row({ collectionDeadline: '2026-10-01' }).expiredLabel).toBe('Expired 20 days ago')
  })

  it('works as a map callback, where the second argument is the array index', () => {
    const rows = [{ listingId: 1, name: 'Old', status: 'LIVE', collectionDeadline: '2000-01-01' },
      { listingId: 2, name: 'New', status: 'LIVE', collectionDeadline: '2999-01-01' }].map(toListingRow)
    expect(rows.map(r => r.expired)).toEqual([true, false])
  })
})
