<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Misuse reports & disputes</h2>
        <div class="sub">Review reported incidents, configure penalty policies, and enforce automated or custom bans</div>
      </div>
    </div>

    <!-- 1. Offence Policy Configuration Widget -->
    <div class="policy-widget">
      <div class="pw-head">
        <div class="pw-title">
          <span class="pw-icon">⚙️</span>
          <div>
            <strong>Default Ban Duration Policy</strong>
            <div class="pw-sub">Default suspension period applied when enforcing bans for repeated offences</div>
          </div>
        </div>
        <button class="btn-save-policy" @click="savePolicy" :disabled="policyBusy || !policyReady">
          {{ policySaved ? '✓ Saved' : 'Save policy' }}
        </button>
      </div>

      <div class="pw-grid">
        <div class="pw-card">
          <div class="pw-badge badge-1st">1st Offence</div>
          <div class="pw-input-wrap">
            <input type="number" :disabled="policyBusy || !policyReady" @input="policySaved = false" v-model.number="banPolicy.first" min="1" max="365">
            <span class="pw-unit">days ban</span>
          </div>
          <div class="pw-desc">Minor infractions (e.g. slight delay, unnotified cancellation)</div>
        </div>

        <div class="pw-card">
          <div class="pw-badge badge-2nd">2nd Offence</div>
          <div class="pw-input-wrap">
            <input type="number" :disabled="policyBusy || !policyReady" @input="policySaved = false" v-model.number="banPolicy.second" min="1" max="365">
            <span class="pw-unit">days ban</span>
          </div>
          <div class="pw-desc">Repeated no-shows or collection discrepancies</div>
        </div>

        <div class="pw-card">
          <div class="pw-badge badge-3rd">3rd Offence</div>
          <div class="pw-input-wrap">
            <input type="number" :disabled="policyBusy || !policyReady" @input="policySaved = false" v-model.number="banPolicy.third" min="1" max="365">
            <span class="pw-unit">days ban</span>
          </div>
          <div class="pw-desc">Chronic misconduct or abusive behavior (account deactivation)</div>
        </div>
      </div>
    </div>

    <p v-if="policyError" role="alert">{{ policyError }} <button @click="loadPolicy">Retry policy</button></p>
    <p v-if="loading">Loading reports…</p>
    <p v-if="loadError" role="alert">{{ loadError }} <button @click="loadReports">Retry</button></p>
    <p v-if="processError" role="alert">{{ processError }}</p>
    <!-- 2. Search & Filter Bar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search reported user, reporter, food item, or reason…" 
        class="search-input"
      >
      <select v-model="filterStatus">
        <option value="">All statuses</option>
        <option value="Pending">Pending Review</option>
        <option value="In review">In review</option>
        <option value="Resolved">Resolved / Actioned</option>
        <option value="Dismissed">Dismissed</option>
      </select>
    </div>

    <!-- 3. Reports Table -->
    <div v-if="paginatedReports.length > 0">
      <table>
        <thead>
          <tr>
            <th>Reported user / entity</th>
            <th>Offence history</th>
            <th>Related food listing</th>
            <th>Reported by</th>
            <th>Reason</th>
            <th>Status</th>
            <th style="text-align:right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in paginatedReports" :key="r.id">
            <td>
              <div class="user-main-name">{{ r.reportedUser }}</div>
              <div class="user-role-sub">{{ r.reportedRole }} · ID: {{ r.reportedId }}</div>
            </td>
            <td>
              <span class="offence-count-badge" :class="getOffenceClass(r.offenceCount)">
                {{ r.offenceCount }} prior ban(s)
              </span>
            </td>
            <td>
              <strong>{{ r.foodName }}</strong>
              <div class="slot-sub">{{ r.timeslot }}</div>
            </td>
            <td>
              <div class="reporter-name">{{ r.reporter }}</div>
            </td>
            <td>
              <div class="reason-text" :title="r.reason">{{ r.reason }}</div>
            </td>
            <td>
              <span class="badge-status" :class="getStatusClass(r.status)">{{ r.status }}</span>
            </td>
            <td style="text-align:right;">
              <div v-if="r.status === 'Pending' || r.status === 'In review'" class="action-btns">
                <button class="btn-process btn-sm" :disabled="processing || loading" @click="openProcessModal(r)">
                  Process
                </button>
                <button class="btn-dismiss btn-sm" :disabled="processing || loading" @click="handleDismiss(r)">
                  Dismiss
                </button>
              </div>
              <span v-else class="action-done-text">Completed</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 4. Bottom Pagination -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredReports.length }}</strong> reports
        </div>
        <div class="pagination-controls">
          <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">‹ Prev</button>
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            @click="currentPage = page"
          >
            {{ page }}
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="currentPage++">Next ›</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-note">
      No misuse reports found matching your criteria.
    </div>

    <!-- 5. Process & Ban Enforcement Modal -->
    <div class="modal-backdrop" :class="{ show: showProcessModal }" @click.self="!processing && (showProcessModal = false)">
      <div class="modal-card" v-if="activeReport">
        <div class="modal-head">
          <h3>Enforce Penalty</h3>
          <span class="offence-count-badge" :class="getOffenceClass(activeReport.offenceCount)">
            Prior bans: {{ activeReport.offenceCount }}
          </span>
        </div>

        <div class="modal-summary">
          <div><strong>Target:</strong> {{ activeReport.reportedUser }} ({{ activeReport.reportedRole }})</div>
          <div><strong>Details:</strong> {{ activeReport.details || 'None' }}</div>
          <div v-if="activeReport.evidenceImageUrl"><strong>Evidence:</strong> <a :href="activeReport.evidenceImageUrl" target="_blank" rel="noopener noreferrer">View evidence</a></div>
          <div><strong>Reported For:</strong> {{ activeReport.reason }}</div>
        </div>

        <div class="field">
          <label class="field-label">Penalty Action</label>
          <select v-model="processAction" :disabled="processing">
            <option value="review">Mark as In Review</option>
            <option value="ban">Apply Temporary Suspension</option>
            <option value="warning">Record Warning (No Suspension)</option>
            <option value="deactivate">Permanent Suspension</option>
          </select>
        </div>

        <div v-if="processAction === 'ban'" class="field">
          <label class="field-label">
            Suspension Duration (Days) 
            <span class="policy-hint">• Auto-suggested from {{ getOffenceStage(activeReport.offenceCount + 1) }} policy</span>
          </label>
          <input type="number" :disabled="processing" v-model.number="banDaysInput" min="1" max="365">
        </div>

        <div class="field">
          <label class="field-label">Admin Resolution Note</label>
          <textarea :disabled="processing" v-model="adminNote" rows="3" placeholder="Explain the rationale for this moderation action (saved with the report)…"></textarea>
        </div>

        <div class="modal-foot">
          <button type="button" class="btn-outline" :disabled="processing" @click="showProcessModal = false">Cancel</button>
          <button type="button" class="btn-danger-confirm" :disabled="processing" @click="confirmProcess">
            Confirm & Apply Penalty
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'

