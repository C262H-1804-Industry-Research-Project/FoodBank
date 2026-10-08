<template>
  <section class="panel-section">
    <div class="content-head">
      <div>
        <h2>Food listings management</h2>
        <div class="sub">Review, filter, and moderate active food listings across all donors</div>
      </div>
    </div>

    <!-- 1. Filter Toolbar -->
    <div class="filter-bar">
      <select v-model="filterCategory">
        <option value="">All categories</option>
        <option value="Bakery">Bakery</option>
        <option value="Produce">Produce</option>
        <option value="Dairy">Dairy</option>
        <option value="Pantry">Pantry</option>
      </select>

      <input 
        type="text" 
        v-model="filterLocation" 
        placeholder="📍 Suburb or state (e.g. Adelaide, SA)"
      >

      <select v-model="filterTime">
        <option value="">Any pickup time</option>
        <option value="today">Today</option>
        <option value="tomorrow">Tomorrow</option>
        <option value="weekend">This weekend</option>
      </select>

      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="Search food or donor name…"
      >

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

    <!-- 2. Listings Table (Image column removed) -->
    <div v-if="paginatedListings.length > 0">
      <table>
        <thead>
          <tr>
            <th>Food name</th>
            <th>Donor</th>
            <th>Quantity</th>
            <th>Location</th>
            <th>Status</th>
            <th style="text-align:right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedListings" :key="item.id">
            <td>
              <div class="food-clickable-name" @click="openDetailsModal(item)" title="Click to view details">
                {{ item.name }} <span class="view-link-ic">ℹ️</span>
              </div>
              <div class="food-cat-sub">{{ item.category }}</div>
            </td>
            <td>
              <div class="donor-name">{{ item.donor }}</div>
            </td>
            <td>
              <strong>{{ item.quantity }}</strong>
            </td>
            <td>
              📍 {{ item.suburb }}, {{ item.stateRegion }}
            </td>
            <td>
              <span class="badge-status status-live">Live</span>
            </td>
            <td style="text-align:right;">
              <button 
                class="btn-ban btn-sm" 
                @click="handleBanListing(item)"
              >
                Ban
              </button>
            </td>
          </tr>
        </tbody>
      </table>

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

    <!-- Empty State Note -->
    <div v-else class="empty-note">
      No food listings match your filter criteria.
    </div>

    <!-- Details Modal (Photo & Description) -->
    <div class="modal-backdrop" :class="{ show: showModal }">
      <div class="modal-box" v-if="selectedItem">
        <div class="modal-head">
          <h3>{{ selectedItem.name }}</h3>
          <span class="badge-status status-live">Live</span>
        </div>
        <div class="modal-sub">Listed by {{ selectedItem.donor }} · {{ selectedItem.category }}</div>

        <!-- Food Photo / Graphic Preview -->
        <div class="food-photo-preview">
          <img v-if="selectedItem.photoUrl" :src="selectedItem.photoUrl" alt="Food Photo">
          <div v-else class="photo-placeholder-box">
            <span class="large-emoji">{{ selectedItem.emoji || '🍲' }}</span>
          </div>
        </div>

        <!-- Food Description -->
        <div class="detail-block">
          <label class="detail-label">Description & Storage Instructions</label>
          <div class="detail-desc">
            {{ selectedItem.notes || selectedItem.description || 'No specific storage instructions or description provided by the donor.' }}
          </div>
        </div>

        <!-- Metadata Overview -->
        <div class="detail-grid">
          <div>
            <span class="sub-label">Quantity:</span>
            <strong>{{ selectedItem.quantity }}</strong>
          </div>
          <div>
            <span class="sub-label">Location:</span>
            <span>{{ selectedItem.suburb }}, {{ selectedItem.stateRegion }}</span>
          </div>
        </div>

        <div class="modal-close-row">
          <button class="btn-outline" @click="closeModal">Close</button>
          <button class="btn-ban btn-sm" @click="handleBanListing(selectedItem); closeModal();">
            Ban this listing
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  listings: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['ban-listing'])

const filterCategory = ref('')
const filterLocation = ref('')
const filterTime = ref('')
const searchKeyword = ref('')
const dietaryFilters = ['No pork', 'Nut-free', 'Vegetarian', 'Halal']
const activeTags = ref([])

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

