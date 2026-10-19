<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Donation history</h2>
        <div class="sub">Successful and failed food collection outcomes</div>
      </div>
    </div>

    <!-- 1. Status Tabs -->
    <div class="tab-toolbar">
      <div class="status-tabs">
        <button 
          :class="{ active: currentTab === 'all' }" 
          @click="currentTab = 'all'"
        >
          All ({{ displayList.length }})
        </button>
        <button 
          :class="{ active: currentTab === 'success' }" 
          @click="currentTab = 'success'"
        >
          Successful ({{ successCount }})
        </button>
        <button 
          :class="{ active: currentTab === 'failed' }" 
          @click="currentTab = 'failed'"
        >
          Failed ({{ failedCount }})
        </button>
      </div>
    </div>

    <!-- 2. Search & Filter Bar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search past food item or collector…" 
        class="search-input"
      >
      <select v-model="filterCategory">
        <option value="">All categories</option>
        <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
      </select>
    </div>

    <p v-if="error" class="form-error" role="alert">
      {{ error }} <button class="btn-outline btn-sm" @click="emit('reload')">Retry</button>
    </p>
    <div v-if="loading" class="empty-note" role="status">Loading your donation history...</div>

    <!-- 3. Completed History Table -->
    <div v-else-if="!error && paginatedHistory.length > 0">
      <table>
        <thead>
          <tr>
            <th>Food item</th>
            <th>Category</th>
            <th>Quantity collected</th>
            <th>Collected by</th>
            <th>Outcome date</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedHistory" :key="item.id">
            <td>
              <strong>{{ item.foodName }}</strong>
            </td>
            <td>
              <span class="cat-pill">{{ item.category }}</span>
            </td>
            <td>{{ item.quantity }}</td>
            <td>
              <span class="org-name">{{ item.collectedBy }}</span>
            </td>
            <td>{{ item.completedDate }}</td>
            <!-- Plain text status display without red/green backgrounds -->
            <td>
              <span class="status-plain-text">
                {{ item.status }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 4. Bottom Pagination -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredHistory.length }}</strong> donation records
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

    <div v-else-if="!error" class="empty-note">
      No donation records found matching criteria.
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const emit = defineEmits(['reload'])
const props = defineProps({
  historyList: { type: Array, default: () => [] },
  loading: Boolean,
  error: { type: String, default: '' },
})

const displayList = computed(() => {
  return props.historyList && props.historyList.length > 0 ? props.historyList : []
})

const categories = computed(() => [...new Set(displayList.value.map(item => item.category).filter(Boolean))].sort())
const currentTab = ref('all')
const searchKeyword = ref('')
const filterCategory = ref('')

const successCount = computed(() => displayList.value.filter(item => item.status === 'Completed').length)
const failedCount = computed(() => displayList.value.filter(item => item.status === 'Failed').length)

const filteredHistory = computed(() => {
  return displayList.value.filter(item => {
    // 1. Tab filter
    if (currentTab.value === 'success' && item.status !== 'Completed') return false
    if (currentTab.value === 'failed' && item.status !== 'Failed') return false

    // 2. Category filter
    if (filterCategory.value && item.category !== filterCategory.value) {
      return false
    }

    // 3. Search keyword matching
    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchFood = (item.foodName || '').toLowerCase().includes(q)
      const matchOrg = (item.collectedBy || '').toLowerCase().includes(q)
      if (!matchFood && !matchOrg) return false
    }
    return true
  })
})

const currentPage = ref(1)
const pageSize = ref(5)

watch([currentTab, searchKeyword, filterCategory, () => props.historyList], () => {
  currentPage.value = 1
})

const totalPages = computed(() => Math.ceil(filteredHistory.value.length / pageSize.value) || 1)
const paginationStart = computed(() => filteredHistory.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredHistory.value.length))

const paginatedHistory = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredHistory.value.slice(start, start + pageSize.value)
})
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }

.tab-toolbar {
  margin-bottom: 14px;
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
  font-size: 13.5px;
}
.filter-bar select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13.5px;
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
  font-size: 13.5px;
  border-top: 1px solid var(--border);
  color: var(--text);
  vertical-align: middle;
}
tbody tr:hover { background: #FBFAF6; }

.cat-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  background: var(--surface-2);
  border-radius: 4px;
  color: var(--text-muted);
}
.org-name { font-weight: 700; color: var(--primary-dark); }

/* Normal text display for status without colored background pills */
.status-plain-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.pagination-info { font-size: 13px; color: var(--text-muted); }
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
  font-size: 13px;
  color: var(--text-soft);
  padding: 36px 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 16px;
}
</style>
