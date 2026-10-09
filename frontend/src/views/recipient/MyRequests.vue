<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>My requests</h2>
        <div class="sub">Track every request from submitted to collected — the exact pickup address appears here once a donor approves you</div>
      </div>
    </div>
    <table>
      <thead>
        <tr>
          <th>Food item</th>
          <th>Donor</th>
          <th>Pickup slot (start – end)</th>
          <th>Pickup address</th>
          <th>Status</th>
          <th>Pickup code</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="req in requests" :key="req.id">
          <td>{{ req.foodItem }}</td>
          <td>{{ req.donor }}</td>
          <td>{{ req.pickupSlot || req.pickupTime || '—' }}</td>
          <td>
            <span v-if="req.status === 'Requested'" style="color:var(--text-soft);">Available once approved</span>
            <span v-else>{{ req.address || '—' }}</span>
          </td>
          <td><span class="badge-status" :class="req.statusClass">{{ req.status }}</span></td>
          <td>
            <code v-if="req.code" class="pcode">{{ req.code }}</code>
            <span v-else>—</span>
          </td>
          <td>
            <button v-if="req.status === 'Requested'" class="btn-outline btn-sm" @click="emit('cancel', req.id)">Cancel</button>
            <button v-else class="btn-outline btn-sm">Details</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
defineProps({
  requests: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['cancel'])
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
table {
  width: 100%;
  border-collapse: collapse;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  overflow: hidden;
}
thead th {
  text-align: left;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: .05em;
  color: var(--text-soft);
  background: var(--surface-2);
  padding: 11px 16px;
  font-weight: 700;
}
tbody td {
  padding: 13px 16px;
  font-size: 13px;
  border-top: 1px solid var(--border);
  color: var(--text);
}
tbody tr:hover { background: #FBFAF6; }
code.pcode {
  background: var(--primary-dark);
  color: #EADFBF;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .03em;
}
.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-requested { background: var(--amber-tint); color: var(--amber); }
.status-reserved { background: var(--primary-tint); color: var(--primary-dark); }
.status-expired { background: var(--surface-2); color: var(--text-soft); }
.btn-outline {
  background: transparent;
  border: 1.5px solid var(--border);
  color: var(--text);
  padding: 6px 12px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
}
.btn-outline:hover {
  border-color: var(--primary);
  color: var(--primary-dark);
}
.btn-sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }
</style>