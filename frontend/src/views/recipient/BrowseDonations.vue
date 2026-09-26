<template>
  <section class="panel-section">
    <!-- 1. Filter Toolbar -->
    <div class="filter-bar">
      <!-- Category Filter -->
      <!-- <select v-model="filterCategory">
        <option value="">All categories</option>
        <option value="Bakery">Bakery</option>
        <option value="Produce">Produce</option>
        <option value="Dairy">Dairy</option>
        <option value="Pantry">Pantry</option>
      </select> -->

      <select v-model="filterCategory" :disabled="isLoadingCatalogue">
        <option value="">All categories</option>
        <option v-for="category in categories" :key="category.id" :disabled="isLoadingCatalogue">
          {{ category.name }}
        </option>
      </select>

      <!-- Suburb or Region Search -->
      <input 
        type="text" 
        v-model="filterLocation" 
        placeholder="📍 Suburb or state (e.g. Adelaide, SA)"
      >

      <!-- Pickup Time Filter -->
      <select v-model="filterTime">
        <option value="">Any pickup time</option>
        <option value="today">Today</option>
        <option value="tomorrow">Tomorrow</option>
        <option value="weekend">This weekend</option>
      </select>

      <!-- Food Name Keyword Search -->
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search food name…"
      >

      <!-- Dietary Tag Filters -->
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

      <button 
        v-if="hasActiveFilters" 
        type="button" 
        class="btn-reset" 
        @click="resetFilters"
      >
        Reset filters
      </button>
    </div>

    <!-- 2. Donation Cards Grid -->
    <div v-if="paginatedListings.length > 0">
      <div class="card-grid">
        <div
          v-for="item in paginatedListings"
          :key="item.id"
          class="listing-card"
        >
          <div class="lc-image">
            <img v-if="item.photoUrl" :src="item.photoUrl" :alt="item.name" loading="lazy">
            <span v-else>{{ item.emoji || '🍲' }}</span>
          </div>
          <div class="lc-body">
            <div class="lc-top">
              <div>
                <div class="lc-name">{{ item.name }}</div>
                <div class="lc-cat">{{ item.category }}</div>
              </div>
              <span class="badge-time" :class="item.urgencyClass || 'ok'">{{ item.timeLeft || 'Available' }}</span>
            </div>

            <!-- Address shown as Suburb + State/Region -->
            <div class="lc-meta">
              📍 {{ item.suburb }}, {{ item.stateRegion }}<br>
              🕐 {{ item.pickupWindow || item.slots || 'Pickup scheduled with donor' }}
            </div>

            <!-- Dietary tags -->
            <div v-if="item.tags && item.tags.length" class="lc-tags">
              <span v-for="t in item.tags" :key="t" class="tag-pill">{{ t }}</span>
            </div>
          </div>

          <div class="lc-foot">
            <span class="lc-donor">{{ item.donor || 'Community Donor' }}</span>
            <button class="btn-accent btn-sm" @click="emit('request', item)">Request</button>
          </div>
        </div>
      </div>

      <!-- 3. Bottom Pagination System -->
      <div class="pagination-bar">
        <div class="pagination-info">
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ filteredListings.length }}</strong> listings
        </div>
        <div class="pagination-controls">
          <button 
            class="page-btn" 
            :disabled="currentPage === 1" 
            @click="currentPage--"
          >
            ‹ Prev
          </button>
          
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button 
            class="page-btn" 
            :disabled="currentPage === totalPages" 
            @click="currentPage++"
          >
            Next ›
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-note">
      No surplus food matches all selected criteria. Try adjusting or resetting filters.
    </div>
  </section>
</template>

<script setup>

import { onMounted, ref, computed, watch } from 'vue'
import { getListingCatalogue } from '../../api/client'


