<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>My listings</h2>
        <div class="sub">Everything you currently have available or pending</div>
      </div>
      <button class="btn-accent" @click="emit('create-new')">+ New listing</button>
    </div>

    <p v-if="actionError && !showEditModal" class="action-error" role="alert">{{ actionError }}</p>
    <p v-if="loadError" class="action-error" role="alert">{{ loadError }} <button class="btn-outline btn-sm" @click="loadListings">Retry</button></p>
    <p v-else-if="isLoading && listings.length === 0" role="status">Loading listings…</p>
    <table v-if="listings.length > 0">
      <thead>
        <tr>
          <th>Food item</th>
          <th>Quantity</th>
          <th>Collection rate</th>
          <th>Collect by</th>
          <th>Location</th>
          <th>Pickup slots</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in listings" :key="item.id" :class="{ 'row-expired': item.expired }">
          <td>{{ item.name }}</td>
          <td>
            <template v-if="item.available != null">{{ item.available }} / {{ item.quantity }} left</template>
            <template v-else>{{ item.quantity }}</template>
          </td>
          <td>{{ item.collectionRate || '0/0' }}</td>
          <td>
            <span :class="{ 'date-expired': item.expired }">{{ item.deadline }}</span>
            <div v-if="item.expired" class="expired-ago">{{ item.expiredLabel }}</div>
          </td>
          <td>{{ item.suburb }}, {{ item.stateRegion }}</td>
          <td>
            {{ item.slots }} <span class="text-soft">· {{ item.booked }} booked</span>
          </td>
          <td><span class="badge-status" :class="item.statusClass">{{ item.status }}</span></td>
          <td>
            <div class="action-buttons">
              <button class="btn-outline btn-sm" :disabled="busy" @click="openEditModal(item)">Edit</button>
              <button class="btn-danger btn-sm" :disabled="busy" @click="handleCancelListing(item)">Cancel</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="listings.length > 0" class="pagination-bar">
      <div class="pagination-info">
        Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ totalElements }}</strong> listings
      </div>
      <div class="pagination-controls">
        <button class="page-btn" :disabled="currentPage === 1 || isLoading" @click="currentPage--">‹ Prev</button>
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
        <button class="page-btn" :disabled="currentPage === totalPages || isLoading" @click="currentPage++">Next ›</button>
      </div>
    </div>

    <div v-if="!isLoading && !loadError && listings.length === 0" class="empty-note">
      No active or pending listings found. Click "+ New listing" to add one.
    </div>

    <!-- Edit & Publish Modal -->
    <div class="modal-backdrop" :class="{ show: showEditModal }">
      <div class="modal-box" v-if="editForm">
        <h3>Edit listing</h3>
        <div class="modal-sub">Update the details for {{ editForm.name }}</div>

        <div class="field edit-photo-field">
          <label class="field-label">Food photo</label>
          <input ref="photoInputRef" type="file" accept="image/png,image/jpeg" hidden @change="handlePhotoSelection">
          <div class="edit-photo-preview">
            <img v-if="selectedPhoto?.preview || editForm.photoUrl" :src="selectedPhoto?.preview || editForm.photoUrl" alt="Food photo preview">
            <span v-else class="edit-photo-placeholder">📷 No photo selected</span>
            <div class="edit-photo-actions">
              <button type="button" class="btn-outline" :disabled="busy" @click="photoInputRef?.click()">{{ editForm.photoUrl || selectedPhoto ? 'Replace photo' : 'Add photo' }}</button>
              <button v-if="editForm.photoUrl || selectedPhoto" type="button" class="upload-remove" :disabled="busy" @click="removePhoto">Remove photo</button>
              <span class="upload-hint">PNG or JPG, up to 5MB</span>
            </div>
          </div>
          <p v-if="photoError" class="action-error" role="alert">{{ photoError }}</p>
        </div>

        <div class="form-grid">
          <div class="field span-2">
            <label class="field-label">Food name</label>
            <input v-model="editForm.name" type="text" required>
          </div>

          <div class="field span-2 fields-row">
            <div class="field">
              <label class="field-label">Category</label>
              <select v-model="editForm.categoryId">
                <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
              </select>
            </div>

            <div class="field">
              <label class="field-label">Quantity</label>
              <input v-model="editForm.quantity" type="text" required>
            </div>

            <div class="field">
              <label class="field-label">Max quantity per recipient / day</label>
              <input v-model="editForm.maxQty" type="number" min="1" placeholder="e.g. 4 units">
            </div>
          </div>

          <div class="field">
            <label class="field-label">Expiry date</label>
            <input v-model="editForm.expiryDate" type="date">
          </div>

          <div class="field">
            <label class="field-label">Latest collection deadline <span class="text-soft">(optional)</span></label>
            <input v-model="editForm.deadline" type="date">
          </div>

          <!-- Pickup address: the donor's own address unless unticked -->
          <div v-if="donorAddress" class="field span-2">
            <label class="checkbox-row">
              <input v-model="editForm.useDonorAddress" type="checkbox">
              <div>
                <strong>Pickup address is the same as my organisation's address</strong>
                <div class="checkbox-desc">{{ formatAddress(donorAddress) }}</div>
              </div>
            </label>
          </div>

          <template v-if="!editForm.useDonorAddress">
            <div class="field span-2">
              <label class="field-label">Pickup street address</label>
              <AddressAutocomplete
                v-model="editForm.addressLine"
                placeholder="Start typing the street address"
                required
                @select="applyPickupAddress"
              />
            </div>

            <div class="field">
              <label class="field-label">Suburb / Area</label>
              <input v-model="editForm.suburb" type="text" required>
            </div>

            <div class="field">
              <label class="field-label">State / Region</label>
              <select v-model="editForm.stateRegion" required>
                <option value="" disabled>Select a state or territory</option>
                <option value="ACT">ACT</option>
                <option value="NSW">NSW</option>
                <option value="NT">NT</option>
                <option value="QLD">QLD</option>
                <option value="SA">SA</option>
                <option value="TAS">TAS</option>
                <option value="VIC">VIC</option>
                <option value="WA">WA</option>
              </select>
            </div>
          </template>

          <!-- Checkbox Options -->
          <div class="field span-2 checkbox-group">
            <label class="checkbox-row">
              <input type="checkbox" v-model="editForm.autoApproval">
              <div>
                <strong>Auto-approval for recipient requests</strong>
                <div class="checkbox-desc">Requests are automatically confirmed without manual review (default: ON).</div>
              </div>
            </label>

            <label class="checkbox-row">
              <input type="checkbox" v-model="editForm.requirePickupCode">
              <div>
                <strong>Require pickup code for recipient</strong>
                <div class="checkbox-desc">Recipient must provide a unique secure pickup code on collection (default: OFF).</div>
              </div>
            </label>
          </div>

          <div class="field span-2">
            <label class="field-label">Allergen & dietary tags</label>
            <div class="chip-group">
              <span
                v-for="tag in tags"
                :key="tag.id"
                class="chip"
                :class="{ active: editForm.tagIds.includes(tag.id) }"
                @click="toggleTag(tag)"
              >{{ tag.name }}</span>
            </div>
          </div>

          <!-- Storage instructions / notes -->
          <div class="field span-2">
            <label class="field-label">Storage instructions / additional notes <span class="text-soft">(displayed on food card)</span></label>
            <textarea v-model="editForm.notes" rows="3" placeholder="e.g. Keep refrigerated below 4°C; best collected same day"></textarea>
          </div>

          <!-- Pickup timeslot management -->
          <div class="field">
            <label class="field-label">Recipients allowed per slot</label>
            <div class="slot-stepper">
              <button type="button" class="stepper-btn" @click="adjustEditSlotCapacity(-1)">−</button>
              <input type="number" v-model.number="editForm.slotCapacity" min="1" max="20" class="stepper-input">
              <button type="button" class="stepper-btn" @click="adjustEditSlotCapacity(1)">+</button>
              <span class="stepper-desc">recipients / slot</span>
            </div>
          </div>

          <div class="field span-2 timeslot-section">
            <label class="field-label">Pickup timeslots</label>
            <div class="timeslot-form">
              <input v-model="slotDraft.date" type="date" :min="today">
              <input v-model="slotDraft.startTime" type="time">
              <span>to</span>
              <input v-model="slotDraft.endTime" type="time">
              <button type="button" class="btn-outline" @click="addTimeslot">Add timeslot</button>
            </div>
            <p v-if="timeslotError" class="timeslot-error" role="alert">{{ timeslotError }}</p>
            <div v-if="editForm.timeslots.length" class="timeslot-list">
              <div v-for="(slot, index) in editForm.timeslots" :key="`${slot.startTime}-${slot.endTime}`" class="timeslot-item">
                {{ formatTimeslot(slot) }}
                <button type="button" class="upload-remove" @click="removeTimeslot(index)">Remove</button>
              </div>
            </div>
            <div v-else class="timeslot-empty">No pickup timeslots added yet.</div>
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

        <p v-if="actionError" class="action-error" role="alert">{{ actionError }}</p>
        <div class="modal-close-row">
          <button class="btn-outline" :disabled="busy" @click="closeEditModal">Cancel</button>
          <button class="btn-primary" style="width:auto; padding:10px 22px;" :disabled="busy" @click="saveListing('LIVE')">
            {{ busy ? 'Please wait…' : 'Publish listing' }}
          </button>
          <button class="btn-outline" :disabled="busy" @click="saveListing('DRAFT')">
            Save as draft
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { getDonorListingsPage, getListingCatalogue, updateDonorListing, deleteDonorListing, uploadPhoto } from '../../api/client'
import { formatAddress, isSameAddress, toListingRow } from '../../api/donorListingAdapter'
import AddressAutocomplete from '../../components/AddressAutocomplete.vue'

