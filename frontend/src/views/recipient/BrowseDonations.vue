<template>
  <section class="panel-section">
    <!-- 1. Filter Toolbar -->
    <div class="filter-bar">
      <!-- Category Filter -->
      <select v-model="filterCategory" :disabled="isLoadingCatalogue">
        <option value="">All categories</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">
          {{ category.name }}
        </option>
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
          v-for="tag in tags"
          :key="tag.id"
          class="chip"
          :class="{ active: activeTagIds.includes(tag.id) }"
          @click="toggleTag(tag)"
        >
          {{ tag.name }}
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

    <p v-if="catalogueError" class="load-error" role="alert">{{ catalogueError }}</p>
    <p v-if="listingsError" class="load-error" role="alert">{{ listingsError }}</p>

    <!-- 2. Donation Cards Grid -->
    <div v-if="listings.length > 0" :class="{ 'is-loading': isLoadingListings }">
      <div class="card-grid">
        <div
          v-for="item in listings"
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
          Showing <strong>{{ paginationStart }}</strong>–<strong>{{ paginationEnd }}</strong> of <strong>{{ totalElements }}</strong> listings
        </div>
        <div class="pagination-controls">
          <button 
            class="page-btn" 
            :disabled="currentPage === 1 || isLoadingListings" 
            @click="currentPage--"
          >
            ‹ Prev
          </button>
          
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-number-btn"
            :class="{ active: currentPage === page }"
            :disabled="isLoadingListings"
            @click="currentPage = page"
          >
            {{ page }}
          </button>

          <button 
            class="page-btn" 
            :disabled="currentPage === totalPages || isLoadingListings" 
            @click="currentPage++"
          >
            Next ›
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="isLoadingListings" class="empty-note">Loading donations…</div>

    <!-- Empty State -->
    <div v-else-if="!listingsError" class="empty-note">
      No surplus food matches all selected criteria. Try adjusting or resetting filters.
    </div>
  </section>
</template>

<script setup>

import { onBeforeUnmount, onMounted, ref, computed, watch } from 'vue'
import { getListingCatalogue, getListingsPage } from '../../api/client'
import { toListingCard } from '../../api/listingAdapter'

const emit = defineEmits(['request'])

// Filter states
const filterCategory = ref('')
const searchKeyword = ref('')
const activeTagIds = ref([])
const isLoadingCatalogue = ref(true)
const catalogueError = ref('')
const categories = ref([])
const tags = ref([])

// Current page of listings from the backend
const listings = ref([])
const totalElements = ref(0)
const totalPages = ref(1)
const isLoadingListings = ref(false)
const listingsError = ref('')
const currentPage = ref(1)
const pageSize = 6
let listingsController = null
let searchTimer = null

async function loadListings() {
  listingsController?.abort()
  const controller = new AbortController()
  listingsController = controller
  isLoadingListings.value = true
  listingsError.value = ''
  try {
    const result = await getListingsPage({
      page: currentPage.value - 1,
      size: pageSize,
      foodName: searchKeyword.value,
      categoryId: filterCategory.value,
      tagIds: activeTagIds.value,
    }, controller.signal)
    if (controller.signal.aborted) return
    // The requested page no longer exists (e.g. listings were removed); go to the last one
    if (result.items.length === 0 && result.totalPages > 0 && currentPage.value > result.totalPages) {
      currentPage.value = result.totalPages
      return
    }
    listings.value = result.items.map(row => toListingCard(row))
    totalElements.value = result.totalElements
    totalPages.value = Math.max(result.totalPages, 1)
  } catch (error) {
    if (controller.signal.aborted) return
    listings.value = []
    totalElements.value = 0
    totalPages.value = 1
    listingsError.value = error.message || 'Unable to load donations.'
  } finally {
    if (listingsController === controller) isLoadingListings.value = false
  }
}

// Load categories and tags on mount 
onMounted(async () => {
  loadListings()
  try {
    const catalogue = await getListingCatalogue()
    categories.value = catalogue.categories
    tags.value = catalogue.tags
  } catch (error) {
    catalogueError.value = error.message || 'Unable to load listing categories and tags.'
  } finally {
    isLoadingCatalogue.value = false
  }
})

onBeforeUnmount(() => {
  listingsController?.abort()
  clearTimeout(searchTimer)
})

function toggleTag(tag) {
  const index = activeTagIds.value.indexOf(tag.id)
  if (index > -1) {
    activeTagIds.value.splice(index, 1)
  } else {
    activeTagIds.value.push(tag.id)
  }
}

function resetFilters() {
  filterCategory.value = ''
  searchKeyword.value = ''
  activeTagIds.value = []
}

const hasActiveFilters = computed(() => {
  return (
    filterCategory.value !== '' ||
    searchKeyword.value.trim() !== '' ||
    activeTagIds.value.length > 0
  )
})

// Filter changes go back to page 1; if already there, reload directly
function applyFilters() {
  if (currentPage.value === 1) loadListings()
  else currentPage.value = 1
}

watch([filterCategory, activeTagIds], applyFilters, { deep: true })

// Wait for the user to stop typing before searching
watch(searchKeyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(applyFilters, 300)
})

watch(currentPage, loadListings)

const paginationStart = computed(() => {
  if (totalElements.value === 0) return 0
  return (currentPage.value - 1) * pageSize + 1
})

const paginationEnd = computed(() => {
  return Math.min(currentPage.value * pageSize, totalElements.value)
})

// Reload the current page, e.g. after a listing has been requested
defineExpose({ reload: loadListings })
</script>

<style scoped>
.load-error { color: var(--danger); font-size: 13px; margin: 0 0 14px; }
.is-loading { opacity: .6; transition: opacity .15s ease; }
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