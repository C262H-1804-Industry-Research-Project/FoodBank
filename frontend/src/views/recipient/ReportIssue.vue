<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Report an issue</h2>
        <div class="sub">Let an admin know if something wasn't right with a collection</div>
      </div>
    </div>

    <div class="card" style="max-width:520px;">
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
import { reactive } from 'vue'

const reportForm = reactive({
  collection: 'Mixed vegetable crates — Norwood Fresh Markets, Reserved',
  reason: 'Food quality issue',
  details: ''
})

function resetForm() {
  reportForm.collection = ''
  reportForm.reason = ''
  reportForm.details = ''
}

function handleSubmit() {
  if (!reportForm.collection || !reportForm.reason) return
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