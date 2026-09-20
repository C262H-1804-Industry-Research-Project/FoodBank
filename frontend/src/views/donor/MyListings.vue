<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>My listings</h2>
        <div class="sub">Everything you currently have available or pending</div>
      </div>
      <button class="btn-accent" @click="emit('create-new')">+ New listing</button>
    </div>

    <table>
      <thead>
        <tr>
          <th>Food item</th>
          <th>Quantity</th>
          <th>Collect by</th>
          <th>Location</th>
          <th>Pickup slots</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in listings" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.quantity }}</td>
          <td>{{ item.deadline }}</td>
          <td>{{ item.suburb }}, {{ item.stateRegion }}</td>
          <td>
            {{ item.slots }} <span class="text-soft">· {{ item.booked }} booked</span>
          </td>
          <td><span class="badge-status" :class="item.statusClass">{{ item.status }}</span></td>
          <td>
            <div class="action-buttons">
              <button class="btn-outline btn-sm" @click="openEditModal(item)">Edit</button>
              <button class="btn-danger btn-sm" @click="handleCancelListing(item)">Cancel</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="listings.length === 0" class="empty-note">
      No active or pending listings found. Click "+ New listing" to add one.
    </div>

    <!-- Edit & Publish Modal -->
    <div class="modal-backdrop" :class="{ show: showEditModal }">
      <div class="modal-box" v-if="editForm">
        <h3>Edit & Publish listing</h3>
        <div class="modal-sub">Update details and confirm publication for {{ editForm.name }}</div>

        <div class="form-grid">
          <div class="field span-2">
            <label class="field-label">Food name</label>
            <input v-model="editForm.name" type="text" required>
          </div>

          <div class="field">
            <label class="field-label">Category</label>
            <select v-model="editForm.category">
              <option value="Bakery">Bakery</option>
              <option value="Produce">Produce</option>
              <option value="Dairy">Dairy</option>
              <option value="Pantry">Pantry</option>
            </select>
          </div>

          <div class="field">
            <label class="field-label">Quantity</label>
            <input v-model="editForm.quantity" type="text" required>
          </div>

          <div class="field">
            <label class="field-label">Latest collection deadline</label>
            <input v-model="editForm.deadline" type="text">
          </div>

          <!-- Structured Address fields -->
          <div class="field span-2">
            <label class="field-label">Pickup street address</label>
            <input v-model="editForm.addressLine" type="text" required>
          </div>

          <div class="field">
            <label class="field-label">Suburb / Area</label>
            <input v-model="editForm.suburb" type="text" required>
          </div>

          <div class="field">
            <label class="field-label">State / Region</label>
            <input v-model="editForm.stateRegion" type="text" required>
          </div>

          <!-- Slot Capacity Stepper -->
          <div class="field span-2">
            <label class="field-label">Recipients allowed per 1-hour slot</label>
            <div class="slot-stepper">
              <button type="button" class="stepper-btn" @click="adjustEditSlotCapacity(-1)">−</button>
              <input type="number" v-model.number="editForm.slotCapacity" min="1" max="20" class="stepper-input">
              <button type="button" class="stepper-btn" @click="adjustEditSlotCapacity(1)">+</button>
              <span class="stepper-desc">recipients / slot</span>
            </div>
          </div>

          <!-- Checkbox Options -->
          <div class="field span-2 checkbox-group">
            <label class="checkbox-row">
              <input type="checkbox" v-model="editForm.autoApproval">
              <div><strong>Auto-approval for recipient requests</strong></div>
            </label>

            <label class="checkbox-row">
              <input type="checkbox" v-model="editForm.requirePickupCode">
              <div><strong>Require pickup code for recipient</strong></div>
            </label>
          </div>

          <!-- Storage instructions / notes -->
          <div class="field span-2">
            <label class="field-label">Storage instructions / additional notes</label>
            <textarea v-model="editForm.notes" rows="2" placeholder="e.g. Keep refrigerated; transport in cooler bag"></textarea>
          </div>

          <!-- Privacy toggle -->
          <div class="field span-2">
            <div class="toggle-row">
              <div>
                <div class="tr-label">Hide phone / email from recipients</div>
                <div class="tr-desc">Reduces spam and rude messages; recipients still reach you in-app</div>
              </div>
              <div class="toggle" :class="{ on: editForm.hideContact }" @click="editForm.hideContact = !editForm.hideContact"></div>
            </div>
          </div>
        </div>

        <div class="modal-close-row">
          <button class="btn-outline" @click="closeEditModal">Cancel</button>
          <button class="btn-primary" style="width:auto; padding:10px 22px;" @click="confirmAndPublish">
            Confirm & Publish
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  listings: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['create-new', 'update-listing', 'delete-listing'])

const showEditModal = ref(false)
const editForm = ref(null)

function openEditModal(item) {
  editForm.value = JSON.parse(JSON.stringify(item))
  if (!editForm.value.slotCapacity) editForm.value.slotCapacity = 4
  if (editForm.value.autoApproval === undefined) editForm.value.autoApproval = true
  if (editForm.value.requirePickupCode === undefined) editForm.value.requirePickupCode = false
  if (!editForm.value.suburb) editForm.value.suburb = 'Adelaide'
  if (!editForm.value.stateRegion) editForm.value.stateRegion = 'SA'
  if (!editForm.value.addressLine) editForm.value.addressLine = '14 Rundle Mall'
  showEditModal.value = true
}

