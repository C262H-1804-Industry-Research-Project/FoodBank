<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>File a report</h2>
        <div class="sub">Report issues or discrepancies regarding your food donations, collection timeslots, and specific recipients</div>
      </div>
    </div>

    <div class="card report-card">
      <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
      <p v-else-if="!isLoading && !listingOptions.length" class="form-hint">
        You have no approved pickups to report yet.
      </p>

      <form @submit.prevent="handleSubmitReport">
        <!-- 1. Select Food Listing -->
        <div class="field">
          <label class="field-label">Select food listing</label>
          <select v-model="form.selectedListingId" :disabled="isLoading" @change="handleListingChange" required>
            <option value="" disabled>{{ isLoading ? 'Loading your pickups…' : 'Choose a food listing…' }}</option>
            <option 
              v-for="listing in listingOptions" 
              :key="listing.id" 
              :value="listing.id"
            >
              {{ listing.name }}
            </option>
          </select>
        </div>

        <!-- 2. Select Timeslot (Inserted between Food Listing and Recipient) -->
        <div class="field">
          <label class="field-label">Select pickup timeslot</label>
          <select 
            v-model="form.selectedTimeslotId" 
            :disabled="!availableTimeslots.length" 
            @change="handleTimeslotChange"
            required
          >
            <option value="" disabled>
              {{ availableTimeslots.length ? 'Choose the relevant pickup timeslot…' : 'Please select a food listing first' }}
            </option>
            <option 
              v-for="slot in availableTimeslots" 
              :key="slot.id" 
              :value="slot.id"
            >
              {{ slot.label }}
            </option>
          </select>
        </div>

        <!-- 3. Select Recipient Organisation (Filtered by chosen timeslot) -->
        <div class="field">
          <label class="field-label">Select recipient to report</label>
          <select 
            v-model="form.selectedRequestId" 
            :disabled="!availableRecipients.length" 
            required
          >
            <option value="" disabled>
              {{ availableRecipients.length ? 'Choose the recipient organisation…' : 'Please select a timeslot first' }}
            </option>
            <option 
              v-for="r in availableRecipients" 
              :key="r.requestId" 
              :value="r.requestId"
            >
              {{ r.recipientName }} ({{ r.quantityRequested }} requested)
            </option>
          </select>
        </div>

        <!-- 4. Actual Transaction Time (Date widget removed) -->
        <div class="field">
          <label class="field-label">Actual transaction time</label>
          <input 
            v-model="form.actualTime" 
            type="time" 
            required
          >
        </div>

        <!-- 5. Reason -->
        <div class="field">
          <label class="field-label">Reason</label>
          <select v-model="form.reason" required>
            <option value="" disabled>Select a reason…</option>
            <option v-for="r in reasonOptions" :key="r" :value="r">{{ r }}</option>
          </select>
        </div>

        <!-- 6. Details Textarea -->
        <div class="field">
          <label class="field-label">Details of the issue <span class="opt-label">(optional)</span></label>
          <textarea 
            v-model="form.details" 
            rows="4" 
            placeholder="Please specify what happened…" 
          ></textarea>
        </div>

        <!-- 7. Photo Proof Upload -->
        <div class="field">
          <label class="field-label">* Upload photo proof <span class="opt-label">(optional)</span></label>
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
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 24px;" :disabled="isSubmitting">
            {{ isSubmitting ? 'Submitting…' : 'Submit report' }}
          </button>
          <button type="button" class="btn-outline" :disabled="isSubmitting" @click="resetForm">
            Cancel
          </button>
        </div>
        <p v-if="submitError" class="form-error" role="alert">{{ submitError }}</p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { getReportableRequests, submitReport, uploadPhoto } from '../../api/client'
import { formatPickupSlot } from '../../api/listingAdapter'

const props = defineProps({
  donorId: { type: Number, required: true }
})

const emit = defineEmits(['report-submitted'])

const MAX_PHOTO_BYTES = 5 * 1024 * 1024

const reasonOptions = [
  'Recipient no-show',
  'Late collection',
  'Collected incorrect quantity',
  'Disrespectful behaviour',
  'Other'
]

// Approved pickup requests on this donor's listings, from the API
const reportableRequests = ref([])
const isLoading = ref(true)
const loadError = ref('')
const loadController = new AbortController()

