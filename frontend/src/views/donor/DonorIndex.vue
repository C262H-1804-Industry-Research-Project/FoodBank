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
        <span
          v-if="item.key === 'requests' && pendingCount > 0"
          class="sb-badge"
          :aria-label="`${pendingCount} pending ${pendingCount === 1 ? 'request' : 'requests'}`"
        >{{ pendingCount > 99 ? '99+' : pendingCount }}</span>
      </div>
      <div class="sb-logout" @click="handleLogout">
        <span class="dot-ic"></span>Log out
      </div>
    </aside>

    <div class="app-main">
      <header class="app-topbar">
        <div class="topbar-title">{{ currentPanelTitle }}</div>
        <div class="topbar-right">
          <button
            type="button"
            class="role-switch-button"
            :disabled="switchingRole"
            @click="handleSwitchRole"
          >
            {{ switchingRole ? 'Switching…' : 'Switch to Recipient' }}
          </button>
          <span class="role-badge">Donor · {{ session?.user?.name || 'Donor' }}</span>
          <div class="avatar">GL</div>
        </div>
      </header>

      <main class="app-content">
        <!-- 1. My Listings Panel -->
        <MyListings 
          v-if="currentPanel === 'listings'" 
          :donor-id="donorId"
          :donor-address="donorAddress"
          @create-new="currentPanel = 'newlisting'"
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
          :loading="requestLoading"
          :error="requestError"
          :busy="requestBusy"
          @reload="loadRequestList"
          @approve-request="handleApproveRequest"
          @decline-request="handleDeclineRequest"
          @confirm-handover="handleConfirmHandover"
        />

        <!-- 4. Donation History Panel -->
        <DonationHistory 
          v-else-if="currentPanel === 'history'"
          :history-list="historyData"
          :loading="historyLoading"
          :error="historyError"
          @reload="loadHistory"
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

    <AddRoleModal
      v-if="showAddRoleModal"
      target-role="recipient"
      :pending="addingRole"
      :error="addRoleError"
      @submit="handleCreateRole"
      @cancel="closeAddRoleModal"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  clearSession,
  getDonorDonationHistory,
  getDonorSettings,
  getStoredSession,
  logout,
} from '../../api/client'
import { fetchDonorRequests, changePickupRequest, donorRequestRow } from '../../api/pickupRequests'
import { toDonorAddress } from '../../api/donorListingAdapter'

import MyListings from './MyListings.vue'
import NewListing from './NewListing.vue'
import ApproveRequests from './ApproveRequests.vue'
import DonationHistory from './DonationHistory.vue'
import DonorReports from './DonorReports.vue'
import ReportList from './ReportList.vue'
import AddRoleModal from '../../components/AddRoleModal.vue'
import { useRoleSwitch } from '../../composables/useRoleSwitch'

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

const {
  switchingRole,
  showAddRoleModal,
  addingRole,
  addRoleError,
  handleSwitchRole,
  handleCreateRole,
  closeAddRoleModal,
} = useRoleSwitch({
  currentRole: 'donor',
  targetRole: 'recipient',
  router,
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
const listingsController = new AbortController()

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
  loadDonorAddress()
  // Loaded up front so the sidebar can show the pending count before the panel is opened
  loadRequestList()
})
onBeforeUnmount(() => listingsController.abort())

// My listings loads page 1 (newest first) when it opens, so the new listing is at the top
function handleAddListing() {
  currentPanel.value = 'listings'
}

// This panel uses the donor's persisted requests.
const requestsData = ref([])
const requestLoading = ref(false)
const pendingCount = computed(() => requestsData.value.filter(row => row.status === 'Pending').length)
const requestError = ref('')
const requestBusy = ref(false)

async function loadRequestList() {
  if (requestLoading.value) return
  requestLoading.value = true
  requestError.value = ''
  try {
    const rows = await fetchDonorRequests(donorId, listingsController.signal)
    if (!listingsController.signal.aborted) requestsData.value = rows.map(donorRequestRow)
  } catch (error) {
    if (!listingsController.signal.aborted) requestError.value = error.message
  } finally {
    requestLoading.value = false
  }
}
watch(currentPanel, panel => { if (panel === 'requests') loadRequestList() })

async function processPickup(id, action) {
  if (requestBusy.value || requestLoading.value) return
  const expected = action === 'complete' ? ['Approved', 'Expired'] : ['Pending']
  const index = requestsData.value.findIndex(row => row.id === id && expected.includes(row.status))
  if (index < 0) return
  requestBusy.value = true
  try {
    const result = await changePickupRequest(donorId, id, action)
    requestsData.value[index] = donorRequestRow(result)
    await loadRequestList()
  } catch (error) {
    window.alert(error.message)
  } finally {
    requestBusy.value = false
  }
}
function handleApproveRequest(id) { return processPickup(id, 'approve') }
function handleDeclineRequest(id) { return processPickup(id, 'reject') }
function handleConfirmHandover(id) { return processPickup(id, 'complete') }

// Fetch persisted completed collections when the history panel opens.
const historyData = ref([])
const historyLoading = ref(false)
const historyError = ref('')

async function loadHistory() {
  if (historyLoading.value) return
  historyLoading.value = true
  historyError.value = ''
  try {
    const rows = await getDonorDonationHistory(donorId, listingsController.signal)
    if (!listingsController.signal.aborted) {
      historyData.value = rows.map(item => ({
        id: item.historyId,
        status: item.status === 'FAILED' ? 'Failed'
          : item.status === 'COMPLETED' ? 'Completed' : 'Unknown',
        foodName: item.foodName || 'Unavailable listing',
        category: item.category || 'Uncategorised',
        quantity: item.quantityCollected ?? 'Unavailable',
        collectedBy: item.recipientName || 'Unavailable recipient',
        completedDate: item.collectedDatetime?.replace('T', ' ') || 'Unavailable',
      }))
    }
  } catch (error) {
    if (!listingsController.signal.aborted) historyError.value = error.message
  } finally {
    historyLoading.value = false
  }
}
watch(currentPanel, panel => { if (panel === 'history') loadHistory() })

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
.sb-badge { margin-left: auto; min-width: 20px; height: 20px; padding: 0 6px; border-radius: 10px; background: #E29B9B; color: #2A1414; font-size: 11px; font-weight: 800; line-height: 20px; text-align: center; }
.sb-item.active .sb-badge { background: var(--primary-dark); color: #fff; }
.sb-logout { margin-top: auto; padding: 10px 12px; border-radius: 8px; font-size: 13.5px; font-weight: 600; color: #E29B9B; cursor: pointer; display: flex; align-items: center; gap: 10px; }
.sb-logout .dot-ic { width: 6px; height: 6px; border-radius: 50%; background: #E29B9B; flex: 0 0 6px; }
.sb-logout:hover { background: rgba(255,255,255,.06); }

.app-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.app-topbar { height: 66px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; padding: 0 28px; background: var(--surface); }
.topbar-title { font-family: var(--font-serif); font-size: 19px; font-weight: 600; }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.role-switch-button { padding: 8px 14px; border: 1px solid var(--primary); border-radius: 8px; background: #fff; color: var(--primary); font-size: 13px; font-weight: 700; cursor: pointer; }
.role-switch-button:hover { background: var(--primary-tint); }
.role-switch-button:disabled { cursor: not-allowed; opacity: .6; }
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