function adjustEditSlotCapacity(delta) {
  if (!editForm.value) return
  const newVal = editForm.value.slotCapacity + delta
  if (newVal >= 1 && newVal <= 20) {
    editForm.value.slotCapacity = newVal
  }
}

function closeEditModal() {
  showEditModal.value = false
  editForm.value = null
}

function confirmAndPublish() {
  if (editForm.value) {
    editForm.value.status = 'Live'
    editForm.value.statusClass = 'status-live'
    editForm.value.slots = `5:30–6:30pm (${editForm.value.slotCapacity} slots/hr)`
    emit('update-listing', editForm.value)
    closeEditModal()
  }
}

function handleCancelListing(item) {
  const confirmed = window.confirm(`Are you sure you want to cancel and delete "${item.name}"?`)
  if (confirmed) {
    emit('delete-listing', item.id)
  }
}
</script>

<style scoped>
.content-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
.btn-accent { background: var(--accent); color: #fff; border: none; padding: 10px 18px; border-radius: var(--r-sm); font-weight: 700; font-size: 13.5px; cursor: pointer; }
.btn-accent:hover { background: #A9781F; }
table { width: 100%; border-collapse: collapse; background: var(--surface); border: 1px solid var(--border); border-radius: var(--r-md); overflow: hidden; }
thead th { text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; color: var(--text-soft); background: var(--surface-2); padding: 11px 16px; font-weight: 700; }
tbody td { padding: 13px 16px; font-size: 13px; border-top: 1px solid var(--border); color: var(--text); }
tbody tr:hover { background: #FBFAF6; }
.text-soft { color: var(--text-soft); }
.badge-status { font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 20px; }
.status-live { background: var(--success-tint); color: var(--success); }
.status-draft { background: var(--surface-2); color: var(--text-muted); }

.action-buttons { display: flex; gap: 8px; align-items: center; }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 6px 12px; border-radius: var(--r-sm); font-weight: 700; font-size: 12px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
.btn-danger { background: transparent; border: 1.5px solid var(--danger); color: var(--danger); padding: 6px 12px; border-radius: var(--r-sm); font-weight: 700; font-size: 12px; cursor: pointer; }
.btn-danger:hover { background: var(--danger-tint); }
.btn-sm { padding: 5px 10px; font-size: 12px; border-radius: 6px; }

.empty-note {
  font-size: 12.5px;
  color: var(--text-soft);
  padding: 36px 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 16px;
}

/* Modal styles */
.modal-backdrop { display: none; position: fixed; inset: 0; background: rgba(15,20,17,.45); z-index: 200; align-items: center; justify-content: center; }
.modal-backdrop.show { display: flex; }
.modal-box { background: var(--surface); border-radius: var(--r-lg); width: 560px; max-width: 90vw; padding: 26px; max-height: 85vh; overflow-y: auto; }
.modal-box h3 { font-size: 18px; margin-bottom: 4px; }
.modal-sub { font-size: 12.5px; color: var(--text-muted); margin-bottom: 18px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.span-2 { grid-column: 1 / -1; }
.field { margin-bottom: 4px; }
.field-label { font-size: 12px; font-weight: 700; color: var(--text); margin-bottom: 6px; display: block; }
.field input, .field select, .field textarea { width: 100%; padding: 9px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 13.5px; }
.field input:focus, .field select:focus, .field textarea:focus { outline: 2px solid var(--primary); outline-offset: 1px; border-color: var(--primary); }

.slot-stepper { display: flex; align-items: center; gap: 8px; }
.stepper-btn { width: 32px; height: 32px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface-2); font-weight: 700; font-size: 15px; cursor: pointer; }
.stepper-btn:hover { border-color: var(--primary); color: var(--primary-dark); }
.stepper-input { width: 56px !important; text-align: center; }
.stepper-desc { font-size: 12px; color: var(--text-muted); }

.checkbox-group { background: var(--surface-2); border-radius: var(--r-md); padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.checkbox-row { display: flex; gap: 10px; align-items: center; cursor: pointer; font-size: 12.5px; }
.checkbox-row input { width: auto !important; }

.toggle-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); margin: 6px 0; }
.toggle-row .tr-label { font-size: 13px; font-weight: 700; }
.toggle-row .tr-desc { font-size: 11px; color: var(--text-soft); margin-top: 2px; }
.toggle { position: relative; width: 38px; height: 20px; background: var(--border); border-radius: 20px; cursor: pointer; transition: .15s; }
.toggle::after { content: ''; position: absolute; width: 14px; height: 14px; background: #fff; border-radius: 50%; top: 3px; left: 3px; transition: .15s; }
.toggle.on { background: var(--primary); }
.toggle.on::after { left: 21px; }

.modal-close-row { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-primary { padding: 10px 20px; border: none; border-radius: var(--r-sm); background: var(--primary); color: #fff; font-weight: 700; font-size: 14px; cursor: pointer; }
.btn-primary:hover { background: var(--primary-dark); }
</style>