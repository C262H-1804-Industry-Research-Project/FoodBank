<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>New listing</h2>
        <div class="sub">Publish surplus food for recipients to reserve</div>
      </div>
    </div>

    <div class="card">
      <form @submit.prevent="submitListing('Live')">
        <div class="form-grid">
          <!-- Food Photo upload -->
          <div class="field span-2">
            <label class="field-label">Food photo</label>
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
            </div>
          </div>

          <div class="field span-2">
            <label class="field-label">Food name</label>
            <input v-model="form.name" type="text" placeholder="e.g. Sourdough loaves" required>
          </div>

          <div class="field">
            <label class="field-label">Category</label>
            <select v-model="form.category">
              <option value="Bakery">Bakery</option>
              <option value="Produce">Produce</option>
              <option value="Dairy">Dairy</option>
              <option value="Pantry">Pantry</option>
            </select>
          </div>

          <div class="field">
            <label class="field-label">Quantity</label>
            <input v-model="form.quantity" type="text" placeholder="e.g. 12 units" required>
          </div>

          <div class="field">
            <label class="field-label">Expiry date</label>
            <input v-model="form.expiryDate" type="text" placeholder="dd/mm/yyyy">
          </div>

          <div class="field">
            <label class="field-label">Latest collection deadline <span class="text-soft">(optional)</span></label>
            <input v-model="form.deadline" type="text" placeholder="dd/mm/yyyy, hh:mm">
          </div>

          <!-- Structured 3-field Pickup Address -->
          <div class="field span-2">
            <label class="field-label">Pickup street address</label>
            <input v-model="form.addressLine" type="text" placeholder="Street number & name (e.g. 14 Rundle Mall)" required>
          </div>

          <div class="field">
            <label class="field-label">Suburb / Area</label>
            <input v-model="form.suburb" type="text" placeholder="e.g. Adelaide" required>
          </div>

          <div class="field">
            <label class="field-label">State / Region</label>
            <input v-model="form.stateRegion" type="text" placeholder="e.g. SA" required>
          </div>

          <!-- Timeslot capacity adjustment (Default 4 per slot/hour) -->
          <div class="field">
            <label class="field-label">Recipients allowed per 1-hour slot</label>
            <div class="slot-stepper">
              <button type="button" class="stepper-btn" @click="adjustSlotCapacity(-1)">−</button>
              <input type="number" v-model.number="form.slotCapacity" min="1" max="20" class="stepper-input">
              <button type="button" class="stepper-btn" @click="adjustSlotCapacity(1)">+</button>
              <span class="stepper-desc">recipients / slot</span>
            </div>
          </div>

          <div class="field">
            <label class="field-label">Max quantity per recipient / day</label>
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

          <!-- Dietary tags -->
          <div class="field span-2">
            <label class="field-label">Allergen & dietary tags</label>
            <div class="chip-group">
              <span
                v-for="tag in dietaryTags"
                :key="tag"
                class="chip"
                :class="{ active: form.tags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <!-- Storage instructions / notes (Shown on food card) -->
          <div class="field span-2">
            <label class="field-label">Storage instructions / additional notes <span class="text-soft">(displayed on food card)</span></label>
            <textarea v-model="form.notes" rows="3" placeholder="e.g. Keep refrigerated below 4°C; best collected same day"></textarea>
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

        <div class="btn-row">
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 22px;">Publish listing</button>
          <button type="button" class="btn-outline" @click="submitListing('Draft')">Save as draft</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive } from 'vue'

const emit = defineEmits(['add-listing'])

const fileInputRef = ref(null)
const isDragOver = ref(false)
const uploadedPhoto = ref(null)

const dietaryTags = ['Contains gluten', 'Contains nuts', 'Contains pork', 'Vegetarian', 'Halal']

const form = reactive({
  name: '',
  category: 'Bakery',
  quantity: '',
  expiryDate: '',
  deadline: 'Today, 8:00pm',
  addressLine: '14 Rundle Mall',
  suburb: 'Adelaide',
  stateRegion: 'SA',
  slotCapacity: 4,              // Default 4 per slot/hour
  autoApproval: true,           // Default: yes
  requirePickupCode: false,     // Default: no
  tags: ['Contains gluten'],
  hideContact: true,
  notes: '',
  maxQty: '4 units'
})

function adjustSlotCapacity(delta) {
  const newVal = form.slotCapacity + delta
  if (newVal >= 1 && newVal <= 20) {
    form.slotCapacity = newVal
  }
}

function triggerFileInput() {
  if (fileInputRef.value) fileInputRef.value.click()
}

function processImageFile(file) {
  if (!file || !file.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedPhoto.value = { name: file.name, url: e.target.result }
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
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function toggleTag(tag) {
  const idx = form.tags.indexOf(tag)
  if (idx > -1) form.tags.splice(idx, 1)
  else form.tags.push(tag)
}

function submitListing(status) {
  if (!form.name || !form.quantity) return

  const newRecord = {
    id: Date.now(),
    name: form.name,
    category: form.category,
    quantity: form.quantity,
    deadline: form.deadline || '—',
    slots: `5:30–6:30pm (${form.slotCapacity} slots/hr)`,
    booked: `0/${form.slotCapacity}`,
    slotCapacity: form.slotCapacity,
    autoApproval: form.autoApproval,
    requirePickupCode: form.requirePickupCode,
    addressLine: form.addressLine,
    suburb: form.suburb,
    stateRegion: form.stateRegion,
    status: status,
    statusClass: status === 'Live' ? 'status-live' : 'status-draft',
    hideContact: form.hideContact,
    notes: form.notes,
    tags: [...form.tags]
  }

  emit('add-listing', newRecord)
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
.field input, .field select, .field textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 14px; }
.field input:focus, .field select:focus, .field textarea:focus { outline: 2px solid var(--primary); outline-offset: 1px; border-color: var(--primary); }
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
.btn-primary { padding: 11px 22px; border: none; border-radius: var(--r-sm); background: var(--primary); color: #fff; font-weight: 700; font-size: 14.5px; cursor: pointer; }
.btn-primary:hover { background: var(--primary-dark); }
.btn-outline { background: transparent; border: 1.5px solid var(--border); color: var(--text); padding: 9px 16px; border-radius: var(--r-sm); font-weight: 700; font-size: 13px; cursor: pointer; }
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }
@media (max-width: 980px) { .form-grid { grid-template-columns: 1fr; } }
</style>