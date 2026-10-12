<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>User account management</h2>
        <div class="sub">Review registered donors and recipient organisations, audit accounts, and deactivate non-compliant users</div>
      </div>
    </div>
    <p v-if="isLoading">Loading accounts…</p>
    <p v-if="loadError" role="alert">{{ loadError }} <button @click="loadAccounts">Retry</button></p>

    <!-- 1. Filter Toolbar -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search by User ID, name, email, or suburb…" 
        class="search-input"
      >

      <select v-model="filterRole">
        <option value="">All roles</option>
        <option value="DONOR">Donor</option>
        <option value="RECIPIENT">Recipient</option>
      </select>

      <select v-model="filterStatus">
        <option value="">All statuses</option>
        <option value="ACTIVE">Active</option>
        <option value="UNDER_REVIEW">Under review</option>
        <option value="BANNED">Deactivated</option>
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
    <div v-if="users.length > 0">
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
          <tr v-for="user in users" :key="`${user.role}-${user.userId}`">
            <td>
              <code class="uid-tag">#{{ user.userId }}</code>
            </td>
            <td>
              <div class="user-name">{{ user.name }}</div>
              <div v-if="user.location" class="user-org">{{ user.location }}</div>
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
              <button 
                v-if="user.status === 'Active'" 
                class="btn-deactivate btn-sm" 
                :disabled="busy"
                @click="openDeactivate(user)"
              >
                Deactivate
              </button>
              <button 
                v-else-if="user.status === 'Deactivated'" 
                class="btn-reactivate btn-sm" 
                :disabled="busy"
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
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ totalElements }}</strong> users
        </div>
        <div class="pagination-controls">
          <button 
            class="page-btn" 
            :disabled="currentPage === 1 || isLoading" 
            @click="currentPage--"
          >
            ‹ Prev
          </button>
          
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            :disabled="isLoading"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button 
            class="page-btn" 
            :disabled="currentPage === totalPages || isLoading" 
            @click="currentPage++"
          >
            Next ›
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!isLoading && !loadError" class="empty-note">
      No users found matching your search and filter criteria.
    </div>

    <!-- 4. Deactivate Modal -->
    <div class="modal-backdrop" :class="{ show: deactivateTarget }" @click.self="!busy && closeDeactivate()">
      <div class="modal-card" v-if="deactivateTarget">
        <div class="modal-head">
          <h3>Deactivate account</h3>
        </div>
        <div class="modal-summary">
          <div><strong>Account:</strong> {{ deactivateTarget.name }} ({{ deactivateTarget.role }}, #{{ deactivateTarget.userId }})</div>
          <div>They will be logged out and permanently lose access until an admin reactivates them.</div>
        </div>
        <div class="field">
          <label class="field-label">Reason</label>
          <textarea v-model="deactivateReason" :disabled="busy" rows="3" maxlength="500" placeholder="Explain why this account is being deactivated…"></textarea>
        </div>
        <div class="modal-foot">
          <button type="button" class="btn-outline" :disabled="busy" @click="closeDeactivate">Cancel</button>
          <button type="button" class="btn-danger-confirm" :disabled="busy || !deactivateReason.trim()" @click="confirmDeactivate">
            Deactivate account
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { fetchAdminAccounts, deactivateUser, reactivateUser } from '../../api/adminModeration'

const PAGE_SIZE = 10

function statusClass(status) {
  return { Active: 'status-active', 'Under review': 'status-review' }[status] || 'status-deactivated'
}

// Search, role and status filtering and paging all happen on the server
const searchKeyword = ref('')
// Backend codes; '' means no filter
const filterRole = ref('')
const filterStatus = ref('')
const currentPage = ref(1)

const users = ref([])
const totalElements = ref(0)
const totalPages = ref(1)
const isLoading = ref(true)
const loadError = ref('')
let loadController = null

async function loadAccounts() {
  // Abort an older request so a slow response cannot overwrite newer results
  loadController?.abort()
  const controller = new AbortController()
  loadController = controller
  isLoading.value = true
  loadError.value = ''
  try {
    const result = await fetchAdminAccounts({
      page: currentPage.value - 1,
      size: PAGE_SIZE,
      role: filterRole.value,
      status: filterStatus.value,
      search: searchKeyword.value
    }, controller.signal)
    // The page can fall off the end if accounts changed since the last load
    if (!result.items.length && result.totalPages > 0 && currentPage.value > result.totalPages) {
      currentPage.value = result.totalPages
      return
    }
    users.value = result.items
    totalElements.value = result.totalElements
    totalPages.value = Math.max(result.totalPages, 1)
  } catch (error) {
    if (!controller.signal.aborted) loadError.value = error.message || 'Unable to load accounts.'
  } finally {
    if (!controller.signal.aborted && loadController === controller) isLoading.value = false
  }
}

// A new filter or search starts again from page 1; changing currentPage triggers the load
function reloadFromFirstPage() {
  if (currentPage.value === 1) loadAccounts()
  else currentPage.value = 1
}

let searchTimer = null
watch(searchKeyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(reloadFromFirstPage, 300)
})
watch([filterRole, filterStatus], reloadFromFirstPage)
watch(currentPage, loadAccounts)

onMounted(loadAccounts)
onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  loadController?.abort()
})

const hasActiveFilters = computed(() => {
  return searchKeyword.value.trim() !== '' || filterRole.value !== '' || filterStatus.value !== ''
})

function resetFilters() {
  searchKeyword.value = ''
  filterRole.value = ''
  filterStatus.value = ''
}

const paginationStart = computed(() => totalElements.value === 0 ? 0 : (currentPage.value - 1) * PAGE_SIZE + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * PAGE_SIZE, totalElements.value))

const busy = ref(false)
const deactivateTarget = ref(null)
const deactivateReason = ref('')

function openDeactivate(user) {
  deactivateTarget.value = user
  deactivateReason.value = ''
}

function closeDeactivate() {
  deactivateTarget.value = null
}

async function confirmDeactivate() {
  const reason = deactivateReason.value.trim()
  if (busy.value || !reason) return
  busy.value = true
  try {
    await deactivateUser(deactivateTarget.value.userId, reason)
    closeDeactivate()
    loadAccounts()
  } catch (error) {
    window.alert(error.message)
  } finally {
    busy.value = false
  }
}

async function handleReactivate(user) {
  if (busy.value || !window.confirm(`Restore account access for ${user.name} (#${user.userId})?`)) return
  busy.value = true
  try {
    await reactivateUser(user.userId)
    loadAccounts()
  } catch (error) {
    window.alert(error.message)
  } finally {
    busy.value = false
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

.btn-deactivate:disabled, .btn-reactivate:disabled { opacity: 0.45; cursor: not-allowed; }

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
.modal-head { margin-bottom: 14px; }
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
.field textarea {
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
.btn-outline:disabled, .btn-danger-confirm:disabled { opacity: 0.45; cursor: not-allowed; }

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