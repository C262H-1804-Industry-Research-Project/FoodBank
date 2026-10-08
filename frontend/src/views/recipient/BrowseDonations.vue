<template>
  <section class="panel-section">
    <!-- Recipient's location: listings are sorted and labelled by distance from here -->
    <div class="location-bar">
      <template v-if="showLocationInput">
        <span class="lb-label">📍 Your location</span>
        <AddressAutocomplete
          v-model="locationQuery"
          aria-label="Your location"
          class="location-search"
          types="address,locality,place,postcode"
          placeholder="Enter your address, suburb or postcode to see how far each pickup is"
          @select="applyLocation"
        />
        <button v-if="myLocation" type="button" class="lb-link" @click="cancelLocationChange">Cancel</button>
      </template>
      <template v-else>
        <span class="lb-label">📍 Distances from</span>
        <strong class="lb-address">{{ myLocation.label }}</strong>
        <button type="button" class="lb-link" @click="changeLocation">Change</button>
        <button type="button" class="lb-link" @click="clearLocation">Clear</button>
      </template>
    </div>

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

      <!-- Optionally limit results to the recipient's own suburb -->
      <button
        v-if="mySuburbLabel"
        type="button"
        class="chip"
        :class="{ active: onlyMySuburb }"
        :aria-pressed="onlyMySuburb"
        @click="onlyMySuburb = !onlyMySuburb"
      >
        Only in {{ mySuburbLabel }}
      </button>

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
              📍 {{ item.suburb }}, {{ item.stateRegion }}<span v-if="item.distanceKm != null" class="lc-distance"> · ~ {{ formatDistance(item.distanceKm) }}</span><br>
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
import {
  clearMyLocation, getListingCatalogue, getListingsPage, getMyLocation, getStoredSession, saveMyLocation,
} from '../../api/client'
import { formatDistance, toListingCard } from '../../api/listingAdapter'
import { isAddressLookupEnabled } from '../../api/mapbox'
import AddressAutocomplete from '../../components/AddressAutocomplete.vue'

const emit = defineEmits(['request'])

// Filter states
const filterCategory = ref('')
const searchKeyword = ref('')
const activeTagIds = ref([])
const locationQuery = ref('')
const myLocation = ref(null) // { label, suburb, stateRegion, latitude, longitude }
const isEditingLocation = ref(false)
const onlyMySuburb = ref(false)
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
let locationTimer = null

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
      suburb: onlyMySuburb.value ? myLocation.value?.suburb : '',
      region: onlyMySuburb.value ? myLocation.value?.stateRegion : '',
      latitude: myLocation.value?.latitude,
      longitude: myLocation.value?.longitude,
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

// Only a signed-in recipient has a saved location
const canSaveLocation = getStoredSession()?.user?.role === 'recipient'

function locationLabel({ addressLine, suburb, stateRegion }) {
  const area = [suburb, stateRegion].filter(Boolean).join(', ')
  // For a suburb-only location the address line is just the suburb name
  if (!addressLine || addressLine.toLowerCase() === suburb?.toLowerCase()) return area
  return [addressLine, area].filter(Boolean).join(', ')
}

// Restore the location the recipient entered last time; returns true if one was set
async function restoreSavedLocation() {
  if (!canSaveLocation) return false
  try {
    const saved = await getMyLocation()
    if (!saved) return false
    const location = { addressLine: saved.addressLine, suburb: saved.suburb, stateRegion: saved.region }
    myLocation.value = {
      ...location,
      label: locationLabel(location),
      latitude: saved.latitude ?? undefined,
      longitude: saved.longitude ?? undefined,
    }
    return true
  } catch (error) {
    console.warn('Unable to load saved location:', error.message)
    return false
  }
}

// Load categories and tags on mount 
onMounted(async () => {
  // Setting a restored location reloads the listings through the watcher below
  restoreSavedLocation().then(restored => { if (!restored) loadListings() })
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
  clearTimeout(locationTimer)
})

function toggleTag(tag) {
  const index = activeTagIds.value.indexOf(tag.id)
  if (index > -1) {
    activeTagIds.value.splice(index, 1)
  } else {
    activeTagIds.value.push(tag.id)
  }
}

const mySuburbLabel = computed(() => {
  if (!myLocation.value?.suburb) return ''
  return [myLocation.value.suburb, myLocation.value.stateRegion].filter(Boolean).join(', ')
})

// Without Mapbox there are no suggestions to pick, so keep the text box open
const showLocationInput = computed(() => !myLocation.value || isEditingLocation.value || !isAddressLookupEnabled)

function applyLocation(address) {
  myLocation.value = {
    label: address.label || [address.addressLine, mySuburbLabel.value].filter(Boolean).join(', '),
    addressLine: address.addressLine,
    suburb: address.suburb,
    stateRegion: address.stateRegion,
    latitude: address.latitude,
    longitude: address.longitude,
  }
  if (!myLocation.value.suburb) onlyMySuburb.value = false
  locationQuery.value = ''
  isEditingLocation.value = false
  if (canSaveLocation) {
    saveMyLocation({
      addressLine: address.addressLine,
      suburb: address.suburb,
      region: address.stateRegion,
      latitude: Number.isFinite(address.latitude) ? address.latitude : null,
      longitude: Number.isFinite(address.longitude) ? address.longitude : null,
    }).catch(error => console.warn('Unable to save location:', error.message))
  }
}

function changeLocation() {
  locationQuery.value = ''
  isEditingLocation.value = true
}

function cancelLocationChange() {
  locationQuery.value = ''
  isEditingLocation.value = false
}

function clearLocation() {
  locationQuery.value = ''
  myLocation.value = null
  onlyMySuburb.value = false
  isEditingLocation.value = false
  if (canSaveLocation) {
    clearMyLocation().catch(error => console.warn('Unable to clear saved location:', error.message))
  }
}

// Without Mapbox, the typed text can only be used as a suburb name (no distances)
watch(locationQuery, query => {
  if (isAddressLookupEnabled) return
  clearTimeout(locationTimer)
  locationTimer = setTimeout(() => {
    myLocation.value = query.trim() ? { label: query.trim(), suburb: query.trim(), stateRegion: '' } : null
    onlyMySuburb.value = Boolean(myLocation.value)
  }, 300)
})

function resetFilters() {
  filterCategory.value = ''
  searchKeyword.value = ''
  activeTagIds.value = []
  onlyMySuburb.value = false
}

const hasActiveFilters = computed(() => {
  return (
    filterCategory.value !== '' ||
    searchKeyword.value.trim() !== '' ||
    activeTagIds.value.length > 0 ||
    onlyMySuburb.value
  )
})

// Filter changes go back to page 1; if already there, reload directly
function applyFilters() {
  if (currentPage.value === 1) loadListings()
  else currentPage.value = 1
}

watch([filterCategory, activeTagIds, myLocation, onlyMySuburb], applyFilters, { deep: true })

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
.location-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 12px 16px;
  margin-bottom: 12px;
}
.lb-label { font-size: 13px; font-weight: 700; color: var(--text-muted); }
.lb-address { font-size: 13.5px; color: var(--text); }
.lb-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--primary-dark);
  font-size: 12.5px;
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
}
.location-search { flex: 1; min-width: 260px; }
.location-bar :deep(.address-autocomplete input) {
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
}
.lc-distance { font-weight: 500;}
.filter-bar select, .filter-bar input[type=text], .filter-bar :deep(.address-autocomplete input) {
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