<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>User account management</h2>
        <div class="sub">Review registered donors and recipient organisations, audit accounts, and deactivate non-compliant users</div>
      </div>
    </div>
    <p v-if="loading">Loading accounts…</p>
    <p v-if="error" role="alert">{{ error }} <button @click="emit('reload')">Retry</button></p>

    <!-- 1. Filter Toolbar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search by User ID, name, organisation, or email…" 
        class="search-input"
      >

      <select v-model="filterRole">
        <option value="">All roles</option>
        <option value="Donor">Donor</option>
        <option value="Recipient">Recipient</option>
      </select>

      <select v-model="filterStatus">
        <option value="">All statuses</option>
        <option value="Active">Active</option>
        <option value="Under review">Under review</option>
        <option value="Deactivated">Deactivated</option>
      </select>

      <button 
        v-if="hasActiveFilters" 
        type="button" 
        class="btn-reset" 
        @click="resetFilters"
      >
        Reset filters
      </button>
    </div>

    <!-- 2. Users Table -->
    <div v-if="paginatedUsers.length > 0">
      <table>
        <thead>
          <tr>
            <th>User ID</th>
            <th>Name / Organisation</th>
            <th>Role</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Status</th>
            <th style="text-align:right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="`${user.role}-${user.userId}`">
            <td>
              <code class="uid-tag">#{{ user.userId }}</code>
            </td>
            <td>
              <div class="user-name">{{ user.name }}</div>
              <div v-if="user.orgName" class="user-org">{{ user.orgName }}</div>
            </td>
            <td>
              <span class="role-badge" :class="user.role === 'Donor' ? 'role-donor' : 'role-recipient'">
                {{ user.role }}
              </span>
            </td>
            <td>{{ user.email }}</td>
            <td>{{ user.phone || '—' }}</td>
            <td>
              <span class="badge-status" :class="statusClass(user.status)">
                {{ user.status }}
              </span>
            </td>
            <td style="text-align:right;">
              <!-- Replaced "Detail" button with "Deactivate" / "Reactivate" toggle action -->
              <button 
                v-if="user.status === 'Active'" 
                class="btn-deactivate btn-sm" 
                @click="handleDeactivate(user)"
              >
                Deactivate
              </button>
              <button 
                v-else-if="user.status === 'Deactivated'" 
                class="btn-reactivate btn-sm" 
                @click="handleReactivate(user)"
              >
                Reactivate
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 3. Bottom Pagination -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredUsers.length }}</strong> users
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
    <div v-else-if="!loading" class="empty-note">
      No users found matching your search and filter criteria.
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  loading: Boolean,
  error: String,
  usersList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['deactivate-user', 'reactivate-user', 'reload'])

function statusClass(status) {
  return { Active: 'status-active', 'Under review': 'status-review' }[status] || 'status-deactivated'
}

const users = ref([...props.usersList])
watch(() => props.usersList, (newVal) => {
  users.value = [...newVal]
}, { deep: true })

const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')

const hasActiveFilters = computed(() => {
  return searchKeyword.value.trim() !== '' || filterRole.value !== '' || filterStatus.value !== ''
})

function resetFilters() {
  searchKeyword.value = ''
  filterRole.value = ''
  filterStatus.value = ''
}

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    if (filterRole.value && user.role !== filterRole.value) return false
    if (filterStatus.value && user.status !== filterStatus.value) return false

    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchId = String(user.userId).toLowerCase().includes(q)
      const matchName = (user.name || '').toLowerCase().includes(q)
      const matchOrg = (user.orgName || '').toLowerCase().includes(q)
      const matchEmail = (user.email || '').toLowerCase().includes(q)
      if (!matchId && !matchName && !matchOrg && !matchEmail) return false
    }

    return true
  })
})

const currentPage = ref(1)
const pageSize = ref(5)

watch([searchKeyword, filterRole, filterStatus], () => {
  currentPage.value = 1
})

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize.value) || 1)
const paginationStart = computed(() => filteredUsers.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredUsers.value.length))
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

function handleDeactivate(user) {
  const confirmed = window.confirm(`Are you sure you want to DEACTIVATE user ${user.name} (${user.userId})? They will immediately lose access to post or claim listings.`)
  if (confirmed) {
    user.status = 'Deactivated'
    emit('deactivate-user', user.userId)
  }
}

function handleReactivate(user) {
  const confirmed = window.confirm(`Restore account access for user ${user.name} (${user.userId})?`)
  if (confirmed) {
    user.status = 'Active'
    emit('reactivate-user', user.userId)
  }
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
  min-width: 260px;
}
.filter-bar select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}
.btn-reset {
  background: transparent;
  border: 1px dashed var(--danger);
  color: var(--danger);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  margin-left: auto;
}
.btn-reset:hover { background: var(--danger-tint); }

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

.uid-tag {
  background: var(--surface-2);
  color: var(--text);
  padding: 3px 7px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
}
.user-name { font-weight: 700; color: var(--text); }
.user-org { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }

.role-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 12px;
}
.role-donor { background: var(--primary-tint); color: var(--primary-dark); }
.role-recipient { background: var(--amber-tint); color: var(--amber); }

.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-active { background: var(--success-tint); color: var(--success); }
.status-deactivated { background: var(--danger-tint); color: var(--danger); }
.status-review { background: var(--amber-tint); color: var(--amber); }

.btn-deactivate {
  background: transparent;
  border: 1.5px solid var(--danger);
  color: var(--danger);
  padding: 5px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s ease;
}
.btn-deactivate:hover {
  background: var(--danger);
  color: #fff;
}

.btn-reactivate {
  background: transparent;
  border: 1.5px solid var(--primary);
  color: var(--primary-dark);
  padding: 5px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}
.btn-reactivate:hover {
  background: var(--primary-tint);
}

.btn-sm { font-size: 12px; border-radius: 6px; }

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