const props = defineProps({
  donorId: { type: Number, required: true },
  donorAddress: { type: Object, default: null },
})

// Server-side pagination: the current page of this donor's listings, newest first
const PAGE_SIZE = 5
const listings = ref([])
const currentPage = ref(1)
const totalElements = ref(0)
const totalPages = ref(1)
const isLoading = ref(true)
const loadError = ref('')
let loadController = null

async function loadListings() {
  // Abort an older request so a slow response cannot overwrite newer results
  loadController?.abort()
  const controller = new AbortController()
  loadController = controller
  isLoading.value = true
  loadError.value = ''
  try {
    const result = await getDonorListingsPage(props.donorId, { page: currentPage.value - 1, size: PAGE_SIZE }, controller.signal)
    // The page can fall off the end, e.g. after cancelling the only listing on the last page
    if (!result.items.length && result.totalPages > 0 && currentPage.value > result.totalPages) {
      currentPage.value = result.totalPages
      return
    }
    listings.value = result.items.map(listing => toListingRow(listing))
    totalElements.value = result.totalElements
    totalPages.value = Math.max(result.totalPages, 1)
  } catch (error) {
    if (!controller.signal.aborted) loadError.value = error.message || 'Unable to load your listings.'
  } finally {
    if (!controller.signal.aborted && loadController === controller) isLoading.value = false
  }
}