import { fetchAdminReports, processAdminReport, fetchBanPolicy, saveBanPolicy, adminReportRow } from '../../api/adminModeration'

const emit = defineEmits(['report-actioned'])

// 1. Configurable Default Penalty Policy Widget state
const banPolicy = reactive({
  first: 3,
  second: 7,
  third: 30
})

const policySaved = ref(false)
const reports = ref([])
const loading = ref(false)
const loadError = ref('')
const policyError = ref('')
const policyBusy = ref(false)
const policyReady = ref(false)
const processing = ref(false)
const processError = ref('')
async function loadReports() {
  loading.value = true; loadError.value = ''
  try {
    const rows = await fetchAdminReports()
    if (!Array.isArray(rows)) throw new Error('Invalid report response')
    reports.value = rows.map(adminReportRow)
  } catch (error) { loadError.value = error.message }
  finally { loading.value = false }
}
async function loadPolicy() {
  policyError.value = ''; policyReady.value = false
  try { Object.assign(banPolicy, await fetchBanPolicy()); policyReady.value = true }
  catch (error) { policyError.value = error.message }
}
onMounted(() => { loadReports(); loadPolicy() })
async function savePolicy() {
  if (policyBusy.value || !policyReady.value) return
  policySaved.value = false; policyError.value = ''
  if (!Object.values(banPolicy).every(value => Number.isInteger(value) && value >= 1 && value <= 365)) {
    policyError.value = 'Each duration must be an integer between 1 and 365'; return
  }
  policyBusy.value = true
  try { Object.assign(banPolicy, await saveBanPolicy({ ...banPolicy })); policySaved.value = true }
  catch (error) { policyError.value = error.message }
  finally { policyBusy.value = false }
}

