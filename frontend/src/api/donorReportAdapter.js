import { formatPickupSlot } from './listingAdapter'

const STATUS_DISPLAY = {
  PENDING: { status: 'Under Review', statusClass: 'status-pending' },
  IN_REVIEW: { status: 'Under Review', statusClass: 'status-pending' },
  RESOLVED: { status: 'Resolved', statusClass: 'status-resolved' },
  DISMISSED: { status: 'Dismissed', statusClass: 'status-dismissed' },
}

export function toReportRow(report) {
  const details = (report.details || '').trim()
  const timeslot = report.timeslotStart
    ? formatPickupSlot({ startTime: report.timeslotStart, endTime: report.timeslotEnd })
    : ''
  const [actualDate = '', actualTime = ''] = (report.actualTransactionTime || '').split('T')

  return {
    id: report.reportId,
    foodName: report.listingName || 'Deleted listing',
    recipient: report.reportedName || 'Unknown recipient',
    timeslot: timeslot || '—',
    actualDate: actualDate ? `${actualDate} ${actualTime.slice(0, 5)}` : '',
    reason: details ? `${report.reason}: ${details}` : report.reason || '',
    photo: report.evidenceImageUrl ? { name: 'Evidence', url: report.evidenceImageUrl } : null,
    ...(STATUS_DISPLAY[report.status] || { status: report.status || 'Unknown', statusClass: 'status-pending' }),
  }
}
