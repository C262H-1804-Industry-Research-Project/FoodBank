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
          :donor-id="donorId"
          :donor-address="donorAddress"
          :listings="listingsData"
          @create-new="currentPanel = 'newlisting'"
          @update-listing="handleUpdateListing"
          @delete-listing="handleDeleteListing"
        />

        <!-- 2. New Listing Form Panel -->
        <NewListing
          v-else-if="currentPanel === 'newlisting'" 
          :donor-id="donorId"
          :donor-address="donorAddress"
          @add-listing="handleAddListing"
        />

        <!-- 3. Requests to Approve Panel -->
        <ApproveRequests 
          v-else-if="currentPanel === 'requests'"
          :requests-list="requestsData"
          @approve-request="handleApproveRequest"
          @decline-request="handleDeclineRequest"
        />

        <!-- 4. Donation History Panel -->
        <DonationHistory 
          v-else-if="currentPanel === 'history'"
          :history-list="historyData"
        />

        <!-- 5. Submit Report Panel -->
        <DonorReports 
          v-else-if="currentPanel === 'reports'"
          :donor-id="donorId"
          @report-submitted="handleReportSubmitted"
        />

        <!-- 6. Report List Panel -->
        <ReportList 
          v-else-if="currentPanel === 'reportlist'"
          :donor-id="donorId"
          @create-report="currentPanel = 'reports'"
        />

        <!-- 7. Other Panels: Placeholder -->
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearSession, getDonorListings, getDonorSettings, getStoredSession, logout } from '../../api/client'
import { toDonorAddress, toListingRow } from '../../api/donorListingAdapter'

import MyListings from './MyListings.vue'
import NewListing from './NewListing.vue'
import ApproveRequests from './ApproveRequests.vue'
import DonationHistory from './DonationHistory.vue'
import DonorReports from './DonorReports.vue'
import ReportList from './ReportList.vue'

const router = useRouter()

// Nav items including Report list and Reports
const navItems = [
  { key: 'listings', name: 'My listings' },
  { key: 'newlisting', name: 'New listing' },
  { key: 'requests', name: 'Requests to approve' },
  { key: 'history', name: 'Donation history' },
  { key: 'reportlist', name: 'Report list' },
  { key: 'reports', name: 'File a report' }
]

const currentPanel = ref('listings')
const currentPanelTitle = computed(() => {
  return navItems.find(item => item.key === currentPanel.value)?.name || ''
})

async function handleLogout() {
  try {
    await logout()
  } catch {
    // Session fallback
  } finally {
    clearSession()
    router.push('/auth')
  }
}

const session = getStoredSession()
const donorId = Number(session?.user?.donorId)
const listingsData = ref([])
const listingsController = new AbortController()

async function loadListings() {
  try {
    const listings = await getDonorListings(donorId, listingsController.signal)
    if (!listingsController.signal.aborted) {
      listingsData.value = listings.map(toListingRow)
    }
  } catch (error) {
    if (!listingsController.signal.aborted) {
      console.error('Unable to load donor listings.', error)
    }
  }
}

// The donor's own address, used by "same as my organisation's address" on listings
const donorAddress = ref(null)

async function loadDonorAddress() {
  try {
    const settings = await getDonorSettings(donorId, listingsController.signal)
    if (!listingsController.signal.aborted) donorAddress.value = toDonorAddress(settings)
  } catch (error) {
    if (!listingsController.signal.aborted) {
      console.error('Unable to load the donor address.', error)
    }
  }
}

onMounted(() => {
  loadListings()
  loadDonorAddress()
})
onBeforeUnmount(() => listingsController.abort())

function handleAddListing(newItem) {
  listingsData.value.unshift(newItem)
  currentPanel.value = 'listings'
  // Reload so fields the backend filled in (e.g. the donor's own pickup address) are shown
  loadListings()
}

function handleUpdateListing(updatedItem) {
  const index = listingsData.value.findIndex(item => item.id === updatedItem.id)
  if (index !== -1) {
    listingsData.value[index] = updatedItem
  }
}

function handleDeleteListing(id) {
  listingsData.value = listingsData.value.filter(item => item.id !== id)
}

// Requests to Approve state
const requestsData = ref([
  {
    id: 201,
    foodName: 'Sourdough loaves',
    totalStock: '12 units',
    recipientOrg: 'Community Kitchen East',
    contactName: 'Priya Nair',
    phone: '0412 345 678',
    quantity: '4 units',
    slot: 'Today, 5:30–6:00pm',
    notes: 'Serving dinner for 20 community members tonight.',
    status: 'Pending',
    statusClass: 'status-pending'
  },
  {
    id: 202,
    foodName: 'Pantry staples box',
    totalStock: '8 boxes',
    recipientOrg: 'St. Vincent de Paul Adelaide',
    contactName: 'Mark Higgins',
    phone: '0423 789 012',
    quantity: '2 boxes',
    slot: 'Sat, 10:00–10:30am',
    notes: 'Weekly food hamper distribution.',
    status: 'Pending',
    statusClass: 'status-pending'
  }
])

function handleApproveRequest(id) {
  const target = requestsData.value.find(r => r.id === id)
  if (target) {
    target.status = 'Approved'
    target.statusClass = 'status-approved'
  }
}

function handleDeclineRequest(id) {
  const target = requestsData.value.find(r => r.id === id)
  if (target) {
    target.status = 'Declined'
    target.statusClass = 'status-declined'
  }
}

// Donation history state
const historyData = ref([
  {
    id: 301,
    foodName: 'Artisan Bagels bundle (15 units)',
    category: 'Bakery',
    quantity: '15 units',
    collectedBy: 'Norwood Youth Refuge',
    completedDate: '2026-09-28 18:30'
  },
  {
    id: 302,
    foodName: 'Seasonal Fruit crates (4 boxes)',
    category: 'Produce',
    quantity: '4 boxes',
    collectedBy: 'Community Kitchen East',
    completedDate: '2026-09-25 11:00'
  },
  {
    id: 303,
    foodName: 'Unsweetened Oat Milk (20 cartons)',
    category: 'Dairy',
    quantity: '20 cartons',
    collectedBy: 'Salvation Army Centre',
    completedDate: '2026-09-21 16:45'
  },
  {
    id: 304,
    foodName: 'Canned Beans & Chickpeas (25 cans)',
    category: 'Pantry',
    quantity: '25 cans',
    collectedBy: 'St. Vincent de Paul Adelaide',
    completedDate: '2026-09-15 14:00'
  }
])

// ReportList reloads from the API when it mounts, so the new report shows up there
function handleReportSubmitted() {
  currentPanel.value = 'reportlist'
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

.placeholder-container { display: flex; justify-content: center; align-items: center; min-height: 400px; }
.placeholder-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--r-lg); padding: 40px; text-align: center; max-width: 480px; width: 100%; }
.placeholder-icon { font-size: 36px; margin-bottom: 12px; }
.placeholder-card h3 { font-size: 20px; color: var(--text); margin-bottom: 8px; }
.placeholder-card p { font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin-bottom: 24px; }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 8px 16px; border-radius: var(--r-sm); font-weight: 700; font-size: 13px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
.btn-sm { padding: 6px 14px; font-size: 12.5px; }
</style>
