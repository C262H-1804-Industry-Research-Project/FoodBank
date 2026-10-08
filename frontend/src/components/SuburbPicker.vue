<template>
  <div class="suburb-picker">
    
    <div class="suburb-search">
      <input
        v-model="query"
        type="text"
        :placeholder="modelValue.length ? 'Add suburb…' : 'Filter by suburb…'"
        aria-label="Filter by suburb"
        autocomplete="off"
        role="combobox"
        aria-autocomplete="list"
        :aria-expanded="isOpen"
        @input="handleInput"
        @keydown="handleKeydown"
        @blur="closeSoon"
      >
      <ul v-if="isOpen" class="suggestion-list" role="listbox">
        <li
          v-for="(suggestion, index) in visibleSuggestions"
          :key="keyOf(suggestion)"
          role="option"
          :aria-selected="index === activeIndex"
          :class="{ active: index === activeIndex }"
          @mousedown.prevent="add(suggestion)"
        >
          {{ labelOf(suggestion) }}
        </li>
      </ul>
    </div>

    <span v-for="item in modelValue" :key="keyOf(item)" class="suburb-pill">
      {{ labelOf(item) }}
      <button type="button" class="pill-remove" :aria-label="`Remove ${labelOf(item)}`" @click="remove(item)">×</button>
    </span>
    
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { isAddressLookupEnabled, suggestAddresses, toPickupAddress } from '../api/mapbox'

// Selected suburbs as { suburb, region }; region is empty when a typed name matches any state
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

const query = ref('')
const suggestions = ref([])
const activeIndex = ref(-1)
let sessionToken = newSessionToken()
let searchTimer = null
let blurTimer = null
let controller = null

function newSessionToken() {
  return crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const keyOf = item => `${item.suburb.toLowerCase()}|${(item.region || '').toLowerCase()}`
const labelOf = item => [item.suburb, item.region].filter(Boolean).join(', ')

const visibleSuggestions = computed(() => {
  const selected = new Set(props.modelValue.map(keyOf))
  return suggestions.value.filter(suggestion => !selected.has(keyOf(suggestion)))
})
const isOpen = computed(() => visibleSuggestions.value.length > 0)

function closeSuggestions() {
  suggestions.value = []
  activeIndex.value = -1
}

function handleInput() {
  clearTimeout(searchTimer)
  controller?.abort()
  if (!isAddressLookupEnabled || query.value.trim().length < 2) {
    closeSuggestions()
    return
  }
  // Wait for a pause in typing to stay well under Mapbox's rate limit
  searchTimer = setTimeout(loadSuggestions, 250)
}

async function loadSuggestions() {
  controller = new AbortController()
  const { signal } = controller
  try {
    const results = await suggestAddresses(query.value.trim(), sessionToken, signal, 'locality,place')
    if (signal.aborted) return
    // The suggestion already carries the suburb name and state, so no /retrieve call is needed
    const unique = new Map()
    for (const result of results) {
      const { suburb, stateRegion } = toPickupAddress(result.name, result.context, result.feature_type)
      if (!suburb) continue
      const item = { suburb, region: stateRegion }
      unique.set(keyOf(item), item)
    }
    suggestions.value = [...unique.values()]
    activeIndex.value = -1
  } catch {
    if (signal.aborted) return
    closeSuggestions()
  }
}

function add(item) {
  if (!props.modelValue.some(selected => keyOf(selected) === keyOf(item))) {
    emit('update:modelValue', [...props.modelValue, item])
  }
  query.value = ''
  closeSuggestions()
  sessionToken = newSessionToken()
}

function remove(item) {
  emit('update:modelValue', props.modelValue.filter(selected => keyOf(selected) !== keyOf(item)))
}

function handleKeydown(event) {
  // Backspace in an empty box removes the last pill
  if (event.key === 'Backspace' && query.value === '' && props.modelValue.length) {
    remove(props.modelValue[props.modelValue.length - 1])
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    if (isOpen.value) {
      add(visibleSuggestions.value[Math.max(activeIndex.value, 0)])
    } else if (!isAddressLookupEnabled && query.value.trim()) {
      // Without Mapbox, use the typed name as-is (matches that suburb in any state)
      add({ suburb: query.value.trim(), region: '' })
    }
    return
  }
  if (!isOpen.value) return
  const count = visibleSuggestions.value.length
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % count
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = activeIndex.value <= 0 ? count - 1 : activeIndex.value - 1
  } else if (event.key === 'Escape') {
    closeSuggestions()
  }
}

function closeSoon() {
  clearTimeout(blurTimer)
  blurTimer = setTimeout(closeSuggestions, 150)
}

onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  clearTimeout(blurTimer)
  controller?.abort()
})
</script>

<style scoped>
.suburb-picker { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.suburb-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 6px 5px 12px;
  border-radius: 20px;
  background: var(--primary-dark);
  border: 1.3px solid var(--primary-dark);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.pill-remove {
  background: none;
  border: none;
  color: inherit;
  font-size: 15px;
  line-height: 1;
  padding: 0 4px;
  cursor: pointer;
  opacity: .8;
}
.pill-remove:hover { opacity: 1; }
.suburb-search { position: relative; }
.suburb-search input {
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  font-size: 13px;
  min-width: 170px;
}
.suggestion-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  min-width: 100%;
  z-index: 20;
  margin: 0;
  padding: 4px;
  list-style: none;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  box-shadow: 0 8px 20px rgba(0, 0, 0, .12);
  max-height: 260px;
  overflow-y: auto;
}
.suggestion-list li {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  cursor: pointer;
}
.suggestion-list li.active, .suggestion-list li:hover { background: var(--surface-2); }
</style>
