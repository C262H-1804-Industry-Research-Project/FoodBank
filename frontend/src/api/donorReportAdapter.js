export const REPORT_REASONS = [
  'Recipient no-show',
  'Late collection',
  'Collected incorrect quantity',
  'Disrespectful behaviour',
  'Other',
]

// Matches the backend: a report can be edited or cancelled until an admin picks it up
const CHANGEABLE_STATUSES = ['PENDING']

const STATUS_DISPLAY = {
  PENDING: { status: 'Under Review', statusClass: 'status-pending' },
  IN_REVIEW: { status: 'Being Reviewed', statusClass: 'status-reviewing' },
  RESOLVED: { status: 'Resolved', statusClass: 'status-resolved' },
  DISMISSED: { status: 'Dismissed', statusClass: 'status-dismissed' },
  CANCELLED: { status: 'Cancelled', statusClass: 'status-dismissed' },
}

export function toReportRow(report) {
  const details = (report.details || '').trim()
  const [slotDate = '', slotStart = ''] = (report.timeslotStart || '').split('T')
  const [, slotEnd = ''] = (report.timeslotEnd || '').split('T')
  const [actualDate = '', actualTime = ''] = (report.actualTransactionTime || '').split('T')
  // e.g. "2026-10-12 17:30-18:00: 17:45"; the actual time is left off when not recorded
  const slot = slotDate ? `${slotDate} ${slotStart.slice(0, 5)}-${slotEnd.slice(0, 5)}` : ''
  const actual = actualTime.slice(0, 5)
  const timeslot = slot && actual ? `${slot} (${actual})`
    : slot || (actualDate ? `${actualDate} ${actual}` : '—')

  return {
    id: report.reportId,
    foodName: report.listingName || 'Deleted listing',
    recipient: report.reportedName || 'Unknown recipient',
    timeslot,
    reason: details ? `${report.reason}: ${details}` : report.reason || '',
    photo: report.evidenceImageUrl ? { name: 'Evidence', url: report.evidenceImageUrl } : null,
    canChange: CHANGEABLE_STATUSES.includes(report.status),
    // Raw values for the edit dialog; the date is kept so only the time needs editing
    edit: {
      reason: report.reason || '',
      details,
      actualDate: actualDate || (report.timeslotStart || '').split('T')[0],
      actualTime: actualTime.slice(0, 5),
      evidenceImageUrl: report.evidenceImageUrl || '',
    },
    ...(STATUS_DISPLAY[report.status] || { status: report.status || 'Unknown', statusClass: 'status-pending' }),
  }
}