const searchKeyword = ref('')
const filterStatus = ref('')

const filteredReports = computed(() => {
  return reports.value.filter(r => {
    if (filterStatus.value && r.status !== filterStatus.value) return false
    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchUser = r.reportedUser.toLowerCase().includes(q)
      const matchReporter = r.reporter.toLowerCase().includes(q)
      const matchFood = r.foodName.toLowerCase().includes(q)
      const matchReason = r.reason.toLowerCase().includes(q)
      if (!matchUser && !matchReporter && !matchFood && !matchReason) return false
    }
    return true
  })
})

const currentPage = ref(1)
const pageSize = ref(5)

watch([searchKeyword, filterStatus], () => {
  currentPage.value = 1
})

const totalPages = computed(() => Math.ceil(filteredReports.value.length / pageSize.value) || 1)
watch(totalPages, pages => { currentPage.value = Math.min(currentPage.value, pages) })
const paginationStart = computed(() => filteredReports.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredReports.value.length))
const paginatedReports = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredReports.value.slice(start, start + pageSize.value)
})

function getOffenceClass(count) {
  if (count === 1) return 'offence-1'
  if (count === 2) return 'offence-2'
  return 'offence-3'
}

function getStatusClass(status) {
  if (status === 'Pending' || status === 'In review') return 'status-pending'
  if (status === 'Resolved') return 'status-resolved'
  return 'status-dismissed'
}

function getOffenceStage(count) {
  if (count === 1) return '1st offence'
  if (count === 2) return '2nd offence'
  return '3rd offence'
}

// 3. Process & Ban Enforcement
const showProcessModal = ref(false)
const activeReport = ref(null)
const processAction = ref('ban')
const banDaysInput = ref(3)
const adminNote = ref('')

function openProcessModal(report) {
  if (processing.value) return
  processError.value = ''
  activeReport.value = report
  adminNote.value = ''
  processAction.value = 'ban'

  // Automatically suggest days according to the policy widget configuration
  if (report.offenceCount === 0) {
    banDaysInput.value = banPolicy.first
  } else if (report.offenceCount === 1) {
    banDaysInput.value = banPolicy.second
  } else {
    banDaysInput.value = banPolicy.third
  }

  showProcessModal.value = true
}

