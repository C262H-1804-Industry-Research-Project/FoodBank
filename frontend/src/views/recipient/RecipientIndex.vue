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
          @cancel="handleCancelRequest"
        />

        <!-- 3. Collection History Panel -->
        <CollectionHistory 
          v-else-if="currentPanel === 'history'"
          :history-list="myHistoryList"
        />

        <!-- 4. Report an Issue Panel -->
        <ReportIssue 
          v-else-if="currentPanel === 'report'"
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
          <select v-model="modalForm.slot">
            <option v-for="slot in modalItem.timeslots" :key="slot.startTime">
              {{ formatPickupSlot(slot) }} · {{ slot.remainingSlot }} spots left
            </option>
          </select>
        </div>
        
        <div class="field">
          <label class="field-label">Quantity <span style="font-weight:500;color:var(--text-soft);">(max {{ modalItem.capacity }} per recipient)</span></label>
          <input type="text" v-model="modalForm.qty" placeholder="e.g. 4">
        </div>
        
        <div class="field">
          <label class="field-label">Note <span style="font-weight:500;color:var(--text-soft);">(optional — e.g. request extra beyond the limit)</span></label>
          <textarea v-model="modalForm.note" rows="3" placeholder="Add context for the donor…"></textarea>
        </div>
        
        <div class="field-hint" style="margin-bottom:6px;">
          📍 The exact pickup address is shared in <strong>My requests</strong> once the donor approves you — for now you only see the general area.
        </div>
        
        <div class="modal-close-row">
          <button class="btn-outline" @click="closeModal">Cancel</button>
          <button class="btn-primary" style="width:auto; padding:10px 22px;" @click="confirmRequest">Submit request</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'

import BrowseDonations from './BrowseDonations.vue'
import MyRequests from './MyRequests.vue'
import CollectionHistory from './CollectionHistory.vue'
import ReportIssue from './ReportIssue.vue'

import { getAllListings, clearSession, logout } from '../../api/client'
import { formatPickupSlot, toListingCard } from '../../api/listingAdapter'

const router = useRouter()

const navItems = [
  { key: 'browse', name: 'Browse donations' },
  { key: 'myrequests', name: 'My requests' },
  { key: 'history', name: 'Collection history' },
  { key: 'report', name: 'Report an issue' }
]

const currentPanel = ref('browse')
const currentPanelTitle = computed(() => {
  return navItems.find(item => item.key === currentPanel.value)?.name || ''
})

async function handleLogout() {
  try {
    await logout()
  } catch {
    // Still remove a stale local session when server logout is unavailable.
  } finally {
    clearSession()
    router.push('/auth')
  }
}

