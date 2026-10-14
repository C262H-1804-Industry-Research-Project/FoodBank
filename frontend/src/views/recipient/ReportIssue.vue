<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>File a report</h2>
        <div class="sub">Let an admin know if something wasn't right with a collection</div>
      </div>
    </div>

    <div class="card" style="max-width:560px;">
      <p v-if="loadError" class="form-error" role="alert">
        {{ loadError }} <button type="button" class="btn-link" @click="loadCollections">Retry</button>
      </p>
      <p v-else-if="!isLoading && !collections.length" class="form-hint">
        You have no approved collections to report yet.
      </p>

      <form @submit.prevent="handleSubmit">
        <div class="field">
          <label class="field-label">Which collection?</label>
          <select v-model="reportForm.requestId" :disabled="isLoading || isSubmitting" required>
            <option value="" disabled>{{ isLoading ? 'Loading your collections…' : 'Select a collection…' }}</option>
            <option v-for="c in collections" :key="c.requestId" :value="c.requestId">
              {{ c.label }}
            </option>
          </select>
        </div>

        <div class="field">
          <label class="field-label">Reason</label>
          <select v-model="reportForm.reason" :disabled="isSubmitting" required>
            <option value="" disabled>Select a reason…</option>
            <option value="Food quality issue">Food quality issue</option>
            <option value="Item didn't match listing">Item didn't match listing</option>
            <option value="Donor no-show">Donor no-show</option>
            <option value="Other">Other</option>
          </select>
        </div>

        <div class="field">
          <label class="field-label">Additional details <span class="opt-label">(optional)</span></label>
          <textarea 
            v-model="reportForm.details" 
            rows="4" 
            maxlength="1000"
            :disabled="isSubmitting"
            placeholder="Describe what went wrong with this collection…"
          ></textarea>
        </div>

        <!-- Photo Proof Upload (same as donor) -->
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
                  <button type="button" class="upload-remove" :disabled="isSubmitting" @click="removeUpload">Remove photo</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="btn-row">
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 22px;" :disabled="isSubmitting">
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
import { getRecipientReportableRequests, submitReport, uploadPhoto } from '../../api/client'
import { formatPickupSlot } from '../../api/listingAdapter'

const emit = defineEmits(['report-submitted'])

const MAX_PHOTO_BYTES = 5 * 1024 * 1024

// The recipient's approved pickups, which are the collections they can report
const reportableRequests = ref([])
const isLoading = ref(true)
const loadError = ref('')
let loadController = null

async function loadCollections() {
  loadController?.abort()
  const controller = new AbortController()
  loadController = controller
  isLoading.value = true
  loadError.value = ''
  try {
    reportableRequests.value = await getRecipientReportableRequests(controller.signal)
  } catch (error) {
    if (!controller.signal.aborted) loadError.value = error.message || 'Unable to load your collections.'
  } finally {
    if (loadController === controller) isLoading.value = false
  }
}

onMounted(loadCollections)
onBeforeUnmount(() => loadController?.abort())

const collections = computed(() => reportableRequests.value.map(r => ({
  ...r,
  label: `${r.listingName || 'Listing'} — ${r.donorName || 'Unknown donor'}, ${formatPickupSlot({ startTime: r.timeslotStart, endTime: r.timeslotEnd })}`
})))

const reportForm = reactive({
  requestId: '',
  reason: '',
  details: ''
})

const fileInputRef = ref(null)
const isDragOver = ref(false)
const uploadedPhoto = ref(null)
const isSubmitting = ref(false)
const submitError = ref('')

function triggerFileInput() {
  if (fileInputRef.value && !isSubmitting.value) fileInputRef.value.click()
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
  if (isSubmitting.value) return
  const file = e.dataTransfer?.files?.[0]
  if (file) processImageFile(file)
}

function removeUpload() {
  uploadedPhoto.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function resetForm() {
  reportForm.requestId = ''
  reportForm.reason = ''
  reportForm.details = ''
  submitError.value = ''
  removeUpload()
}

async function handleSubmit() {
  const collection = collections.value.find(c => c.requestId === reportForm.requestId)
  if (!collection || !reportForm.reason || isSubmitting.value) return

  isSubmitting.value = true
  submitError.value = ''
  try {
    const evidenceImageUrl = uploadedPhoto.value ? await uploadPhoto(uploadedPhoto.value.file) : null
    const created = await submitReport({
      reportType: 'FOOD_QUALITY',
      listingId: collection.listingId,
      pickupRequestId: collection.requestId,
      reason: reportForm.reason,
      details: reportForm.details.trim(),
      evidenceImageUrl
    })

    emit('report-submitted', created)
    alert(`Report for "${collection.listingName}" has been submitted to administrators.`)
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
.form-error { color: var(--danger); font-size: 13px; font-weight: 600; margin: 0 0 12px; }
.form-hint { color: var(--text-muted); font-size: 13px; margin: 0 0 12px; }
.btn-link { background: none; border: none; color: var(--primary-dark); font-weight: 700; cursor: pointer; padding: 0; text-decoration: underline; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.card {
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

.field select, .field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
  box-sizing: border-box;
}
.field select:focus, .field textarea:focus {
  outline: 2px solid var(--primary);
  border-color: var(--primary);
}

.upload-box {
  border: 1.5px dashed var(--border);
  border-radius: var(--r-md);
  padding: 22px;
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
  font-size: 14px;
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
</style>