watch(currentPage, loadListings)
onMounted(loadListings)
onBeforeUnmount(() => loadController?.abort())

const paginationStart = computed(() => totalElements.value === 0 ? 0 : (currentPage.value - 1) * PAGE_SIZE + 1)
const paginationEnd = computed(() => (currentPage.value - 1) * PAGE_SIZE + listings.value.length)

const emit = defineEmits(['create-new'])

const showEditModal = ref(false)
const editForm = ref(null)
const photoInputRef = ref(null)
const selectedPhoto = ref(null)
const photoError = ref('')
const busy = ref(false)
const actionError = ref('')
const categories = ref([])
const tags = ref([])
const today = new Date().toISOString().slice(0, 10)
const timeslotError = ref('')
const slotDraft = reactive({ date: '', startTime: '', endTime: '' })

async function openEditModal(item) {
  if (busy.value) return
  editForm.value = JSON.parse(JSON.stringify(item))
  selectedPhoto.value = null
  photoError.value = ''
  editForm.value.deadline = item.collectionDeadline || ''
  editForm.value.expiryDate = item.expiryDate || ''
  editForm.value.maxQty = item.maxDailyCapacity ?? ''
  editForm.value.tagIds = []
  editForm.value.timeslots = item.timeslots || []
  editForm.value.useDonorAddress = Boolean(props.donorAddress) && isSameAddress(item, props.donorAddress)
  timeslotError.value = ''
  slotDraft.date = ''
  slotDraft.startTime = ''
  slotDraft.endTime = ''
  actionError.value = ''
  showEditModal.value = true
  busy.value = true
  try {
    const catalogue = await getListingCatalogue()
    categories.value = catalogue.categories
    tags.value = catalogue.tags
    editForm.value.tagIds = tags.value
      .filter(tag => (item.tags || []).includes(tag.name))
      .map(tag => tag.id)
  } catch (error) {
    actionError.value = error.message
  } finally {
    busy.value = false
  }
}

