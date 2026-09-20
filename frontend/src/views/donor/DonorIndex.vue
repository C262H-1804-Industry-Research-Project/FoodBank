<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="sb-brand">Second Table</div>
      <div class="sb-section-label">Donor</div>
      <div
        v-for="item in navItems"
        :key="item.key"
        class="sb-item"
        :class="{ active: currentPanel === item.key }"
        @click="currentPanel = item.key"
      >
        <span class="dot-ic"></span>{{ item.name }}
      </div>
      <div class="sb-logout" @click="handleLogout">
        <span class="dot-ic"></span>Log out
      </div>
    </aside>

    <div class="app-main">
      <header class="app-topbar">
        <div class="topbar-title">{{ currentPanelTitle }}</div>
        <div class="topbar-right">
          <span class="role-badge">Donor · Green Leaf Café</span>
          <div class="avatar">GL</div>
        </div>
      </header>

      <main class="app-content">
        <!-- 1. My Listings Panel -->
        <MyListings 
          v-if="currentPanel === 'listings'" 
          :listings="listingsData"
          @create-new="currentPanel = 'newlisting'"
          @update-listing="handleUpdateListing"
          @delete-listing="handleDeleteListing"
        />

        <!-- 2. New Listing Form Panel -->
        <NewListing 
          v-else-if="currentPanel === 'newlisting'" 
          @add-listing="handleAddListing"
        />

        <!-- 3. Other Panels: Placeholder -->
        <section v-else class="placeholder-container">
          <div class="placeholder-card">
            <div class="placeholder-icon">🚧</div>
            <h3>{{ currentPanelTitle }}</h3>
            <p>This module is currently under active development. Please check back later.</p>
            <button class="btn-outline btn-sm" @click="currentPanel = 'listings'">
              ← Back to My listings
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import MyListings from './MyListings.vue'
import NewListing from './NewListing.vue'

const router = useRouter()

const navItems = [
  { key: 'listings', name: 'My listings' },
  { key: 'newlisting', name: 'New listing' },
  { key: 'requests', name: 'Requests to approve' },
  { key: 'history', name: 'Donation history' },
  { key: 'settings', name: 'Settings & reports' }
]

const currentPanel = ref('listings')
const currentPanelTitle = computed(() => {
  return navItems.find(item => item.key === currentPanel.value)?.name || ''
})

function handleLogout() {
  router.push('/auth')
}

// Global donor listings state managed purely on client-side
const listingsData = ref([
  {
    id: 1,
    name: 'Sourdough loaves',
    category: 'Bakery',
    quantity: '12',
    deadline: 'Today, 8:00pm',
    slots: '5:30–6:00pm',
    booked: '2/6',
    status: 'Live',
    statusClass: 'status-live',
    hideContact: true,
    notes: 'Keep refrigerated'
  },
  {
    id: 2,
    name: 'Pantry staples box',
    category: 'Pantry',
    quantity: '8',
    deadline: 'Sat, 6:00pm',
    slots: '10:00–10:30am',
    booked: '1/6',
    status: 'Live',
    statusClass: 'status-live',
    hideContact: true,
    notes: 'Long shelf life'
  },
  {
    id: 3,
    name: 'Dairy bundle',
    category: 'Dairy',
    quantity: '10',
    deadline: '—',
    slots: 'Not yet published',
    booked: '0/6',
    status: 'Draft',
    statusClass: 'status-draft',
    hideContact: false,
    notes: ''
  }
])

// Add new listing to array and return to listings table
function handleAddListing(newItem) {
  listingsData.value.unshift(newItem)
  currentPanel.value = 'listings'
}

// Update existing listing item in array
function handleUpdateListing(updatedItem) {
  const index = listingsData.value.findIndex(item => item.id === updatedItem.id)
  if (index !== -1) {
    listingsData.value[index] = updatedItem
  }
}

// Delete / cancel listing by id
function handleDeleteListing(id) {
  listingsData.value = listingsData.value.filter(item => item.id !== id)
}
</script>

<style scoped>
.app-shell { display: flex; min-height: 100vh; }
.app-sidebar { flex: 0 0 232px; background: var(--sidebar); color: #D9E3DD; padding: 26px 16px; display: flex; flex-direction: column; gap: 2px; }
.app-sidebar .sb-brand { font-family: var(--font-serif); color: #fff; font-size: 19px; font-weight: 600; padding: 0 10px 22px; }
.sb-section-label { font-size: 10.5px; text-transform: uppercase; letter-spacing: .08em; color: #7FA091; padding: 14px 10px 6px; }
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
.role-badge { font-size: 11.5px; font-weight: 700; background: var(--primary-tint); color: var(--primary-dark); padding: 5px 11px; border-radius: 20px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 13px; }
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