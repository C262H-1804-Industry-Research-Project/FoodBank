<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>File a report</h2>
        <div class="sub">Let an admin know if something wasn't right with a collection</div>
      </div>
    </div>

    <div class="card" style="max-width:560px;">
      <form @submit.prevent="handleSubmit">
        <div class="field">
          <label class="field-label">Which collection?</label>
          <select v-model="reportForm.collection" required>
            <option value="" disabled>Select a collection…</option>
            <option value="Mixed vegetable crates — Norwood Fresh Markets, Reserved">
              Mixed vegetable crates — Norwood Fresh Markets, Reserved
            </option>
            <option value="Sourdough loaves — Green Leaf Café, Requested">
              Sourdough loaves — Green Leaf Café, Requested
            </option>
          </select>
        </div>

        <div class="field">
          <label class="field-label">Reason</label>
          <select v-model="reportForm.reason" required>
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
                  <button type="button" class="upload-remove" @click="removeUpload">Remove photo</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="btn-row">
          <button type="submit" class="btn-primary" style="width:auto; padding:11px 22px;">
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
import { ref, reactive } from 'vue'

const emit = defineEmits(['report-submitted'])

const reportForm = reactive({
  collection: 'Mixed vegetable crates — Norwood Fresh Markets, Reserved',
  reason: 'Food quality issue',
  details: ''
})

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
  reportForm.collection = ''
  reportForm.reason = ''
  reportForm.details = ''
  removeUpload()
}

function handleSubmit() {
  if (!reportForm.collection || !reportForm.reason) return

  const newReport = {
    id: Date.now(),
    collection: reportForm.collection,
    reason: reportForm.reason,
    details: reportForm.details || '—',
    submittedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
    status: 'Under Review',
    statusClass: 'status-pending',
    photo: uploadedPhoto.value ? { ...uploadedPhoto.value } : null
  }

  emit('report-submitted', newReport)
  alert(`Report for "${reportForm.collection}" has been submitted to administrators.`)
  resetForm()
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
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