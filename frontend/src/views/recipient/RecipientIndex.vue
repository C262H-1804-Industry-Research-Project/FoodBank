<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="sb-brand">Second Table</div>
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
          <span class="role-badge">Recipient · Community Kitchen East</span>
          <div class="avatar">CK</div>
        </div>
      </header>

      <main class="app-content">
        <!-- 1. Browse Donations Panel -->
        <BrowseDonations 
          v-if="currentPanel === 'browse'" 
          ref="browseRef"
          @request="openRequestModal" 
        />

        <!-- 2. My Requests Panel -->
        <MyRequests 
          v-else-if="currentPanel === 'myrequests'"
          :requests="myRequestsList"
          :loading="myRequestsLoading"
          :error="myRequestsError"
          :busy="cancellationPending"
          @reload="loadMyRequests"
          @cancel="handleCancelRequest"
        />

        <!-- 3. Collection History Panel -->
        <CollectionHistory 
          v-else-if="currentPanel === 'history'"
          :history-list="myHistoryList"
        />

        <!-- 4. Report List Panel (Matching Donor) -->
        <ReportList
          v-else-if="currentPanel === 'reports'"
          @create-report="currentPanel = 'filereport'"
        />

        <!-- 5. File a Report Panel (Matching Donor) -->
        <ReportIssue
          v-else-if="currentPanel === 'filereport'"
          @report-submitted="handleReportSubmitted"
        />

        <!-- Fallback Placeholder -->
        <section v-else class="placeholder-container">
          <div class="placeholder-card">
            <div class="placeholder-icon">🚧</div>
            <h3>{{ currentPanelTitle }}</h3>
            <p>This module is currently under active development. Please check back later.</p>
            <button class="btn-outline btn-sm" @click="currentPanel = 'browse'">
              ← Back to Browse donations
            </button>
          </div>
        </section>
      </main>
    </div>

    <!-- Request Modal with Description Box -->
    <div class="modal-backdrop" :class="{ show: showModal }">
      <div class="modal-box" v-if="modalItem">
        <h3>Request: {{ modalItem.name }}</h3>
        <div class="modal-sub">{{ modalItem.donor }}</div>
        
        <!-- Description block from listing notes -->
        <div class="field" v-if="modalItem.notes">
          <label class="field-label">Description</label>
          <div class="modal-description-box">
            {{ modalItem.notes }}
          </div>
        </div>

        <div class="field">
          <label class="field-label">Pickup timeslot</label>
          <select v-model="modalForm.slot" :disabled="submissionPending">
            <option value="" disabled>Select a pickup timeslot</option>
            <option v-for="slot in modalItem.timeslots" :key="slot.timeslotId" :value="slot.timeslotId" :disabled="!isPickupOpen(slot)">
              {{ formatPickupSlot(slot) }} · {{ slot.remainingSlot }} spots left
            </option>
          </select>
        </div>
        
        <div class="field">
          <label class="field-label">Quantity <span style="font-weight:500;color:var(--text-soft);">(max {{ modalItem.capacity }} per recipient)</span></label>
          <input type="number" min="0" step="any" v-model="modalForm.qty" :disabled="submissionPending" placeholder="e.g. 4">
        </div>
        
        <div class="field">
          <label class="field-label">Note <span style="font-weight:500;color:var(--text-soft);">(optional)</span></label>
          <textarea v-model="modalForm.note" :disabled="submissionPending" maxlength="500" rows="3" placeholder="Add context for the donor…"></textarea>
        </div>
        
        <div class="field-hint" style="margin-bottom:6px;">
          📍 The exact pickup address is shared in <strong>My requests</strong> once the donor approves you — for now you only see the general area.
        </div>
        
        <div class="modal-close-row">
          <button class="btn-outline" :disabled="submissionPending" @click="closeModal">Cancel</button>
          <button class="btn-primary" style="width:auto; padding:10px 22px;" :disabled="submissionPending" @click="confirmRequest">{{ submissionPending ? 'Submitting…' : 'Submit request' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

import BrowseDonations from './BrowseDonations.vue'
import MyRequests from './MyRequests.vue'
import CollectionHistory from './CollectionHistory.vue'
import ReportList from './ReportList.vue'
import ReportIssue from './ReportIssue.vue'

import { clearSession, logout } from '../../api/client'
import { fetchRecipientRequests, createPickupRequest, cancelPickupRequest, recipientRequestRow, isPickupOpen, canCancelPickup } from '../../api/pickupRequests'
import { formatPickupSlot } from '../../api/listingAdapter'

const router = useRouter()

// Side navigation matching Donor portal structure
const navItems = [
  { key: 'browse', name: 'Browse donations' },
  { key: 'myrequests', name: 'My requests' },
  { key: 'history', name: 'Collection history' },
  { key: 'reports', name: 'Report list' },
  { key: 'filereport', name: 'File a report' }
]

const currentPanel = ref('browse')
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

const browseRef = ref(null)

// Server data for the currently signed-in recipient.
const myRequestsList = ref([])
const myRequestsLoading = ref(false)
const myRequestsError = ref('')
const cancellationPending = ref(false)
const requestController = new AbortController()
onBeforeUnmount(() => requestController.abort())

async function loadMyRequests() {
  if (myRequestsLoading.value) return
  myRequestsLoading.value = true
  myRequestsError.value = ''
  try {
    const result = await fetchRecipientRequests(requestController.signal)
    if (!requestController.signal.aborted) myRequestsList.value = result.map(recipientRequestRow)
  } catch (error) {
    if (!requestController.signal.aborted) myRequestsError.value = error.message
  } finally {
    myRequestsLoading.value = false
  }
}
watch(currentPanel, panel => { if (panel === 'myrequests') loadMyRequests() })

async function handleCancelRequest(id) {
  if (cancellationPending.value || myRequestsLoading.value) return
  const row = myRequestsList.value.find(request => request.id === id)
  if (!row || !canCancelPickup(row)) {
    window.alert('This request can no longer be cancelled before pickup.')
    return
  }
  if (!window.confirm('Are you sure you want to cancel this request?')) return
  cancellationPending.value = true
  try {
    await cancelPickupRequest(id)
    Object.assign(row, { statusCode: 'CANCELLED', status: 'Cancelled', statusClass: 'status-expired', address: null, code: null })
    await loadMyRequests()
  } catch (error) {
    window.alert(error.message)
  } finally {
    cancellationPending.value = false
  }
}

// Historical collections dataset
const myHistoryList = ref([
  {
    id: 201,
    foodName: 'Sourdough loaves (8 units)',
    category: 'Bakery',
    quantity: '8 units',
    donor: 'Green Leaf Café',
    collectedDate: '2026-09-28 17:45'
  },
  {
    id: 202,
    foodName: 'Mixed vegetable crates (3 boxes)',
    category: 'Produce',
    quantity: '3 boxes',
    donor: 'Norwood Fresh Markets',
    collectedDate: '2026-09-22 10:15'
  },
  {
    id: 203,
    foodName: 'Pantry staples hamper',
    category: 'Pantry',
    quantity: '4 boxes',
    donor: 'Kent Town IGA',
    collectedDate: '2026-09-15 11:30'
  }
])

function handleReportSubmitted() {
  currentPanel.value = 'reports' // Jump to Report list, which reloads from the API
}

const showModal = ref(false)
const modalItem = ref(null)
const submissionPending = ref(false)
const modalForm = reactive({ slot: '', qty: '', note: '' })

function openRequestModal(item) {
  modalItem.value = item
  modalForm.slot = item.timeslots?.find(slot => isPickupOpen(slot))?.timeslotId ?? ''
  modalForm.qty = ''
  modalForm.note = ''
  showModal.value = true
}

function closeModal() {
  if (submissionPending.value) return
  showModal.value = false
  modalItem.value = null
}

async function confirmRequest() {
  if (submissionPending.value || !modalItem.value) return
  const selected = modalItem.value.timeslots?.find(slot => slot.timeslotId === modalForm.slot)
  const quantity = Number(modalForm.qty)
  if (!isPickupOpen(selected)) {
    window.alert('Choose a future pickup timeslot with available places.')
    return
  }
  if (!Number.isFinite(quantity) || quantity <= 0) {
    window.alert('Enter a quantity greater than zero.')
    return
  }
  if (Number.isFinite(modalItem.value.capacity) && quantity > modalItem.value.capacity) {
    window.alert(`The daily limit for this listing is ${modalItem.value.capacity}.`)
    return
  }
  const note = modalForm.note.trim()
  if (note.length > 500) {
    window.alert('The note must contain at most 500 characters.')
    return
  }
  submissionPending.value = true
  try {
    const saved = await createPickupRequest({ timeslotId: selected.timeslotId, quantityRequested: quantity, note: note || null })
    const row = recipientRequestRow(saved)
    myRequestsList.value = [row, ...myRequestsList.value.filter(existing => existing.id !== row.id)]
    window.alert('Pickup request submitted successfully.')
    showModal.value = false
    modalItem.value = null
    browseRef.value?.reload()
  } catch (error) {
    window.alert(error.message)
  } finally {
    submissionPending.value = false
  }
}
</script>

<style scoped>
.app-shell { display: flex; min-height: 100vh; }
.app-sidebar { flex: 0 0 232px; background: var(--sidebar); color: #D9E3DD; padding: 26px 16px; display: flex; flex-direction: column; gap: 2px; }
.app-sidebar .sb-brand { font-family: var(--font-serif); color: #fff; font-size: 19px; font-weight: 600; padding: 0 10px 22px; }
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

/* Modal and Description box styles */
.modal-backdrop { display: none; position: fixed; inset: 0; background: rgba(15,20,17,.45); z-index: 200; align-items: center; justify-content: center; }
.modal-backdrop.show { display: flex; }
.modal-box { background: var(--surface); border-radius: var(--r-lg); width: 440px; max-width: 90vw; padding: 26px; max-height: 85vh; overflow-y: auto; }
.modal-box h3 { font-size: 18px; margin-bottom: 4px; }
.modal-sub { font-size: 12.5px; color: var(--text-muted); margin-bottom: 16px; }

.modal-description-box {
  background: var(--surface-2);
  border-radius: var(--r-sm);
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.45;
}

.modal-close-row { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.field { margin-bottom: 16px; }
.field-label { font-size: 12.5px; font-weight: 700; color: var(--text); margin-bottom: 6px; display: block; }
.field input, .field select, .field textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 14px; }
.field-hint { font-size: 11.5px; color: var(--text-soft); margin-top: 4px; }
.btn-primary { padding: 12px; border: none; border-radius: var(--r-sm); background: var(--primary); color: #fff; font-weight: 700; font-size: 14.5px; cursor: pointer; }
.btn-primary:hover { background: var(--primary-dark); }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 8px 16px; border-radius: var(--r-sm); font-weight: 700; font-size: 13px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
.btn-sm { padding: 6px 14px; font-size: 12.5px; }
</style>