// Global available donation listings
const availableListings = ref([
  {
    id: 1,
    emoji: '🥖',
    name: 'Sourdough loaves (12)',
    category: 'Bakery',
    timeLeft: '2h left',
    urgencyClass: 'urgent',
    addressLine: '14 Rundle Mall',
    suburb: 'Adelaide',
    postcode: '5000',
    stateRegion: 'SA',
    pickupWindow: 'Today, 5:30–6:00pm',
    timeType: 'today',
    tags: ['Contains gluten', 'Vegetarian'],
    donor: 'Green Leaf Café',
    notes: 'Freshly baked this morning; best consumed within 48h or kept refrigerated.'
  },
  {
    id: 2,
    emoji: '🥗',
    name: 'Mixed vegetable crates (5)',
    category: 'Produce',
    timeLeft: '18h left',
    urgencyClass: 'soon',
    addressLine: '82 The Parade',
    suburb: 'Norwood',
    postcode: '5067',
    stateRegion: 'SA',
    pickupWindow: 'Tomorrow, 9:00–9:30am',
    timeType: 'tomorrow',
    tags: ['Vegetarian', 'No pork'],
    donor: 'Norwood Fresh Markets',
    notes: 'Includes potatoes, carrots, and broccoli. Keep chilled in a cool environment.'
  },
  {
    id: 3,
    emoji: '🥫',
    name: 'Pantry staples box (8)',
    category: 'Pantry',
    timeLeft: '2 days left',
    urgencyClass: 'ok',
    addressLine: '10 King William St',
    suburb: 'Kent Town',
    postcode: '5067',
    stateRegion: 'SA',
    pickupWindow: 'Sat, 10:00–10:30am',
    timeType: 'weekend',
    tags: ['Halal', 'Nut-free', 'No pork'],
    donor: 'Kent Town IGA',
    notes: 'Non-perishable canned tomatoes and dried pasta; room temperature storage.'
  },
  {
    id: 4,
    emoji: '🥛',
    name: 'Whole milk cartons (15)',
    category: 'Dairy',
    timeLeft: '6h left',
    urgencyClass: 'urgent',
    addressLine: '45 Unley Rd',
    suburb: 'Unley',
    postcode: '5061',
    stateRegion: 'SA',
    pickupWindow: 'Today, 4:00–5:00pm',
    timeType: 'today',
    tags: ['Vegetarian', 'Nut-free'],
    donor: 'Unley Organic Co',
    notes: 'Must be transported using an esky or cooler bag.'
  },
  {
    id: 5,
    emoji: '🥐',
    name: 'Croissant bundles (20)',
    category: 'Bakery',
    timeLeft: '4h left',
    urgencyClass: 'urgent',
    addressLine: '120 O\'Connell St',
    suburb: 'North Adelaide',
    postcode: '5006',
    stateRegion: 'SA',
    pickupWindow: 'Today, 6:00–6:30pm',
    timeType: 'today',
    tags: ['Contains gluten', 'Vegetarian'],
    donor: 'Torrens Bakehouse',
    notes: 'Baked fresh daily. Ready to eat or reheat.'
  },
  {
    id: 6,
    emoji: '🍎',
    name: 'Organic Pink Lady Apples (30kg)',
    category: 'Produce',
    timeLeft: '3 days left',
    urgencyClass: 'ok',
    addressLine: '3 Mount Barker Rd',
    suburb: 'Stirling',
    postcode: '5152',
    stateRegion: 'SA',
    pickupWindow: 'Sat, 9:00–11:00am',
    timeType: 'weekend',
    tags: ['Halal', 'Nut-free', 'Vegetarian'],
    donor: 'Hills Orchard',
    notes: 'Store in cool dry conditions.'
  },
  {
    id: 7,
    emoji: '🥪',
    name: 'Gourmet Baguettes (10)',
    category: 'Bakery',
    timeLeft: '5h left',
    urgencyClass: 'soon',
    addressLine: '18 Jetty Rd',
    suburb: 'Glenelg',
    postcode: '5045',
    stateRegion: 'SA',
    pickupWindow: 'Today, 5:00–5:30pm',
    timeType: 'today',
    tags: ['Contains gluten', 'No pork'],
    donor: 'Bay Deli',
    notes: 'Contains dairy butter.'
  }
])
const browseRef = ref(null)

// My active and pending requests list
const myRequestsList = ref([
  {
    id: 101,
    foodItem: 'Sourdough loaves (4 units)',
    donor: 'Green Leaf Café',
    pickupSlot: 'Today, 5:30–6:00pm',
    pickupTime: 'Today, 5:30–6:00pm',
    address: '14 Rundle Mall, Adelaide',
    status: 'Reserved',
    statusClass: 'status-reserved',
    code: 'SEC-4891'
  },
  {
    id: 102,
    foodItem: 'Mixed vegetable crates (2 boxes)',
    donor: 'Norwood Fresh Markets',
    pickupSlot: 'Tomorrow, 9:00–9:30am',
    pickupTime: 'Tomorrow, 9:00–9:30am',
    address: null,
    status: 'Requested',
    statusClass: 'status-requested',
    code: null
  }
])

function handleCancelRequest(id) {
  const confirmed = window.confirm('Are you sure you want to cancel this request?')
  if (confirmed) {
    myRequestsList.value = myRequestsList.value.filter(r => r.id !== id)
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

const showModal = ref(false)
const listingsController = new AbortController()

onMounted(async () => {
  try {
    const rows = await getAllListings(listingsController.signal)
    if (!listingsController.signal.aborted) availableListings.value = rows.map(row => toListingCard(row))
  } catch (error) {
    if (!listingsController.signal.aborted) {
      window.alert(`${error.message} The original preview data is still displayed; it is not a successful API result.`)
    }
  }
})

onBeforeUnmount(() => listingsController.abort())

const modalItem = ref(null)
const modalForm = reactive({
  slot: 'Today, 5:30–6:00pm · 4 spots left',
  qty: '4',
  note: ''
})

function openRequestModal(item) {
  modalItem.value = item
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  modalItem.value = null
}

function confirmRequest() {
  if (modalItem.value) {
    const requestedItem = modalItem.value
    availableListings.value = availableListings.value.filter(item => item.id !== requestedItem.id)

    myRequestsList.value.unshift({
      id: Date.now(),
      foodItem: `${requestedItem.name} (${modalForm.qty} units)`,
      donor: requestedItem.donor,
      pickupSlot: modalForm.slot.split('·')[0].trim(),
      pickupTime: modalForm.slot.split('·')[0].trim(),
      address: null,
      status: 'Requested',
      statusClass: 'status-requested',
      code: null
    })

    alert(`Request for "${requestedItem.name}" submitted successfully!`)
    browseRef.value?.reload()
    alert(`Request for "${modalItem.value.name}" submitted successfully!`)
    closeModal()
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