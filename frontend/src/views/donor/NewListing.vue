<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>New listing</h2>
        <div class="sub">Publish surplus food for recipients to reserve. Optional fields are marked with (*).</div>
      </div>
    </div>

    <div class="card">
      <form @submit.prevent="submitListing">
        <div class="form-grid">
          <!-- Food Photo upload (Optional) -->
          <div class="field span-2">
            <label class="field-label">* Food photo <span class="text-soft">(optional)</span></label>
            <div
              class="upload-box"
              :class="{ 'has-image': uploadedPhoto, 'drag-over': isDragOver }"
              @click="triggerFileInput"
              @dragover.prevent="isDragOver = true"
              @dragleave.prevent="isDragOver = false"
              @drop="handleFileDrop"
            >
              <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileUpload">
              <div v-if="!uploadedPhoto">
                <div class="upload-icon">📷</div>
                <div class="upload-text">Click to upload a photo, or drag and drop</div>
                <div class="upload-hint">PNG or JPG, up to 5MB</div>
              </div>
              <div v-else @click.stop>
                <div class="upload-preview-wrap">
                  <img :src="uploadedPhoto.url" alt="Food photo preview">
                  <div>
                    <div class="upload-file-name">{{ uploadedPhoto.name }}</div>
                    <button type="button" class="upload-remove" @click="removeUpload">Remove photo</button>
                  </div>
                </div>
              </div>
              <div v-if="photoError" class="field-error">{{ photoError }}</div>
            </div>
          </div>

          <div class="field span-2">
            <label class="field-label">Food name</label>
            <input v-model="form.name" type="text" placeholder="e.g. Sourdough loaves" required>
          </div>

          <div class="field span-2 fields-row">
            <div class="field">
              <label class="field-label">Category</label>
              <select v-model="form.category" :disabled="isLoadingCatalogue">
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>

            <div class="field">
              <label class="field-label">Quantity</label>
              <input v-model="form.quantity" type="text" placeholder="e.g. 12 units" required>
            </div>

            <div class="field">
              <label class="field-label">Max quantity per recipient / day</label>
              <input v-model="form.maxQty" type="number" min="1" placeholder="e.g. 4 units">
            </div>
          </div>




          <div class="field">
            <label class="field-label">Expiry date</label>
            <input v-model="form.expiryDate" type="date">
          </div>

          <div class="field">
            <label class="field-label">* Latest collection deadline <span class="text-soft">(optional)</span></label>
            <input v-model="form.deadline" type="date">
          </div>

          <!-- Structured 3-field Pickup Address -->
          <div class="field span-2">
            <label class="field-label">Pickup street address</label>
            <AddressAutocomplete
              v-model="form.addressLine"
              placeholder="Start typing the street address (e.g. 14 Rundle Mall)"
              required
              @select="applyPickupAddress"
            />
          </div>

          <div class="field">
            <label class="field-label">Suburb / Area <span class="req-tag">• Required</span></label>
            <input v-model="form.suburb" type="text" placeholder="e.g. Adelaide" required>
          </div>

          <div class="field">
            <label class="field-label">State / Region</label>
            <input v-model="form.stateRegion" type="text" placeholder="e.g. SA" required>
          </div>


          <div class="field">
            <label class="field-label">* Max quantity per recipient / day <span class="text-soft">(optional)</span></label>
            <input v-model="form.maxQty" type="text" placeholder="e.g. 4 units">
          </div>

          <!-- Checkbox Options: Auto-Approval & Require Pickup Code -->
          <div class="field span-2 checkbox-group">
            <label class="checkbox-row">
              <input type="checkbox" v-model="form.autoApproval">
              <div>
                <strong>Auto-approval for recipient requests</strong>
                <div class="checkbox-desc">Requests are automatically confirmed without manual review (default: ON).</div>
              </div>
            </label>

            <label class="checkbox-row">
              <input type="checkbox" v-model="form.requirePickupCode">
              <div>
                <strong>Require pickup code for recipient</strong>
                <div class="checkbox-desc">Recipient must provide a unique secure pickup code on collection (default: OFF).</div>
              </div>
            </label>
          </div>

          <!-- Dietary tags (Optional) -->
          <div class="field span-2">
            <label class="field-label">* Allergen & dietary tags <span class="text-soft">(optional)</span></label>
            <div class="chip-group">
              <span
                v-for="tag in tags"
                :key="tag.id"
                class="chip"
                :class="{ active: form.tags.includes(tag.id) }"
                @click="toggleTag(tag)"
              >
                {{ tag.name }}
              </span>
            </div>
          </div>

          <!-- Storage instructions / notes (Optional) -->
          <div class="field span-2">
            <label class="field-label">* Storage instructions / description <span class="text-soft">(optional - shown on request card)</span></label>
            <textarea v-model="form.notes" rows="3" placeholder="e.g. Keep refrigerated below 4°C; best collected same day"></textarea>
          </div>

          <!-- Privacy toggle
          <div class="field span-2">
            <div class="toggle-row">
              <div>
                <div class="tr-label">Hide phone / email from recipients</div>
                <div class="tr-desc">Reduces spam and rude messages; recipients still reach you in-app</div>
              </div>
              <div class="toggle" :class="{ on: form.hideContact }" @click="form.hideContact = !form.hideContact"></div>
            </div>
          </div> -->

          <!-- Pickup timeslot management -->
          <div class="field">
            <label class="field-label">Recipients allowed per slot</label>
            <div class="slot-stepper">
              <button type="button" class="stepper-btn" @click="adjustSlotCapacity(-1)">−</button>
              <input type="number" v-model.number="form.slotCapacity" min="1" max="20" class="stepper-input">
              <button type="button" class="stepper-btn" @click="adjustSlotCapacity(1)">+</button>
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

              <button type="button" class="btn-outline" @click="addTimeslot">
                Add timeslot
              </button>
            </div>

            <p v-if="timeslotError" class="timeslot-error" role="alert">
              {{ timeslotError }}
            </p>

            <div v-if="timeslots.length" class="timeslot-list">
              <div v-for="(slot, index) in timeslots" :key="slot.id" class="timeslot-item">
                <span>
                  {{ formatTimeslot(slot) }}
                </span>
                <button type="button" class="upload-remove" @click="removeTimeslot(index)">
                  Remove
                </button>
              </div>
            </div>

            <div v-else class="timeslot-empty">
              No pickup timeslots added yet.
            </div>
          </div>

          <!-- Privacy toggle -->
          <div class="field span-2">
            <div class="toggle-row">
              <div>
                <div class="tr-label">Hide phone / email from recipients</div>
                <div class="tr-desc">Reduces spam and rude messages; recipients still reach you in-app</div>
              </div>
              <div class="toggle" :class="{ on: form.hideContact }" @click="form.hideContact = !form.hideContact"></div>
            </div>
          </div>
        </div>

        <p v-if="submitError" class="submit-message error" role="alert">{{ submitError }}</p>
        <p v-else-if="catalogueError" class="submit-message error" role="alert">{{ catalogueError }}</p>

        <div class="btn-row">
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 22px;" :disabled="isSubmitting || isLoadingCatalogue" @click="submitStatus = 'LIVE'">
            {{ isSubmitting && submitStatus === 'LIVE' ? 'Publishing…' : 'Publish listing' }}
          </button>
          <button type="submit" class="btn-outline" :disabled="isSubmitting || isLoadingCatalogue" @click="submitStatus = 'DRAFT'">
            {{ isSubmitting && submitStatus === 'DRAFT' ? 'Saving…' : 'Save as draft' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { createDonorListing, getListingCatalogue, uploadPhoto } from '../../api/client'
import { toListingRow } from '../../api/donorListingAdapter'
import AddressAutocomplete from '../../components/AddressAutocomplete.vue'

const props = defineProps({
  donorId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['add-listing'])

const fileInputRef = ref(null)
const isDragOver = ref(false)
const uploadedPhoto = ref(null)
const photoError = ref('')
const categories = ref([])
const tags = ref([])
const isLoadingCatalogue = ref(true)
const catalogueError = ref('')
const isSubmitting = ref(false)
const submitError = ref('')
const submitStatus = ref('LIVE')
const today = new Date().toISOString().slice(0, 10)
const timeslots = ref([])
const timeslotError = ref('')
const slotDraft = reactive({
  date: '',
  startTime: '',
  endTime: '',
})

const form = reactive({
  name: '',
  category: null,
  quantity: '',
  expiryDate: '',
  deadline: '',
  addressLine: '14 Rundle Mall',
  suburb: 'Adelaide',
  stateRegion: 'SA',
  slotCapacity: 4,              // Default 4 per slot/hour
  autoApproval: true,           // Default: yes
  requirePickupCode: false,     // Default: no
  tags: [],
  hideContact: true,
  notes: '',
  maxQty: '4 units'
})

onMounted(async () => {
  try {
    const catalogue = await getListingCatalogue()
    categories.value = catalogue.categories
    tags.value = catalogue.tags
    form.category = categories.value[0]?.id ?? null
  } catch (error) {
    catalogueError.value = error.message || 'Unable to load listing categories and tags.'
  } finally {
    isLoadingCatalogue.value = false
  }
})

function adjustSlotCapacity(delta) {
  const newVal = form.slotCapacity + delta
  if (newVal >= 1 && newVal <= 20) {
    form.slotCapacity = newVal
  }
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
  if (timeslots.value.some(slot => slot.startTime === startTime && slot.endTime === endTime)) {
    timeslotError.value = 'This pickup timeslot has already been added.'
    return
  }

  timeslots.value.push({
    id: `${startTime}-${endTime}`,
    startTime,
    endTime,
    maxRecipients: form.slotCapacity,
  })
  slotDraft.date = ''
  slotDraft.startTime = ''
  slotDraft.endTime = ''
}

function removeTimeslot(index) {
  timeslots.value.splice(index, 1)
}

function formatTimeslot(slot) {
  const [date] = slot.startTime.split('T')
  const startTime = slot.startTime.slice(11, 16)
  const endTime = slot.endTime.slice(11, 16)
  return `${date}: ${startTime} - ${endTime} (${slot.maxRecipients} recipients)`
}

function triggerFileInput() {
  if (fileInputRef.value) fileInputRef.value.click()
}

function processImageFile(file) {
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
  reader.onload = (e) => {
    uploadedPhoto.value = { name: file.name, file, url: e.target.result }
  }
  reader.readAsDataURL(file)
}

function handleFileUpload(e) {
  const file = e.target.files?.[0]
  if (file) processImageFile(file)
}

function handleFileDrop(e) {
  e.preventDefault()
  isDragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) processImageFile(file)
}

function removeUpload() {
  uploadedPhoto.value = null
  photoError.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function toggleTag(tag) {
  const idx = form.tags.indexOf(tag.id)
  if (idx > -1) form.tags.splice(idx, 1)
  else form.tags.push(tag.id)
}

function applyPickupAddress(address) {
  if (address.suburb) form.suburb = address.suburb
  if (address.stateRegion) form.stateRegion = address.stateRegion
}

function quantityValue(value) {
  const quantity = Number.parseFloat(String(value).replace(/[^0-9.]/g, ''))
  return Number.isFinite(quantity) && quantity > 0 ? quantity : null
}

function buildRequest() {
  const quantity = quantityValue(form.quantity)
  if (!quantity) throw new Error('Quantity must be a number greater than zero.')
  if (!form.category) throw new Error('Select a category before publishing.')

  return {
    name: form.name.trim(),
    description: form.notes,
    categoryId: form.category,
    quantity,
    expiryDate: form.expiryDate || null,
    collectionDeadline: form.deadline || null,
    addressLine: form.addressLine.trim(),
    suburb: form.suburb.trim(),
    region: form.stateRegion.trim(),
    maxDailyCapacity: quantityValue(form.maxQty),
    photoUrl: null,
    tagIds: [...form.tags],
    timeslots: timeslots.value.map(({ startTime, endTime, maxRecipients }) => ({
      startTime,
      endTime,
      maxRecipients,
    })),
    isAutoApproved: form.autoApproval,
    usePickupCode: form.requirePickupCode,
    status: submitStatus.value
  }
}

async function submitListing() {
  submitError.value = ''
  if (!form.name.trim() || !form.addressLine.trim() || !form.suburb.trim() || !form.stateRegion.trim()) {
    submitError.value = 'Complete all required fields before publishing.'
    return
  }

  let request
  try {
    request = buildRequest()
  } catch (error) {
    submitError.value = error.message
    return
  }

  if (photoError.value) return

  isSubmitting.value = true
  try {
    if (uploadedPhoto.value?.file) {
      request.photoUrl = await uploadPhoto(uploadedPhoto.value.file)
    }
    const created = await createDonorListing(props.donorId, request)

    const newRecord = toListingRow({
      ...request,
      listingId: created.listingId,
      status: created.status || request.status || 'DRAFT',
      category: categories.value.find(category => category.id === form.category)?.name || '',
      photoUrl: created.photoUrl || request.photoUrl,
      tags: tags.value.filter(tag => form.tags.includes(tag.id)).map(tag => tag.name),
      timeslots: request.timeslots.map(slot => ({ ...slot, remainingSlot: slot.maxRecipients })),
      slotCapacity: form.slotCapacity,
      hideContact: form.hideContact,
    })

    emit('add-listing', newRecord)
  } catch (error) {
    submitError.value = error.message || 'Unable to create the listing.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--r-lg); padding: 20px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.span-2 { grid-column: 1 / -1; }
.field { margin-bottom: 4px; }
.field-label { font-size: 12.5px; font-weight: 700; color: var(--text); margin-bottom: 6px; display: block; }
.req-tag { font-size: 11px; font-weight: 700; color: var(--primary-dark); margin-left: 4px; }
.field input, .field :deep(.address-autocomplete input), .field select, .field textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 14px; box-sizing: border-box; }
.field input:focus, .field :deep(.address-autocomplete input:focus), .field select:focus, .field textarea:focus { outline: 2px solid var(--primary); outline-offset: 1px; border-color: var(--primary); }
.text-soft { color: var(--text-soft); font-weight: 500; }

.upload-box { border: 1.5px dashed var(--border); border-radius: var(--r-md); padding: 28px; text-align: center; cursor: pointer; background: var(--surface-2); transition: .15s; }
.upload-box:hover, .upload-box.drag-over { border-color: var(--primary); background: var(--primary-tint); }
.upload-box.has-image { padding: 14px; background: var(--surface); cursor: default; }
.upload-icon { font-size: 26px; margin-bottom: 6px; }
.upload-text { font-size: 13px; font-weight: 700; color: var(--text); }
.upload-hint { font-size: 11px; color: var(--text-soft); margin-top: 3px; }
.upload-preview-wrap { display: flex; align-items: center; gap: 14px; }
.upload-preview-wrap img { height: 90px; width: 120px; object-fit: cover; border-radius: 8px; border: 1px solid var(--border); }
.upload-file-name { font-size: 12.5px; font-weight: 700; color: var(--text); }
.upload-remove { font-size: 11.5px; color: var(--danger); font-weight: 700; background: none; border: none; padding: 0; margin-top: 6px; text-decoration: underline; cursor: pointer; }

/* Stepper widget for slot capacity */
.slot-stepper { display: flex; align-items: center; gap: 8px; }
.stepper-btn {
  width: 34px; height: 34px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface-2); font-weight: 700; font-size: 16px; cursor: pointer;
}
.stepper-btn:hover { border-color: var(--primary); color: var(--primary-dark); }
.stepper-input { width: 60px !important; text-align: center; }
.stepper-desc { font-size: 12.5px; color: var(--text-muted); }

.fields-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

/* Checkbox group */
.checkbox-group {
  background: var(--surface-2);
  border-radius: var(--r-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.checkbox-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  cursor: pointer;
  font-size: 13px;
}
.checkbox-row input { width: auto !important; margin-top: 2px; }
.checkbox-desc { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }

.chip-group { display: flex; gap: 6px; flex-wrap: wrap; }
.chip { padding: 6px 12px; border-radius: 20px; border: 1.3px solid var(--border); font-size: 12px; font-weight: 600; color: var(--text-muted); background: var(--surface); cursor: pointer; }
.chip.active { background: var(--primary-dark); border-color: var(--primary-dark); color: #fff; }

.toggle-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); margin: 6px 0; }
.toggle-row .tr-label { font-size: 13.5px; font-weight: 700; }
.toggle-row .tr-desc { font-size: 11.5px; color: var(--text-soft); margin-top: 2px; }
.toggle { position: relative; width: 40px; height: 22px; background: var(--border); border-radius: 20px; cursor: pointer; transition: .15s; }
.toggle::after { content: ''; position: absolute; width: 16px; height: 16px; background: #fff; border-radius: 50%; top: 3px; left: 3px; transition: .15s; }
.toggle.on { background: var(--primary); }
.toggle.on::after { left: 21px; }

.btn-row { display: flex; gap: 10px; margin-top: 18px; }
.submit-message { margin: 18px 0 0; font-size: 13px; }
.submit-message.error { color: var(--danger); }
.btn-primary { padding: 11px 22px; border: none; border-radius: var(--r-sm); background: var(--primary); color: #fff; font-weight: 700; font-size: 14.5px; cursor: pointer; }
.btn-primary:hover { background: var(--primary-dark); }
.btn-primary:disabled, .btn-outline:disabled { opacity: .6; cursor: not-allowed; }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 9px 16px; border-radius: var(--r-sm); font-weight: 700; font-size: 13px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
@media (max-width: 980px) { .form-grid { grid-template-columns: 1fr; } }

.timeslot-section {
  background: var(--surface-2);
  padding: 14px;
  border-radius: var(--r-md);
}

.timeslot-form {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.timeslot-form input {
  width: auto;
}

.timeslot-form .btn-outline {
  margin-left: 4px;
}

.timeslot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.timeslot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 9px 11px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}

.timeslot-empty {
  margin-top: 10px;
  color: var(--text-soft);
  font-size: 12px;
}

.timeslot-error {
  margin: 8px 0 0;
  color: var(--danger);
  font-size: 12px;
}
</style>