const props = defineProps({
  listings: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['request'])

// Filter states
const filterCategory = ref('')
const filterLocation = ref('')
const filterTime = ref('')
const searchKeyword = ref('')
const dietaryFilters = ['No pork', 'Nut-free', 'Vegetarian', 'Halal']
const activeTags = ref([])
const isLoadingCatalogue = ref(true)
const catalogueError = ref('')
const categories = ref([])
const tags = ref([])


onMounted(async () => {
  try {
    const catalogue = await getListingCatalogue()
    categories.value = catalogue.categories
    tags.value = catalogue.tags
    filterCategory = categories.value[0]?.id ?? null

    
  } catch (error) {
    catalogueError.value = error.message || 'Unable to load listing categories and tags.'
  } finally {
    isLoadingCatalogue.value = false
  }
})

function toggleTag(tag) {
  const index = activeTags.value.indexOf(tag)
  if (index > -1) {
    activeTags.value.splice(index, 1)
  } else {
    activeTags.value.push(tag)
  }
}

function resetFilters() {
  filterCategory.value = ''
  filterLocation.value = ''
  filterTime.value = ''
  searchKeyword.value = ''
  activeTags.value = []
}

const hasActiveFilters = computed(() => {
  return (
    filterCategory.value !== '' ||
    filterLocation.value.trim() !== '' ||
    filterTime.value !== '' ||
    searchKeyword.value.trim() !== '' ||
    activeTags.value.length > 0
  )
})

// Filter matching logic
const filteredListings = computed(() => {
  return props.listings.filter(item => {
    if (filterCategory.value && item.category !== filterCategory.value) {
      return false
    }

    if (filterLocation.value.trim()) {
      const qLoc = filterLocation.value.trim().toLowerCase()
      const suburbMatch = (item.suburb || '').toLowerCase().includes(qLoc)
      const stateMatch = (item.stateRegion || '').toLowerCase().includes(qLoc)
      if (!suburbMatch && !stateMatch) {
        return false
      }
    }

    if (filterTime.value) {
      if (filterTime.value === 'today' && item.timeType !== 'today') return false
      if (filterTime.value === 'tomorrow' && item.timeType !== 'tomorrow') return false
      if (filterTime.value === 'weekend' && item.timeType !== 'weekend') return false
    }

    if (searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const nameMatch = (item.name || '').toLowerCase().includes(q)
      const donorMatch = (item.donor || '').toLowerCase().includes(q)
      if (!nameMatch && !donorMatch) {
        return false
      }
    }

    if (activeTags.value.length > 0) {
      const hasAllTags = activeTags.value.every(tag => (item.tags || []).includes(tag))
      if (!hasAllTags) {
        return false
      }
    }

    return true
  })
})

// Pagination logic
const currentPage = ref(1)
const pageSize = ref(6)

watch([filterCategory, filterLocation, filterTime, searchKeyword, activeTags], () => {
  currentPage.value = 1
})

const totalPages = computed(() => {
  return Math.ceil(filteredListings.value.length / pageSize.value) || 1
})

const paginationStart = computed(() => {
  if (filteredListings.value.length === 0) return 0
  return (currentPage.value - 1) * pageSize.value + 1
})

const paginationEnd = computed(() => {
  return Math.min(currentPage.value * pageSize.value, filteredListings.value.length)
})

const paginatedListings = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredListings.value.slice(start, start + pageSize.value)
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
  transition: all .15s ease;
}
.chip.active {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  color: #fff;
}
.btn-reset {
  background: transparent;
  border: 1px dashed var(--danger);
  color: var(--danger);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  margin-left: auto;
}
.btn-reset:hover { background: var(--danger-tint); }

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
  height: 160px;
  background: linear-gradient(135deg, var(--primary-tint), var(--surface-2));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
}
.lc-image img { width: 100%; height: 100%; object-fit: cover; }
.lc-body { padding: 16px; flex: 1; display: flex; flex-direction: column; }
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
  margin-top: 8px;
  line-height: 1.5;
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
  cursor: pointer;
}
.btn-accent:hover { background: #A9781F; }
.btn-sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }

/* Bottom Pagination System */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
}
.pagination-info {
  font-size: 12.5px;
  color: var(--text-muted);
}
.pagination-controls {
  display: flex;
  gap: 6px;
  align-items: center;
}
.page-btn, .page-number-btn {
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text);
  border-radius: var(--r-sm);
  padding: 6px 12px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all .12s ease;
}
.page-btn:hover:not(:disabled), .page-number-btn:hover:not(.active) {
  border-color: var(--primary);
  color: var(--primary-dark);
}
.page-number-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.empty-note {
  font-size: 12.5px;
  color: var(--text-soft);
  padding: 36px 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 12px;
}

@media (max-width: 980px) {
  .card-grid { grid-template-columns: 1fr 1fr; }
}
</style>