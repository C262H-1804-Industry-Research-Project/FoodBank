import { reportStatusDisplay } from './donorReportAdapter'

// A row of the recipient's report list, from GET /api/recipients/me/reports
export function toRecipientReportRow(report) {
  const details = (report.details || '').trim()
  return {
    id: report.reportId,
    collection: `${report.listingName || 'Deleted listing'} — ${report.donorName || 'Unknown donor'}`,
    reason: report.reason || '',
    details: details || '—',
    // e.g. "2026-10-15 12:30"
    submittedAt: report.createdAt ? report.createdAt.slice(0, 16).replace('T', ' ') : '—',
    photo: report.evidenceImageUrl ? { name: 'Evidence', url: report.evidenceImageUrl } : null,
    ...reportStatusDisplay(report.status),
  }
}
