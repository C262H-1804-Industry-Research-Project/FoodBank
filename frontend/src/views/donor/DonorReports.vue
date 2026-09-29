<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>File a report</h2>
        <div class="sub">Report issues or discrepancies regarding your food donations and specific recipients</div>
      </div>
    </div>

    <div class="card report-card">
      <form @submit.prevent="handleSubmitReport">
        <!-- 1. Select Food Listing / Order -->
        <div class="field">
          <label class="field-label">Select food listing</label>
          <select v-model="form.selectedOrder" @change="handleListingChange" required>
            <option value="" disabled>Choose a food listing…</option>
            <option 
              v-for="order in orderOptions" 
              :key="order.id" 
              :value="order.name"
            >
              {{ order.name }}
            </option>
          </select>
        </div>

        <!-- 2. Select Recipient Organisation (Linked to the food) -->
        <div class="field">
          <label class="field-label">Select recipient to report</label>
          <select v-model="form.selectedRecipient" :disabled="!availableRecipients.length" required>
            <option value="" disabled>
              {{ availableRecipients.length ? 'Choose the recipient organisation…' : 'Please select a food listing first' }}
            </option>
            <option 
              v-for="r in availableRecipients" 
              :key="r" 
              :value="r"
            >
              {{ r }}
            </option>
          </select>
        </div>

        <!-- 3. Actual Transaction Date & Time -->
        <div class="form-grid-row">
          <div class="field">
            <label class="field-label">Actual transaction date</label>
            <input 
              v-model="form.actualDate" 
              type="date" 
              required
            >
          </div>
          <div class="field">
            <label class="field-label">Actual transaction time</label>
            <input 
              v-model="form.actualTime" 
              type="time" 
              required
            >
          </div>
        </div>

        <!-- 4. Reason / Description Textarea -->
        <div class="field">
          <label class="field-label">Reason / details of the issue</label>
          <textarea 
            v-model="form.reason" 
            rows="4" 
            placeholder="Please specify what happened (e.g. Recipient no-show, collected incorrect quantity, collection delay, or disrespectful behavior)…" 
            required
          ></textarea>
        </div>

        <!-- 5. Photo Proof Upload -->
        <div class="field">
          <label class="field-label">Upload photo proof <span class="opt-label">(optional)</span></label>
          <div
            class="upload-box"
            :class="{ 'has-image': uploadedPhoto, 'drag-over': isDragOver }"
            @click="triggerFileInput"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop="handleFileDrop"
          >
            <input 
              ref="fileInputRef" 
              type="file" 
              accept="image/*" 
              style="display:none" 
              @change="handleFileUpload"
            >
            <div v-if="!uploadedPhoto">
              <div class="upload-icon">📷</div>
              <div class="upload-text">Click to upload photo, or drag and drop</div>
              <div class="upload-hint">PNG or JPG, up to 5MB</div>
            </div>
            <div v-else @click.stop>
              <div class="upload-preview-wrap">
                <img :src="uploadedPhoto.url" alt="Proof photo preview">
                <div>
                  <div class="upload-file-name">{{ uploadedPhoto.name }}</div>
                  <button type="button" class="upload-remove" @click="removeUpload">Remove photo</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Submit Button Row -->
        <div class="btn-row">
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 24px;">
            Submit report
          </button>
          <button type="button" class="btn-outline" @click="resetForm">
            Cancel
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const emit = defineEmits(['report-submitted'])

// Food listing options with recipients who claimed/requested slots
const orderOptions = [
  {
    id: 1,
    name: 'Sourdough loaves (12 units)',
    recipients: ['Community Kitchen East', 'Norwood Youth Refuge', 'Salvation Army Centre']
  },
  {
    id: 2,
    name: 'Pantry staples box (8 boxes)',
    recipients: ['St. Vincent de Paul Adelaide', 'Adelaide City Mission']
  },
  {
    id: 3,
    name: 'Dairy bundle (10 units)',
    recipients: ['Norwood Youth Refuge', 'Sturt Community Centre']
  }
]

const todayStr = new Date().toISOString().split('T')[0]

const form = reactive({
  selectedOrder: '',
  selectedRecipient: '',
  actualDate: todayStr,
  actualTime: '17:30',
  reason: ''
})

const availableRecipients = computed(() => {
  const match = orderOptions.find(o => o.name === form.selectedOrder)
  return match ? match.recipients : []
})

function handleListingChange() {
  form.selectedRecipient = ''
}

const fileInputRef = ref(null)
const isDragOver = ref(false)
const uploadedPhoto = ref(null)

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

function resetForm() {
  form.selectedOrder = ''
  form.selectedRecipient = ''
  form.actualDate = todayStr
  form.actualTime = '17:30'
  form.reason = ''
  removeUpload()
}

function handleSubmitReport() {
  if (!form.selectedOrder || !form.selectedRecipient || !form.reason.trim()) return

  const newReport = {
    id: Date.now(),
    foodName: form.selectedOrder,
    recipient: form.selectedRecipient,
    actualDate: `${form.actualDate} ${form.actualTime}`,
    reason: form.reason,
    status: 'Under Review',
    statusClass: 'status-pending'
  }

  emit('report-submitted', newReport)
  alert(`Report regarding "${form.selectedRecipient}" for "${form.selectedOrder}" submitted successfully!`)
  resetForm()
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.report-card {
  max-width: 620px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  padding: 24px;
}

.field { margin-bottom: 16px; }
.field-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
  display: block;
}
.opt-label { font-weight: 500; color: var(--text-soft); }

.field input, .field select, .field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
}
.field select:disabled {
  background: var(--surface-2);
  color: var(--text-soft);
  cursor: not-allowed;
}
.field input:focus, .field select:focus, .field textarea:focus {
  outline: 2px solid var(--primary);
  outline-offset: 1px;
  border-color: var(--primary);
}

.form-grid-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.upload-box {
  border: 1.5px dashed var(--border);
  border-radius: var(--r-md);
  padding: 24px;
  text-align: center;
  cursor: pointer;
  background: var(--surface-2);
  transition: .15s;
}
.upload-box:hover, .upload-box.drag-over {
  border-color: var(--primary);
  background: var(--primary-tint);
}
.upload-box.has-image {
  padding: 12px;
  background: var(--surface);
  cursor: default;
}
.upload-icon { font-size: 24px; margin-bottom: 4px; }
.upload-text { font-size: 13px; font-weight: 700; color: var(--text); }
.upload-hint { font-size: 11px; color: var(--text-soft); margin-top: 2px; }

.upload-preview-wrap { display: flex; align-items: center; gap: 14px; }
.upload-preview-wrap img {
  height: 80px;
  width: 110px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
}
.upload-file-name { font-size: 12.5px; font-weight: 700; color: var(--text); }
.upload-remove {
  font-size: 11.5px;
  color: var(--danger);
  font-weight: 700;
  background: none;
  border: none;
  padding: 0;
  margin-top: 6px;
  text-decoration: underline;
  cursor: pointer;
}

.btn-row { display: flex; gap: 10px; margin-top: 20px; }
.btn-primary {
  padding: 11px 22px;
  border: none;
  border-radius: var(--r-sm);
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  font-size: 14.5px;
  cursor: pointer;
}
.btn-primary:hover { background: var(--primary-dark); }
.btn-outline {
  background: transparent;
  border: 1.5px solid var(--border);
  color: var(--text);
  padding: 9px 16px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }

@media (max-width: 600px) {
  .form-grid-row { grid-template-columns: 1fr; }
}
</style>