onMounted(async () => {
  try {
    reportableRequests.value = await getReportableRequests(props.donorId, loadController.signal)
  } catch (error) {
    if (!loadController.signal.aborted) loadError.value = error.message || 'Unable to load your pickups.'
  } finally {
    isLoading.value = false
  }
})
onBeforeUnmount(() => loadController.abort())

const form = reactive({
  selectedListingId: '',
  selectedTimeslotId: '',
  selectedRequestId: '',
  actualTime: '17:30',
  reason: '',
  details: ''
})

// Listings that have at least one approved request
const listingOptions = computed(() => {
  const byId = new Map()
  for (const r of reportableRequests.value) {
    if (!byId.has(r.listingId)) byId.set(r.listingId, { id: r.listingId, name: r.listingName })
  }
  return [...byId.values()]
})

// Timeslots computed based on selected food listing
const availableTimeslots = computed(() => {
  const byId = new Map()
  for (const r of reportableRequests.value) {
    if (r.listingId !== form.selectedListingId || byId.has(r.timeslotId)) continue
    byId.set(r.timeslotId, {
      id: r.timeslotId,
      startTime: r.timeslotStart,
      label: formatPickupSlot({ startTime: r.timeslotStart, endTime: r.timeslotEnd })
    })
  }
  return [...byId.values()].sort((a, b) => a.startTime.localeCompare(b.startTime))
})

// Recipients (approved requests) computed based on selected timeslot
const availableRecipients = computed(() =>
  reportableRequests.value.filter(r => r.timeslotId === form.selectedTimeslotId)
)

const selectedRequest = computed(() =>
  reportableRequests.value.find(r => r.requestId === form.selectedRequestId) || null
)

function handleListingChange() {
  form.selectedTimeslotId = ''
  form.selectedRequestId = ''
}

function handleTimeslotChange() {
  form.selectedRequestId = ''
}

const fileInputRef = ref(null)
const isDragOver = ref(false)
const uploadedPhoto = ref(null)
const isSubmitting = ref(false)
const submitError = ref('')

function triggerFileInput() {
  if (fileInputRef.value) fileInputRef.value.click()
}

function processImageFile(file) {
  if (!file || !file.type.startsWith('image/')) return
  if (file.size > MAX_PHOTO_BYTES) {
    submitError.value = 'Photo must be 5MB or smaller.'
    return
  }
  submitError.value = ''
  // Keep the file so it can be uploaded on submit; the data URL is only for the preview
  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedPhoto.value = { name: file.name, url: e.target.result, file }
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
  form.selectedListingId = ''
  form.selectedTimeslotId = ''
  form.selectedRequestId = ''
  form.actualTime = '17:30'
  form.reason = ''
  form.details = ''
  submitError.value = ''
  removeUpload()
}

async function handleSubmitReport() {
  const request = selectedRequest.value
  if (!request || !form.reason || isSubmitting.value) return

  isSubmitting.value = true
  submitError.value = ''
  try {
    const evidenceImageUrl = uploadedPhoto.value ? await uploadPhoto(uploadedPhoto.value.file) : null
    // The form only asks for a time; the date comes from the pickup timeslot
    const slotDate = request.timeslotStart.split('T')[0]

    const created = await submitReport({
      reportType: 'RECIPIENT_BEHAVIOUR',
      listingId: request.listingId,
      pickupRequestId: request.requestId,
      reportedUserId: request.recipientUserId,
      reason: form.reason,
      details: form.details.trim(),
      evidenceImageUrl,
      actualTransactionTime: `${slotDate}T${form.actualTime}:00`
    })

    emit('report-submitted', created)
    alert(`Report regarding "${request.recipientName}" submitted successfully!`)
    resetForm()
  } catch (error) {
    submitError.value = error.message || 'Unable to submit the report.'
  } finally {
    isSubmitting.value = false
  }
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
  box-sizing: border-box;
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
.btn-primary:disabled, .btn-outline:disabled { opacity: .6; cursor: not-allowed; }

.form-error { color: var(--danger); font-size: 13px; font-weight: 600; margin: 0 0 12px; }
.form-hint { color: var(--text-muted); font-size: 13px; margin: 0 0 12px; }
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
</style>