function handlePhotoSelection(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  photoError.value = ''
  if (file.size > 5 * 1024 * 1024) {
    photoError.value = 'Choose an image smaller than 5MB.'
    return
  }
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    photoError.value = 'Choose a PNG or JPG image.'
    return
  }
  const reader = new FileReader()
  reader.onload = () => { selectedPhoto.value = { file, preview: reader.result } }
  reader.readAsDataURL(file)
}

function removePhoto() {
  selectedPhoto.value = null
  if (editForm.value) editForm.value.photoUrl = ''
  photoError.value = ''
}

function applyPickupAddress(address) {
  if (!editForm.value) return
  if (address.suburb) editForm.value.suburb = address.suburb
  if (address.stateRegion) editForm.value.stateRegion = address.stateRegion
}

function adjustEditSlotCapacity(delta) {
  if (!editForm.value) return
  const newVal = editForm.value.slotCapacity + delta
  if (newVal >= 1 && newVal <= 20) {
    editForm.value.slotCapacity = newVal
  }
}

function formatTimeslot(slot) {
  const [date = ''] = (slot.startTime || '').split('T')
  const startTime = (slot.startTime || '').slice(11, 16)
  const endTime = (slot.endTime || '').slice(11, 16)
  return `${date}: ${startTime} - ${endTime} (${slot.maxRecipients} recipients)`
}

function toggleTag(tag) {
  const index = editForm.value.tagIds.indexOf(tag.id)
  if (index === -1) editForm.value.tagIds.push(tag.id)
  else editForm.value.tagIds.splice(index, 1)
}

function addTimeslot() {
  timeslotError.value = ''
  if (!slotDraft.date || !slotDraft.startTime || !slotDraft.endTime) {
    timeslotError.value = 'Enter a date, start time, and end time.'
    return
  }
  if (slotDraft.endTime <= slotDraft.startTime) {
    timeslotError.value = 'The end time must be after the start time.'
    return
  }
  const startTime = `${slotDraft.date}T${slotDraft.startTime}`
  const endTime = `${slotDraft.date}T${slotDraft.endTime}`
  if (editForm.value.timeslots.some(slot => slot.startTime === startTime && slot.endTime === endTime)) {
    timeslotError.value = 'This pickup timeslot has already been added.'
    return
  }
  editForm.value.timeslots.push({ startTime, endTime, maxRecipients: editForm.value.slotCapacity })
  slotDraft.date = ''
  slotDraft.startTime = ''
  slotDraft.endTime = ''
}

function removeTimeslot(index) {
  editForm.value.timeslots.splice(index, 1)
}

