<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Requests to approve</h2>
        <div class="sub">Review incoming food requests, verify pickup codes, and manage collection handovers</div>
      </div>
    </div>

    <!-- 1. Filter / Status Tab Bar -->
    <div class="filter-bar">
      <div class="status-tabs">
        <button 
          :class="{ active: currentTab === 'all' }" 
          @click="currentTab = 'all'"
        >
          All ({{ displayRequests.length }})
        </button>
        <button 
          :class="{ active: currentTab === 'pending' }" 
          @click="currentTab = 'pending'"
        >
          Pending ({{ pendingCount }})
        </button>
        <button 
          :class="{ active: currentTab === 'approved' }" 
          @click="currentTab = 'approved'"
        >
          Approved ({{ approvedCount }})
        </button>
        <button 
          :class="{ active: currentTab === 'expired' }" 
          @click="currentTab = 'expired'"
        >
          Expired ({{ expiredCount }})
        </button>
      </div>

      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search recipient, food item, or pickup code…" 
        class="search-input"
      >
    </div>

    <!-- 2. Requests Table -->
    <p v-if="loading" role="status">Loading requests…</p>
    <p v-else-if="error" role="alert">{{ error }} <button @click="emit('reload')">Retry</button></p>
    <div v-else-if="paginatedRequests.length > 0">
      <table>
        <thead>
          <tr>
            <th>Food item</th>
            <th>Recipient org / Contact</th>
            <th>Requested unit</th>
            <th>Pickup slot</th>
            <th>Pickup code</th>
            <th>Notes</th>
            <th>Status</th>
            <th style="text-align:right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedRequests" :key="item.id">
            <td>
              <div class="food-name">{{ item.foodName }}</div>
              <div class="food-sub">Left: {{ item.totalStock }}</div>
            </td>
            <td>
              <div class="recipient-org">{{ item.recipientOrg }}</div>
              <div class="recipient-contact">{{ item.contactName }} · {{ item.phone }}</div>
            </td>
            <td>
              <strong>{{ item.quantity }}</strong>
            </td>
            <td>
              <div class="slot-text">{{ item.slot }}</div>
            </td>
            <td>
              <code v-if="item.pickupCode || item.code" class="pcode">
                {{ item.pickupCode || item.code }}
              </code>
              <span v-else class="text-soft">—</span>
            </td>
            <td>
              <div class="notes-text" :title="item.notes">{{ item.notes || '—' }}</div>
            </td>
            <td>
              <span class="badge-status" :class="item.statusClass">{{ item.status }}</span>
            </td>
            <td style="text-align:right;">
              <!-- 1. Pending: Approve / Decline -->
              <div v-if="item.status === 'Pending'" class="action-btns">
                <button 
                  class="btn-approve btn-sm" 
                  :disabled="busy"
                  @click="handleApprove(item)"
                >
                  Approve
                </button>
                <button 
                  class="btn-decline btn-sm" 
                  :disabled="busy"
                  @click="handleDecline(item)"
                >
                  Decline
                </button>
              </div>

              <!-- 2. Approved: View pickup code & Confirm Handover -->
              <div v-else-if="item.status === 'Approved'" class="action-btns">
                <button 
                  class="btn-verify-code btn-sm" 
                  :disabled="busy"
                  @click="openVerifyModal(item)"
                >
                  View Code & Handover
                </button>
              </div>
              <div v-if="item.status === 'Approved' && item.handoverBy" class="handover-by">
                Handover by {{ item.handoverBy }}
              </div>

              <!-- 3. Expired: not collected by the handover deadline; correctable until the stock is released -->
              <div v-else-if="item.status === 'Expired' && !item.stockReleased" class="action-btns">
                <button 
                  class="btn-verify-code btn-sm" 
                  :disabled="busy"
                  @click="openVerifyModal(item)"
                >
                  Mark as collected
                </button>
              </div>
              <span v-else-if="item.status === 'Expired'" class="action-expired-text">
                Not collected
                <div class="stock-released">Stock released</div>
              </span>

              <!-- 4. Completed / Collected / Declined -->
              <span v-else class="action-done-text">
                {{ item.status === 'Completed' ? 'Completed ✓' : item.status }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 3. Bottom Pagination -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredRequests.length }}</strong> requests
        </div>
        <div class="pagination-controls">
          <button 
            class="page-btn" 
            :disabled="currentPage === 1" 
            @click="currentPage--"
          >
            ‹ Prev
          </button>
          
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button 
            class="page-btn" 
            :disabled="currentPage === totalPages" 
            @click="currentPage++"
          >
            Next ›
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-note">
      No requests found for this filter tab.
    </div>

    <!-- 4. Handover & Pickup Code Verification Modal -->
    <div class="modal-backdrop" :class="{ show: showVerifyModal }" @click.self="closeVerifyModal">
      <div class="modal-card" v-if="verifyingItem">
        <div class="modal-head">
          <h3>Verify Pickup Code & Confirm Handover</h3>
          <button type="button" class="btn-close" @click="closeVerifyModal">✕</button>
        </div>

        <div class="verify-summary">
          <div class="summary-line">
            <span class="label">Food item:</span>
            <strong>{{ verifyingItem.foodName }} ({{ verifyingItem.quantity }})</strong>
          </div>
          <div class="summary-line">
            <span class="label">Recipient organisation:</span>
            <strong>{{ verifyingItem.recipientOrg }}</strong>
          </div>
          <div class="summary-line">
            <span class="label">Scheduled slot:</span>
            <span>{{ verifyingItem.slot }}</span>
          </div>
        </div>

        <div class="code-box">
          <div class="code-label">Assigned Pickup Code</div>
          <template v-if="verifyingItem.pickupCode || verifyingItem.code">
            <div class="code-display">{{ verifyingItem.pickupCode || verifyingItem.code }}</div>
            <div class="code-hint">Ask the recipient courier to present this 6-digit code upon arrival.</div>
          </template>
          <div v-else class="code-hint">This listing does not use pickup codes.</div>
        </div>

        <div class="modal-foot">
          <button type="button" class="btn-outline" @click="closeVerifyModal">Cancel</button>
          <button 
            type="button" 
            class="btn-confirm-handover" 
            :disabled="busy"
            @click="confirmHandoverComplete"
          >
            Confirm Successful Handover
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  busy: Boolean,
  requestsList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['approve-request', 'decline-request', 'confirm-handover', 'reload'])

