import { describe, expect, it } from 'vitest'
import { toReportRow } from './donorReportAdapter'

const report = {
  reportId: 7,
  reason: 'Recipient no-show',
  details: 'Did not arrive',
  actualTransactionTime: '2026-10-12T17:45:00',
  status: 'PENDING',
  listingName: 'Bread rolls',
  reportedName: 'Test Recipient',
  timeslotStart: '2026-10-12T17:30:00',
  timeslotEnd: '2026-10-12T18:00:00',
  evidenceImageUrl: '/uploads/evidence.jpg',
}

describe('toReportRow', () => {
  it('maps a donor report to a report list row', () => {
    expect(toReportRow(report)).toEqual({
      id: 7,
      foodName: 'Bread rolls',
      recipient: 'Test Recipient',
      timeslot: '2026-10-12: 17:30 - 18:00',
      actualDate: '2026-10-12 17:45',
      reason: 'Recipient no-show: Did not arrive',
      photo: { name: 'Evidence', url: '/uploads/evidence.jpg' },
      status: 'Under Review',
      statusClass: 'status-pending',
    })
  })

  it('shows IN_REVIEW as under review and RESOLVED as resolved', () => {
    expect(toReportRow({ ...report, status: 'IN_REVIEW' }).status).toBe('Under Review')
    expect(toReportRow({ ...report, status: 'RESOLVED' })).toMatchObject({ status: 'Resolved', statusClass: 'status-resolved' })
  })

  it('handles reports without an actual time, details, photo or timeslot', () => {
    const row = toReportRow({ ...report, details: null, actualTransactionTime: null, evidenceImageUrl: null, timeslotStart: null, timeslotEnd: null })
    expect(row).toMatchObject({ actualDate: '', timeslot: '—', reason: 'Recipient no-show', photo: null })
  })
})
