<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Collection history</h2>
        <div class="sub">Past food rescue collections completed by your organisation</div>
      </div>
    </div>

    <!-- Filter & Search Toolbar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search this page by food item or donor…"
        class="search-input"
      >
      <select v-model="filterCategory">
        <option value="">All categories</option>
        <option value="Bakery">Bakery</option>
        <option value="Produce">Produce</option>
        <option value="Dairy">Dairy</option>
        <option value="Pantry">Pantry</option>
      </select>
    </div>

    <!-- History Table -->
    <div v-if="filteredHistory.length > 0">
      <table>
        <thead>
          <tr>
            <th>Food item</th>
            <th>Category</th>
            <th>Quantity collected</th>
            <th>Donor organisation</th>
            <th>Collection date & slot</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredHistory" :key="item.id">
            <td>
              <strong>{{ item.foodName }}</strong>
            </td>
            <td>
              <span class="cat-pill">{{ item.category }}</span>
            </td>
            <td>{{ item.quantity }}</td>
            <td>
              <span class="donor-name">{{ item.donor }}</span>
            </td>
            <td>{{ item.collectedDate }}</td>
            <td>
              <span class="badge-status status-completed">Collected</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Bottom Pagination -->
      <div v-if="totalElements > 0" class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ totalElements }}</strong> completed collections
        </div>
        <div class="pagination-controls">
          <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">‹ Prev</button>
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">Next ›</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-note">
      No past collection records match your search criteria.
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  historyList: {
    type: Array,
    default: () => []
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 5
  },
  totalPages: {
    type: Number,
    default: 0
  },
  totalElements: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['page-change'])

const searchKeyword = ref('')
const filterCategory = ref('')

const filteredHistory = computed(() => {
  return props.historyList.filter(item => {
    if (filterCategory.value && item.category !== filterCategory.value) return false
    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchFood = item.foodName.toLowerCase().includes(q)
      const matchDonor = item.donor.toLowerCase().includes(q)
      if (!matchFood && !matchDonor) return false
    }
    return true
  })
})

const paginationStart = computed(() => props.historyList.length === 0 ? 0 : (props.currentPage - 1) * props.pageSize + 1)
const paginationEnd = computed(() => Math.min((props.currentPage - 1) * props.pageSize + props.historyList.length, props.totalElements))

function changePage(page) {
  if (page < 1 || page > props.totalPages || page === props.currentPage) return
  emit('page-change', page)
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

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

.cat-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  background: var(--surface-2);
  border-radius: 4px;
  color: var(--text-muted);
}
.donor-name { font-weight: 700; color: var(--primary-dark); }

.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-completed { background: var(--success-tint); color: var(--success); }

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
</style>