const displayRequests = computed(() => Array.isArray(props.requestsList) ? props.requestsList : [])

const currentTab = ref('pending')
const searchKeyword = ref('')

const pendingCount = computed(() => displayRequests.value.filter(r => r.status === 'Pending').length)
const approvedCount = computed(() => displayRequests.value.filter(r => r.status === 'Approved').length)
const expiredCount = computed(() => displayRequests.value.filter(r => r.status === 'Expired').length)

const filteredRequests = computed(() => {
  return displayRequests.value.filter(item => {
    // 1. Tab filter
    if (currentTab.value === 'pending' && item.status !== 'Pending') return false
    if (currentTab.value === 'approved' && item.status !== 'Approved') return false
    if (currentTab.value === 'expired' && item.status !== 'Expired') return false

    // 2. Search keyword
    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchFood = (item.foodName || '').toLowerCase().includes(q)
      const matchOrg = (item.recipientOrg || '').toLowerCase().includes(q)
      const matchContact = (item.contactName || '').toLowerCase().includes(q)
      const matchCode = (item.pickupCode || item.code || '').toLowerCase().includes(q)
      if (!matchFood && !matchOrg && !matchContact && !matchCode) return false
    }

    return true
  })
})

const currentPage = ref(1)
const pageSize = ref(5)

watch([currentTab, searchKeyword], () => {
  currentPage.value = 1
})

const totalPages = computed(() => Math.ceil(filteredRequests.value.length / pageSize.value) || 1)
watch(totalPages, count => { if (currentPage.value > count) currentPage.value = count })
const paginationStart = computed(() => filteredRequests.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredRequests.value.length))
const paginatedRequests = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRequests.value.slice(start, start + pageSize.value)
})

// Forward to parent emit directly so backend PATCH is properly invoked
function handleApprove(item) {
  emit('approve-request', item.id)
}

function handleDecline(item) {
  const confirmed = window.confirm(`Are you sure you want to decline this request from ${item.recipientOrg}?`)
  if (confirmed) {
    emit('decline-request', item.id)
  }
}

// Modal inspection & manual code confirmation
const showVerifyModal = ref(false)
const verifyingItem = ref(null)

