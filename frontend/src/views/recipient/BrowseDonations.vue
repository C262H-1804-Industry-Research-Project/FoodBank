<template>
  <section class="panel-section">
    <!-- Filter toolbar -->
    <div class="filter-bar">
      <select v-model="filterCategory">
        <option value="All">All categories</option>
        <option value="Bakery">Bakery</option>
        <option value="Produce">Produce</option>
        <option value="Dairy">Dairy</option>
        <option value="Pantry">Pantry</option>
      </select>
      <input type="text" v-model="filterLocation" placeholder="📍 Suburb or postcode">
      <select v-model="filterTime">
        <option value="any">Any pickup time</option>
        <option value="today">Today</option>
        <option value="weekend">This weekend</option>
      </select>
      <input type="text" v-model="searchKeyword" placeholder="Search food name…">
      <div class="chip-group">
        <span
          v-for="tag in dietaryFilters"
          :key="tag"
          class="chip"
          :class="{ active: activeTags.includes(tag) }"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- Cards grid -->
    <div class="card-grid">
      <div
        v-for="item in filteredListings"
        :key="item.id"
        class="listing-card"
      >
        <div class="lc-image">{{ item.emoji }}</div>
        <div class="lc-body">
          <div class="lc-top">
            <div>
              <div class="lc-name">{{ item.name }}</div>
              <div class="lc-cat">{{ item.category }}</div>
            </div>
            <span class="badge-time" :class="item.urgencyClass">{{ item.timeLeft }}</span>
          </div>
          <div class="lc-meta">
            📍 {{ item.location }}<br>
            🕐 {{ item.pickupWindow }}
          </div>
          <div class="lc-tags">
            <span v-for="t in item.tags" :key="t" class="tag-pill">{{ t }}</span>
          </div>
        </div>
        <div class="lc-foot">
          <span class="lc-donor">{{ item.donor }}</span>
          <button class="btn-accent btn-sm" @click="emit('request', item)">Request</button>
        </div>
      </div>
    </div>
    <div v-if="filteredListings.length === 0" class="empty-note">No items match your filter criteria.</div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['request'])

const filterCategory = ref('All')
const filterLocation = ref('')
const filterTime = ref('any')
const searchKeyword = ref('')
const dietaryFilters = ['No pork', 'Nut-free', 'Vegetarian', 'Halal']
const activeTags = ref(['No pork'])

function toggleTag(tag) {
  const index = activeTags.value.indexOf(tag)
  if (index > -1) {
    activeTags.value.splice(index, 1)
  } else {
    activeTags.value.push(tag)
  }
}

const listingsData = ref([
  {
    id: 1,
    emoji: '🥖',
    name: 'Sourdough loaves (12)',
    category: 'Bakery',
    timeLeft: '2h left',
    urgencyClass: 'urgent',
    location: 'Adelaide CBD area · ~1.2km away',
    pickupWindow: 'Pickup today, 5:30–6:00pm',
    tags: ['Contains gluten'],
    donor: 'Green Leaf Café'
  },
  {
    id: 2,
    emoji: '🥗',
    name: 'Mixed vegetable crates (5)',
    category: 'Produce',
    timeLeft: '18h left',
    urgencyClass: 'soon',
    location: 'Norwood area · ~3.4km away',
    pickupWindow: 'Pickup tomorrow, 9:00–9:30am',
    tags: ['Vegetarian', 'No pork'],
    donor: 'Norwood Fresh Markets'
  },
  {
    id: 3,
    emoji: '🥫',
    name: 'Pantry staples box (8)',
    category: 'Pantry',
    timeLeft: '2 days left',
    urgencyClass: 'ok',
    location: 'Kent Town area · ~2.1km away',
    pickupWindow: 'Pickup Sat, 10:00–10:30am',
    tags: ['Halal', 'Nut-free'],
    donor: 'Kent Town IGA'
  }
])

const filteredListings = computed(() => {
  return listingsData.value.filter(item => {
    if (filterCategory.value !== 'All' && item.category !== filterCategory.value) {
      return false
    }
    if (searchKeyword.value.trim() && !item.name.toLowerCase().includes(searchKeyword.value.toLowerCase().trim())) {
      return false
    }
    return true
  })
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 14px 16px;
  margin-bottom: 22px;
  align-items: center;
}
.filter-bar select, .filter-bar input[type=text] {
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}
.chip-group { display: flex; gap: 6px; flex-wrap: wrap; }
.chip {
  padding: 6px 12px;
  border-radius: 20px;
  border: 1.3px solid var(--border);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--surface);
  cursor: pointer;
}
.chip.active {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  color: #fff;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.listing-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.lc-image {
  height: 110px;
  background: linear-gradient(135deg, var(--primary-tint), var(--surface-2));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
}
.lc-body { padding: 16px; }
.lc-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.lc-name { font-weight: 800; font-size: 14.5px; }
.lc-cat {
  font-size: 10.5px;
  color: var(--text-soft);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .04em;
  margin-top: 2px;
}
.badge-time {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 9px;
  border-radius: 20px;
  white-space: nowrap;
}
.badge-time.urgent { background: var(--danger-tint); color: var(--danger); }
.badge-time.soon { background: var(--amber-tint); color: var(--amber); }
.badge-time.ok { background: var(--success-tint); color: var(--success); }
.lc-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 10px;
  line-height: 1.6;
}
.lc-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 10px; }
.tag-pill {
  font-size: 10.5px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 5px;
  background: var(--surface-2);
  color: var(--text-muted);
}
.lc-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.lc-donor { font-size: 11.5px; color: var(--text-soft); }
.btn-accent {
  background: var(--accent);
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 13px;
}
.btn-accent:hover { background: #A9781F; }
.btn-sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }
.empty-note {
  font-size: 12.5px;
  color: var(--text-soft);
  padding: 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 12px;
}

@media (max-width: 980px) {
  .card-grid { grid-template-columns: 1fr 1fr; }
}
</style>