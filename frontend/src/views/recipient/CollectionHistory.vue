<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Collection history</h2>
        <div class="sub">Everything your organisation has collected to date</div>
      </div>
    </div>
    <table>
      <thead>
        <tr>
          <th>Date collected</th>
          <th>Food item</th>
          <th>Donor</th>
          <th>Quantity</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(hist, idx) in historyList" :key="idx">
          <td>{{ hist.date }}</td>
          <td>{{ hist.foodItem }}</td>
          <td>{{ hist.donor }}</td>
          <td>{{ hist.quantity }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const historyList = ref([
  { date: '28 Aug 2026', foodItem: 'Bread & pastries', donor: 'Green Leaf Café', quantity: '15 units' },
  { date: '21 Aug 2026', foodItem: 'Vegetable crates', donor: 'Norwood Fresh Markets', quantity: '4 crates' },
  { date: '14 Aug 2026', foodItem: 'Canned goods', donor: 'Kent Town IGA', quantity: '22 units' }
])
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
</style>