function openVerifyModal(item) {
  verifyingItem.value = item
  showVerifyModal.value = true
}

function closeVerifyModal() {
  showVerifyModal.value = false
  verifyingItem.value = null
}

function confirmHandoverComplete() {
  if (!verifyingItem.value) return
  // Parent PATCHes the request and refreshes the row from the server response
  emit('confirm-handover', verifyingItem.value.id)
  closeVerifyModal()
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 12px 16px;
  margin-bottom: 22px;
  gap: 12px;
  flex-wrap: wrap;
}
.status-tabs {
  display: flex;
  gap: 6px;
}
.status-tabs button {
  padding: 7px 14px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--text-muted);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .12s ease;
}
.status-tabs button.active {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  color: #fff;
}
.search-input {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
  min-width: 240px;
}
.search-input:focus {
  outline: 2px solid var(--primary);
  border-color: var(--primary);
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

.food-name { font-weight: 700; color: var(--text); }
.food-sub { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }
.recipient-org { font-weight: 700; color: var(--primary-dark); }
.recipient-contact { font-size: 11.5px; color: var(--text-muted); margin-top: 2px; }
.slot-text { font-size: 12.5px; color: var(--text); }
.text-soft { color: var(--text-soft); font-size: 12px; }

code.pcode {
  background: #1B2925;
  color: #EADFBF;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
}

.notes-text {
  font-size: 12px;
  color: var(--text-muted);
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-pending { background: var(--amber-tint); color: var(--amber); }
.status-approved { background: var(--primary-tint); color: var(--primary-dark); }
.status-expired { background: var(--danger-tint); color: var(--danger); }
.status-completed { background: #E5F3EB; color: #1F5D50; }
.status-declined { background: var(--surface-2); color: var(--text-soft); }

.action-btns {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}
.btn-approve {
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}
.btn-approve:hover { background: var(--primary-dark); }

.btn-verify-code {
  background: #1F5D50;
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: opacity .15s;
}
.btn-verify-code:hover { opacity: 0.9; }

.btn-decline {
  background: transparent;
  border: 1.5px solid var(--danger);
  color: var(--danger);
  padding: 5px 10px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}
.btn-decline:hover { background: var(--danger-tint); }

.btn-sm { font-size: 12px; border-radius: 6px; }
.action-expired-text { font-size: 12px; color: var(--danger); font-weight: 700; }
.stock-released { font-size: 11px; color: var(--text-soft); font-weight: 600; margin-top: 2px; }
.handover-by { font-size: 11px; color: var(--text-soft); margin-top: 4px; }
.action-done-text { font-size: 12px; color: #1F5D50; font-weight: 700; }

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
  transition: all .12s ease;
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

.modal-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15,20,17,.6);
  z-index: 200;
  align-items: center;
  justify-content: center;
}
.modal-backdrop.show { display: flex; }
.modal-card {
  background: var(--surface);
  border-radius: var(--r-lg);
  width: 480px;
  max-width: 90vw;
  padding: 24px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
}
.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.modal-head h3 { font-size: 17px; margin: 0; }
.btn-close {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: var(--text-muted);
}
.verify-summary {
  background: var(--surface-2);
  border-radius: var(--r-sm);
  padding: 12px 14px;
  margin-bottom: 16px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.summary-line { display: flex; justify-content: space-between; }
.summary-line .label { color: var(--text-muted); }

.code-box {
  text-align: center;
  padding: 18px;
  border: 1.5px dashed var(--primary);
  background: #FAF8F2;
  border-radius: var(--r-md);
  margin-bottom: 20px;
}
.code-label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; font-weight: 700; letter-spacing: .05em; }
.code-display { font-size: 28px; font-weight: 900; letter-spacing: .15em; color: var(--primary-dark); margin: 6px 0; font-family: monospace; }
.code-hint { font-size: 11.5px; color: var(--text-soft); }

.modal-foot { display: flex; justify-content: flex-end; gap: 10px; }
.btn-outline {
  background: transparent;
  border: 1.5px solid var(--border);
  padding: 8px 14px;
  border-radius: var(--r-sm);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.btn-confirm-handover {
  background: #1F5D50;
  color: #fff;
  border: none;
  padding: 9px 18px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}
.btn-confirm-handover:hover { background: #17473D; }
</style>