async function applyAction(report, body) {
  if (processing.value) return false
  processing.value = true; processError.value = ''
  try {
    const result = await processAdminReport(report.id, body)
    if (result?.reportId !== report.id || result.status !== body.status) throw new Error('Invalid process response')
    const index = reports.value.findIndex(item => item.id === report.id)
    if (index >= 0) reports.value[index] = adminReportRow(result)
    emit('report-actioned', result)
    await loadReports()
    return true
  } catch (error) { processError.value = error.message; return false }
  finally { processing.value = false }
}
async function handleDismiss(report) {
  if (processing.value || !window.confirm(`Dismiss report against ${report.reportedUser}?`)) return
  await applyAction(report, { status: 'DISMISSED', action: 'DISMISS', adminNote: null, outcome: 'Dismissed' })
}
async function confirmProcess() {
  if (!activeReport.value || processing.value) return
  if (processAction.value === 'ban' && (!Number.isInteger(banDaysInput.value) || banDaysInput.value < 1 || banDaysInput.value > 365)) {
    processError.value = 'Duration must be an integer between 1 and 365'; return
  }
  const action = processAction.value.toUpperCase()
  const body = { status: action === 'REVIEW' ? 'IN_REVIEW' : 'RESOLVED', action,
    adminNote: adminNote.value.trim() || null,
    outcome: action === 'BAN' ? `Temporary suspension: ${banDaysInput.value} days` : action === 'DEACTIVATE' ? 'Permanent suspension' : action === 'WARNING' ? 'Warning recorded' : 'Under review',
    ...(action === 'BAN' ? { banDays: banDaysInput.value } : {}) }
  if (await applyAction(activeReport.value, body)) showProcessModal.value = false
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

/* Policy Configuration Widget */
.policy-widget {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  padding: 18px 20px;
  margin-bottom: 24px;
}
.pw-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pw-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pw-icon { font-size: 22px; }
.pw-title strong { font-size: 15px; color: var(--text); }
.pw-sub { font-size: 12px; color: var(--text-soft); margin-top: 2px; }

.btn-save-policy {
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 7px 16px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12.5px;
  cursor: pointer;
}
.btn-save-policy:hover { background: var(--primary-dark); }

.pw-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.pw-card {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 14px 16px;
}
.pw-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 12px;
  margin-bottom: 8px;
}
.badge-1st { background: #E5F3EB; color: #1F5D50; }
.badge-2nd { background: #FEF3E2; color: #B37D28; }
.badge-3rd { background: #FDECEC; color: #C93B3B; }

.pw-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.pw-input-wrap input {
  width: 70px;
  padding: 6px 8px;
  border: 1.5px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 15px;
  font-weight: 700;
  text-align: center;
}
.pw-input-wrap input:focus {
  outline: 2px solid var(--primary);
  border-color: var(--primary);
}
.pw-unit { font-size: 13px; font-weight: 700; color: var(--text); }
.pw-desc { font-size: 11px; color: var(--text-muted); line-height: 1.4; }

/* Filter bar */
.filter-bar {
  display: flex;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 12px 16px;
  margin-bottom: 20px;
  align-items: center;
}
.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}
.filter-bar select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  overflow: hidden;
}
thead th {
  text-align: left;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: .05em;
  color: var(--text-soft);
  background: var(--surface-2);
  padding: 12px 16px;
  font-weight: 700;
}
tbody td {
  padding: 14px 16px;
  font-size: 13px;
  border-top: 1px solid var(--border);
  color: var(--text);
  vertical-align: middle;
}
tbody tr:hover { background: #FBFAF6; }

.user-main-name { font-weight: 700; color: var(--text); }
.user-role-sub { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }
.slot-sub { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }
.reporter-name { font-weight: 600; color: var(--primary-dark); }
.reason-text {
  font-size: 12px;
  color: var(--text-muted);
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.offence-count-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 3px 9px;
  border-radius: 12px;
}
.offence-1 { background: #E5F3EB; color: #1F5D50; }
.offence-2 { background: #FEF3E2; color: #B37D28; }
.offence-3 { background: #FDECEC; color: #C93B3B; }

.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-pending { background: var(--amber-tint); color: var(--amber); }
.status-resolved { background: var(--success-tint); color: var(--success); }
.status-dismissed { background: var(--surface-2); color: var(--text-soft); }

.action-btns { display: flex; gap: 6px; justify-content: flex-end; }
.btn-process {
  background: var(--danger);
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}
.btn-process:hover { opacity: 0.9; }
.btn-dismiss {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 5px 10px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}
.btn-dismiss:hover { background: var(--surface-2); }
.btn-sm { font-size: 12px; border-radius: 6px; }
.action-done-text { font-size: 12px; color: var(--text-soft); font-weight: 600; }

/* Pagination controls */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.pagination-info { font-size: 12.5px; color: var(--text-muted); }
.pagination-controls { display: flex; gap: 6px; align-items: center; }
.page-btn, .page-number-btn {
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text);
  border-radius: var(--r-sm);
  padding: 6px 12px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.page-btn:hover:not(:disabled), .page-number-btn:hover:not(.active) {
  border-color: var(--primary);
  color: var(--primary-dark);
}
.page-number-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.page-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.empty-note {
  font-size: 12.5px;
  color: var(--text-soft);
  padding: 36px 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 16px;
}

/* Modal */
.modal-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15,20,17,.5);
  z-index: 200;
  align-items: center;
  justify-content: center;
}
.modal-backdrop.show { display: flex; }
.modal-card {
  background: var(--surface);
  border-radius: var(--r-lg);
  width: 460px;
  max-width: 90vw;
  padding: 24px;
}
.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.modal-head h3 { font-size: 18px; margin: 0; }
.modal-summary {
  background: var(--surface-2);
  padding: 12px 14px;
  border-radius: var(--r-sm);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 16px;
}
.field { margin-bottom: 14px; }
.field-label { font-size: 12.5px; font-weight: 700; display: block; margin-bottom: 6px; }
.policy-hint { font-size: 11px; color: var(--primary-dark); font-weight: 600; }
.field input, .field select, .field textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13.5px;
  box-sizing: border-box;
}
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-outline {
  background: transparent;
  border: 1.5px solid var(--border);
  padding: 8px 14px;
  border-radius: var(--r-sm);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.btn-danger-confirm {
  background: var(--danger);
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: var(--r-sm);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 768px) {
  .pw-grid { grid-template-columns: 1fr; }
}
</style>