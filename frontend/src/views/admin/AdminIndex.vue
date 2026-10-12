<template>
  <div class="app-shell">
    <!-- Left sidebar navigation -->
    <aside class="app-sidebar">
      <div class="sb-brand">Second Table</div>
      <div class="sb-section-label">Admin Console</div>
      <div
        v-for="item in navItems"
        :key="item.key"
        class="sb-item"
        :class="{ active: currentPanel === item.key }"
        @click="currentPanel = item.key"
      >
        <span class="dot-ic"></span>{{ item.name }}
      </div>
      
      <!-- Bottom logout action -->
      <div class="sb-logout" @click="handleLogout">
        <span class="dot-ic"></span>Log out
      </div>
    </aside>

    <!-- Main view container -->
    <div class="app-main">
      <header class="app-topbar">
        <div class="topbar-title">{{ currentPanelTitle }}</div>
        <div class="topbar-right">
          <span class="role-badge">Super Admin · SA Food Network</span>
          <div class="avatar">AD</div>
        </div>
      </header>

      <main class="app-content">
        <!-- 1. Food Listings Management Panel -->
        <ManageListings 
          v-if="currentPanel === 'listings'" 
          :listings="adminListings"
          :loading="listingsLoading" :error="listingsError" :busy="listingBusy" @reload="loadListings"
          @ban-listing="handleBanListing"
        />

        <!-- 2. Account Management Panel (with userId & Deactivate action) -->
        <AccountManagement
          v-else-if="currentPanel === 'users'"
          :users-list="adminUsers"
          :loading="usersLoading" :error="usersError" @reload="loadUsers"
          @deactivate-user="handleDeactivateUser"
          @reactivate-user="handleReactivateUser"
        />

        <!-- 3. Misuse Reports & Penalty Policy Panel -->
        <MisuseReports
          v-else-if="currentPanel === 'reports'"
        />

        <!-- 4. Other Panels: Placeholder -->
        <section v-else class="placeholder-container">
          <div class="placeholder-card">
            <div class="placeholder-icon">🚧</div>
            <h3>{{ currentPanelTitle }}</h3>
            <p>This module is currently under active development. Please check back later.</p>
            <button class="btn-outline btn-sm" @click="currentPanel = 'listings'">
              ← Back to Food listings
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { logout, clearSession } from '../../api/client'
import { fetchAdminListings, banFoodListing, fetchAdminAccounts } from '../../api/adminModeration'
import ManageListings from './ManageListings.vue'
import AccountManagement from './AccountManagement.vue'
import MisuseReports from './MisuseReports.vue'

const router = useRouter()

// Admin Navigation Menu
const navItems = [
  { key: 'listings', name: 'Food listings' },
  { key: 'users', name: 'User management' },
  { key: 'approvals', name: 'Pending approvals' },
  { key: 'reports', name: 'Reports & disputes' },
  { key: 'settings', name: 'System settings' }
]

const currentPanel = ref('listings')
const currentPanelTitle = computed(() => {
  return navItems.find(item => item.key === currentPanel.value)?.name || ''
})

async function handleLogout() {
  try {
    await logout()
  } catch (error) {
    window.alert(error.message)
  } finally {
    clearSession()
    router.push('/auth')
  }
}

// Global Listings for Admin Moderation
const adminListings = ref([])
const listingsLoading = ref(false)
const listingsError = ref('')
const listingBusy = ref(null)
let loadGeneration = 0
async function loadListings() {
  const generation = ++loadGeneration
  listingsLoading.value = true
  listingsError.value = ''
  try {
    const rows = await fetchAdminListings()
    if (generation === loadGeneration) adminListings.value = rows
  } catch (error) {
    if (generation === loadGeneration) listingsError.value = error.message
  } finally {
    if (generation === loadGeneration) listingsLoading.value = false
  }
}
watch(currentPanel, panel => { if (panel === 'listings') loadListings() }, { immediate: true })
async function handleBanListing(id) {
  if (listingBusy.value !== null) return
  listingBusy.value = id
  try {
    const response = await banFoodListing(id)
    if (response?.status !== 'BANNED') throw new Error('Invalid ban response')
    const row = adminListings.value.find(item => item.id === id)
    if (row) row.status = 'BANNED'
    await loadListings()
  } catch (error) { window.alert(error.message) }
  finally { listingBusy.value = null }
}