function closeEditModal() {
  if (busy.value) return
  actionError.value = ''
  selectedPhoto.value = null
  photoError.value = ''
  showEditModal.value = false
  editForm.value = null
}

async function saveListing(status) {
  if (!editForm.value || busy.value) return
  actionError.value = ''
  if (photoError.value) return
  busy.value = true
  try {
    const form = editForm.value
    const quantity = Number(form.quantity)
    const requiredFields = form.useDonorAddress ? [form.name] : [form.name, form.addressLine, form.suburb, form.stateRegion]
    if (!requiredFields.every(value => value?.trim())) {
      throw new Error('Complete all required fields before publishing.')
    }
    if (!Number.isFinite(quantity) || quantity <= 0) throw new Error('Quantity must be greater than zero.')
    if (!Number.isInteger(form.slotCapacity) || form.slotCapacity < 1 || form.slotCapacity > 20) {
      throw new Error('Recipients per slot must be between 1 and 20.')
    }
    if (form.expiryDate && !/^\d{4}-\d{2}-\d{2}$/.test(form.expiryDate)) throw new Error('Enter the expiry date as YYYY-MM-DD, or leave it blank.')
    if (form.deadline && !/^\d{4}-\d{2}-\d{2}$/.test(form.deadline)) throw new Error('Enter the collection deadline as YYYY-MM-DD, or leave it blank.')
    if (form.expiryDate && form.deadline && form.deadline > form.expiryDate) throw new Error('The collection deadline cannot be after the expiry date.')
    const maxDailyCapacity = form.maxQty === '' || form.maxQty == null ? null : Number(form.maxQty)
    if (maxDailyCapacity !== null && (!Number.isInteger(maxDailyCapacity) || maxDailyCapacity < 1)) {
      throw new Error('Max quantity per recipient / day must be a whole number greater than zero.')
    }
    if (!form.categoryId) throw new Error('Select a category before publishing.')

    const photoUrl = selectedPhoto.value
      ? await uploadPhoto(selectedPhoto.value.file)
      : (form.photoUrl || null)
    const saved = await updateDonorListing(props.donorId, form.id, {
      name: form.name.trim(), description: form.notes, categoryId: form.categoryId,
      quantity, expiryDate: form.expiryDate || null, collectionDeadline: form.deadline || null,
      // Blank address fields tell the backend to use the donor's own address
      addressLine: form.useDonorAddress ? null : form.addressLine.trim(),
      suburb: form.useDonorAddress ? null : form.suburb.trim(),
      region: form.useDonorAddress ? null : form.stateRegion.trim(),
      maxDailyCapacity, tagIds: form.tagIds,
      timeslots: form.timeslots.map(({ startTime, endTime }) => ({ startTime, endTime, maxRecipients: form.slotCapacity })),
      slotCapacity: form.slotCapacity, isAutoApproved: form.autoApproval,
      usePickupCode: form.requirePickupCode, hideContact: form.hideContact, photoUrl, status,
    })
    // Editing keeps the listing's place in the newest-first order, so it can be updated in place
    const updated = toListingRow({ ...saved, photoUrl: saved.photoUrl || photoUrl })
    listings.value = listings.value.map(item => item.id === updated.id ? updated : item)
    showEditModal.value = false
    editForm.value = null
    selectedPhoto.value = null
  } catch (error) {
    actionError.value = error.message || 'Unable to update the listing.'
  } finally {
    busy.value = false
  }
}

async function handleCancelListing(item) {
  if (busy.value) return
  if (!window.confirm(`Are you sure you want to cancel and delete "${item.name}"?`)) return
  actionError.value = ''
  busy.value = true
  try {
    await deleteDonorListing(props.donorId, item.id)
    // Reload so the next listing moves up onto this page
    await loadListings()
  } catch (error) {
    actionError.value = error.message || 'Unable to cancel the listing.'
  } finally {
    busy.value = false
  }
}

</script>

