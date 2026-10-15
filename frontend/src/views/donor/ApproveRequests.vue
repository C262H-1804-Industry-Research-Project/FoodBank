<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Requests to approve</h2>
        <div class="sub">Review incoming food requests from approved recipient organisations and verify collection handovers</div>
      </div>
    </div>

    <!-- 1. Filter / Status Tab Bar -->
    <div class="filter-bar">
      <div class="status-tabs">
        <button 
          :class="{ active: currentTab === 'all' }" 
          @click="currentTab = 'all'"
        >
          All ({{ requestsList.length }})
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
          :class="{ active: currentTab === 'collected' }" 
          @click="currentTab = 'collected'"
        >
          Collected ({{ collectedCount }})
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
              <div class="food-sub">Total available: {{ item.totalStock }}</div>
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
            <!-- Pickup code display -->
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
              <!-- Action for Pending -->
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

              <!-- Action for Approved: Confirm Handover -->
              <div v-else-if="item.status === 'Approved'" class="action-btns">
                <button 
                  class="btn-handover btn-sm" 
                  :disabled="busy"
                  @click="handleConfirmHandover(item)"
                >
                  Confirm Handover
                </button>
              </div>

              <!-- Action for Collected / Declined / Completed -->
              <span v-else class="action-done-text">
                {{ item.status === 'Collected' ? 'Collected ✓' : 'Processed' }}
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
    required: true
  }
})

const emit = defineEmits(['approve-request', 'decline-request', 'confirm-handover', 'reload'])

// Tab filter & Search keyword
const currentTab = ref('pending')
const searchKeyword = ref('')

const pendingCount = computed(() => props.requestsList.filter(r => r.status === 'Pending').length)
const approvedCount = computed(() => props.requestsList.filter(r => r.status === 'Approved').length)
const collectedCount = computed(() => props.requestsList.filter(r => r.status === 'Collected').length)

// Filtered list
const filteredRequests = computed(() => {
  return props.requestsList.filter(item => {
    // Tab filter
    if (currentTab.value === 'pending' && item.status !== 'Pending') return false
    if (currentTab.value === 'approved' && item.status !== 'Approved') return false
    if (currentTab.value === 'collected' && item.status !== 'Collected') return false

    // Search filter
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

// Pagination
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

function handleApprove(item) {
  emit('approve-request', item.id)
}

function handleDecline(item) {
  const confirmed = window.confirm(`Are you sure you want to decline this request from ${item.recipientOrg}?`)
  if (confirmed) {
    emit('decline-request', item.id)
  }
}

function handleConfirmHandover(item) {
  const code = item.pickupCode || item.code || 'N/A'
  const confirmed = window.confirm(
    `Confirm Handover:\n\nRecipient: ${item.recipientOrg}\nItem: ${item.foodName} (${item.quantity})\nPickup Code: ${code}\n\nHave you verified the pickup code and successfully handed over the food?`
  )
  if (confirmed) {
    // Update local status directly for instant feedback
    item.status = 'Collected'
    item.statusClass = 'status-collected'
    emit('confirm-handover', item.id)
  }
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

/* Filter & Tab bar */
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

/* Table styling */
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
  background: var(--primary-dark);
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

/* Status badges */
.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-pending { background: var(--amber-tint); color: var(--amber); }
.status-approved { background: var(--primary-tint); color: var(--primary-dark); }
.status-collected { background: var(--success-tint); color: var(--success); }
.status-declined { background: var(--danger-tint); color: var(--danger); }

/* Action buttons */
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

.btn-handover {
  background: var(--success, #1F5D50);
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: opacity .15s;
}
.btn-handover:hover { opacity: 0.9; }

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
</style>