const currentPage = ref(1)
const pageSize = ref(5)

watch([filterCategory, filterLocation, filterTime, searchKeyword, activeTags], () => {
  currentPage.value = 1
})

const totalPages = computed(() => Math.ceil(filteredListings.value.length / pageSize.value) || 1)
const paginationStart = computed(() => filteredListings.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredListings.value.length))
const paginatedListings = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredListings.value.slice(start, start + pageSize.value)
})

// Modal states
const showModal = ref(false)
const selectedItem = ref(null)

function openDetailsModal(item) {
  selectedItem.value = item
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  selectedItem.value = null
}

function handleBanListing(item) {
  const confirmed = window.confirm(`Are you sure you want to BAN and permanently remove "${item.name}" listed by ${item.donor}?`)
  if (confirmed) {
    emit('ban-listing', item.id)
  }
}
</script>

<style scoped>
.content-head { margin-bottom: 20px; }
.content-head h2 { font-size: 22px; }
.content-head .sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

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
  padding: 12px 16px;
  font-weight: 700;
}
tbody td {
  padding: 14px 16px;
  font-size: 13px;
  border-top: 1px solid var(--border);
  color: var(--text);
  vertical-align: middle;
}
tbody tr:hover { background: #FBFAF6; }

.food-clickable-name {
  font-weight: 700;
  color: var(--primary-dark);
  font-size: 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.food-clickable-name:hover {
  text-decoration: underline;
}
.view-link-ic { font-size: 12px; }
.food-cat-sub { font-size: 11px; color: var(--text-soft); text-transform: uppercase; letter-spacing: .04em; margin-top: 2px; }
.donor-name { font-weight: 600; color: var(--text); }

.badge-status {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-live { background: var(--success-tint); color: var(--success); }

.btn-ban {
  background: var(--danger);
  color: #fff;
  border: none;
  padding: 6px 14px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: opacity .15s ease;
}
.btn-ban:hover { opacity: 0.88; }
.btn-sm { padding: 6px 14px; font-size: 12px; border-radius: 6px; }

/* Modal styles */
.modal-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15,20,17,.45);
  z-index: 200;
  align-items: center;
  justify-content: center;
}
.modal-backdrop.show { display: flex; }
.modal-box {
  background: var(--surface);
  border-radius: var(--r-lg);
  width: 480px;
  max-width: 90vw;
  padding: 24px;
  max-height: 85vh;
  overflow-y: auto;
}
.modal-head { display: flex; justify-content: space-between; align-items: center; }
.modal-head h3 { font-size: 18px; margin: 0; }
.modal-sub { font-size: 12.5px; color: var(--text-muted); margin-top: 4px; margin-bottom: 16px; }

.food-photo-preview {
  margin-bottom: 16px;
  border-radius: var(--r-md);
  overflow: hidden;
  border: 1px solid var(--border);
}
.food-photo-preview img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}
.photo-placeholder-box {
  height: 160px;
  background: linear-gradient(135deg, var(--primary-tint), var(--surface-2));
  display: flex;
  align-items: center;
  justify-content: center;
}
.large-emoji { font-size: 54px; }

.detail-block { margin-bottom: 16px; }
.detail-label { font-size: 12px; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px; }
.detail-desc {
  background: var(--surface-2);
  border-radius: var(--r-sm);
  padding: 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  font-size: 12.5px;
  padding: 12px 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  margin-bottom: 18px;
}
.sub-label { color: var(--text-soft); margin-right: 6px; }

.modal-close-row { display: flex; justify-content: flex-end; gap: 10px; }
.btn-outline {
  background: transparent;
  border: 1.5px solid var(--border);
  color: var(--text);
  padding: 8px 16px;
  border-radius: var(--r-sm);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}
.btn-outline:hover { border-color: var(--primary); color: var(--primary-dark); }

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.pagination-info { font-size: 12.5px; color: var(--text-muted); }
.pagination-controls { display: flex; gap: 6px; align-items: center; }
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
.page-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.empty-note {
  font-size: 12.5px;
  color: var(--text-soft);
  padding: 36px 20px;
  text-align: center;
  background: var(--surface-2);
  border-radius: var(--r-md);
  margin-top: 16px;
}
</style>