<style scoped>
.action-error { color: var(--danger); margin: 12px 0; }
.edit-photo-field { margin: 18px 0; }
.edit-photo-preview { display: flex; align-items: center; gap: 14px; }
.edit-photo-preview img { width: 120px; height: 90px; object-fit: cover; border: 1px solid var(--border); border-radius: 8px; }
.edit-photo-placeholder { width: 120px; height: 90px; display: grid; place-items: center; background: var(--surface-2); border-radius: 8px; color: var(--text-soft); font-size: 12px; }
.edit-photo-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; }
.upload-hint { color: var(--text-soft); font-size: 11px; }
button:disabled { opacity: .6; cursor: not-allowed; }
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
/* Expired rows are only highlighted; Edit and Cancel keep working */
tbody tr.row-expired { background: #FDF5F4; }
tbody tr.row-expired:hover { background: #FAEDEC; }
tbody tr.row-expired td:first-child { box-shadow: inset 3px 0 0 var(--danger); }
.date-expired { color: var(--danger); font-weight: 700; }
.expired-ago { font-size: 11.5px; color: var(--danger); margin-top: 2px; }

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
.modal-box { background: var(--surface); border-radius: var(--r-lg); width: 760px; max-width: 90vw; padding: 26px; max-height: 85vh; overflow-y: auto; }
.modal-box h3 { font-size: 18px; margin-bottom: 4px; }
.modal-sub { font-size: 12.5px; color: var(--text-muted); margin-bottom: 18px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.span-2 { grid-column: 1 / -1; }
.field { margin-bottom: 4px; }
.field-label { font-size: 12px; font-weight: 700; color: var(--text); margin-bottom: 6px; display: block; }
.field input, .field :deep(.address-autocomplete input), .field select, .field textarea { width: 100%; padding: 9px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 13.5px; }
.field input:focus, .field :deep(.address-autocomplete input:focus), .field select:focus, .field textarea:focus { outline: 2px solid var(--primary); outline-offset: 1px; border-color: var(--primary); }
.text-soft { color: var(--text-soft); font-weight: 500; }
.fields-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }

.slot-stepper { display: flex; align-items: center; gap: 8px; }
.stepper-btn { width: 32px; height: 32px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface-2); font-weight: 700; font-size: 15px; cursor: pointer; }
.stepper-btn:hover { border-color: var(--primary); color: var(--primary-dark); }
.stepper-input { width: 56px !important; text-align: center; }
.stepper-desc { font-size: 12px; color: var(--text-muted); }

.checkbox-group { background: var(--surface-2); border-radius: var(--r-md); padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.checkbox-row { display: flex; gap: 10px; align-items: center; cursor: pointer; font-size: 12.5px; }
.checkbox-row input { width: auto !important; }
.checkbox-desc { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }

.chip-group { display: flex; gap: 6px; flex-wrap: wrap; }
.chip { padding: 6px 12px; border-radius: 20px; border: 1.3px solid var(--border); font-size: 12px; font-weight: 600; color: var(--text-muted); background: var(--surface); cursor: pointer; }
.chip.active { background: var(--primary-dark); border-color: var(--primary-dark); color: #fff; }

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

.timeslot-section { background: var(--surface-2); padding: 14px; border-radius: var(--r-md); }
.timeslot-form { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.timeslot-form input { width: auto; }
.timeslot-form .btn-outline { margin-left: 4px; }
.timeslot-list { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
.timeslot-item { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 9px 11px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--r-sm); font-size: 13px; }
.timeslot-empty { margin-top: 10px; color: var(--text-soft); font-size: 12px; }
.timeslot-error { margin: 8px 0 0; color: var(--danger); font-size: 12px; }
.upload-remove { font-size: 11.5px; color: var(--danger); font-weight: 700; background: none; border: none; padding: 0; text-decoration: underline; cursor: pointer; }

@media (max-width: 680px) {
  .form-grid, .fields-row { grid-template-columns: 1fr; }
}
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
</style>