// Global Users Data for Account Management
const adminUsers = ref([])
const usersLoading = ref(false)
const usersError = ref('')
let usersGeneration = 0
async function loadUsers() {
  const generation = ++usersGeneration
  usersLoading.value = true
  usersError.value = ''
  try {
    const rows = await fetchAdminAccounts()
    if (generation === usersGeneration) adminUsers.value = rows
  } catch (error) {
    if (generation === usersGeneration) usersError.value = error.message
  } finally {
    if (generation === usersGeneration) usersLoading.value = false
  }
}
watch(currentPanel, panel => { if (panel === 'users') loadUsers() })

function handleDeactivateUser(userId) {
  const user = adminUsers.value.find(u => u.userId === userId)
  if (user) {
    user.status = 'Deactivated'
  }
}

function handleReactivateUser(userId) {
  const user = adminUsers.value.find(u => u.userId === userId)
  if (user) {
    user.status = 'Active'
  }
}
</script>

<style scoped>
.app-shell { display: flex; min-height: 100vh; }
.app-sidebar { flex: 0 0 232px; background: #1B2925; color: #D9E3DD; padding: 26px 16px; display: flex; flex-direction: column; gap: 2px; }
.app-sidebar .sb-brand { font-family: var(--font-serif); color: #fff; font-size: 19px; font-weight: 600; padding: 0 10px 14px; }
.sb-section-label { font-size: 10.5px; text-transform: uppercase; letter-spacing: .08em; color: #7FA091; padding: 10px 10px 8px; font-weight: 700; }
.sb-item { padding: 10px 12px; border-radius: 8px; font-size: 13.5px; font-weight: 600; color: #C4D3CC; cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all .12s; }
.sb-item .dot-ic { width: 6px; height: 6px; border-radius: 50%; background: #5B7A6C; flex: 0 0 6px; }
.sb-item:hover { background: rgba(255,255,255,.06); color: #fff; }
.sb-item.active { background: #EADFBF; color: var(--primary-dark); }
.sb-item.active .dot-ic { background: var(--primary-dark); }
.sb-logout { margin-top: auto; padding: 10px 12px; border-radius: 8px; font-size: 13.5px; font-weight: 600; color: #E29B9B; cursor: pointer; display: flex; align-items: center; gap: 10px; }
.sb-logout .dot-ic { width: 6px; height: 6px; border-radius: 50%; background: #E29B9B; flex: 0 0 6px; }
.sb-logout:hover { background: rgba(255,255,255,.06); }

.app-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.app-topbar { height: 66px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; padding: 0 28px; background: var(--surface); }
.topbar-title { font-family: var(--font-serif); font-size: 19px; font-weight: 600; }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.role-badge { font-size: 11.5px; font-weight: 700; background: var(--danger-tint); color: var(--danger); padding: 5px 11px; border-radius: 20px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--danger); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 13px; }
.app-content { flex: 1; padding: 28px; overflow-y: auto; }

/* Placeholder Styles */
.placeholder-container { display: flex; justify-content: center; align-items: center; min-height: 400px; }
.placeholder-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--r-lg); padding: 40px; text-align: center; max-width: 480px; width: 100%; }
.placeholder-icon { font-size: 36px; margin-bottom: 12px; }
.placeholder-card h3 { font-size: 20px; color: var(--text); margin-bottom: 8px; }
.placeholder-card p { font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin-bottom: 24px; }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 8px 16px; border-radius: var(--r-sm); font-weight: 700; font-size: 13px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
.btn-sm { padding: 6px 14px; font-size: 12.5px; }
</style>