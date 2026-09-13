<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Report an issue</h2>
        <div class="sub">Let an admin know if something wasn't right with a collection</div>
      </div>
    </div>
    <div class="card" style="max-width:520px;">
      <div class="field">
        <label class="field-label">Which collection?</label>
        <select v-model="reportForm.collection">
          <option value="Mixed vegetable crates">Mixed vegetable crates — Norwood Fresh Markets, Reserved</option>
          <option value="Sourdough loaves">Sourdough loaves — Green Leaf Café, Requested</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">Reason</label>
        <select v-model="reportForm.reason">
          <option value="Food quality issue">Food quality issue</option>
          <option value="Item didn't match listing">Item didn't match listing</option>
          <option value="Donor no-show">Donor no-show</option>
          <option value="Other">Other</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">Details</label>
        <textarea v-model="reportForm.details" rows="4" placeholder="Tell us what happened…"></textarea>
      </div>
      <button class="btn-primary" style="width:auto; padding:10px 22px;" @click="submitReport">Submit report</button>
    </div>
  </section>
</template>

<script setup>
import { reactive } from 'vue'

const reportForm = reactive({
  collection: 'Mixed vegetable crates',
  reason: 'Food quality issue',
  details: ''
})

function submitReport() {
  alert('Report submitted to admin!')
  reportForm.details = ''
}
</script>

<style scoped>
.content-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}
.content-head h2 { font-size: 22px; }
.content-head .sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  padding: 20px;
}
.field { margin-bottom: 16px; }
.field-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
  display: block;
}
.field select, .field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
}
.field select:focus, .field textarea:focus {
  outline: 2px solid var(--primary);
  outline-offset: 1px;
  border-color: var(--primary);
}
.btn-primary {
  padding: 12px;
  border: none;
  border-radius: var(--r-sm);
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  font-size: 14.5px;
}
.btn-primary:hover { background: var(--primary-dark); }
</style>