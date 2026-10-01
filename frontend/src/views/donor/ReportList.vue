<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Report list</h2>
        <div class="sub">View and track historical reports submitted for your listings</div>
      </div>
      <button class="btn-accent" @click="emit('create-report')">+ File new report</button>
    </div>

    <!-- 1. Search Toolbar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search by food name, recipient, or reason…" 
        class="search-input"
      >
      <select v-model="filterStatus">
        <option value="">All statuses</option>
        <option value="Under Review">Under Review</option>
        <option value="Resolved">Resolved</option>
        <option value="Dismissed">Dismissed</option>
      </select>
    </div>

    <!-- 2. Report History Table -->
    <div v-if="paginatedReports.length > 0">
      <table>
        <thead>
          <tr>
            <th>Food listing</th>
            <th>Reported recipient</th>
            <th>Transaction date & time</th>
            <th>Reason</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in paginatedReports" :key="r.id">
            <td>
              <strong>{{ r.foodName }}</strong>
            </td>
            <td>
              <span class="recipient-name">{{ r.recipient }}</span>
            </td>
            <td>{{ r.actualDate }}</td>
            <td>
              <div class="reason-text" :title="r.reason">{{ r.reason }}</div>
            </td>
            <td>
              <span class="badge-status" :class="r.statusClass">{{ r.status }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 3. Pagination Controls -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredReports.length }}</strong> reports
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
      No submitted reports match your search criteria.
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  reports: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['create-report'])

const searchKeyword = ref('')
const filterStatus = ref('')

const filteredReports = computed(() => {
  return props.reports.filter(r => {
    if (filterStatus.value && r.status !== filterStatus.value) {
      return false
    }
    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchFood = r.foodName.toLowerCase().includes(q)
      const matchRecipient = r.recipient.toLowerCase().includes(q)
      const matchReason = r.reason.toLowerCase().includes(q)
      if (!matchFood && !matchRecipient && !matchReason) return false
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
const paginationStart = computed(() => filteredReports.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredReports.value.length))
const paginatedReports = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredReports.value.slice(start, start + pageSize.value)
})
</script>

<style scoped>
.content-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
.btn-accent {
  background: var(--accent);
  color: #fff;
  border: none;
  padding: 10px 18px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 13.5px;
  cursor: pointer;
}
.btn-accent:hover { background: #A9781F; }

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

.recipient-name { font-weight: 700; color: var(--primary-dark); }
.reason-text {
  font-size: 12px;
  color: var(--text-muted);
  max-width: 260px;
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
.status-resolved { background: var(--success-tint); color: var(--success); }
.status-dismissed { background: var(--surface-2); color: var(--text-soft); }

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