<template>
  <div class="address-autocomplete">
    <input
      :value="modelValue"
      type="text"
      :placeholder="placeholder"
      :required="required"
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
        v-for="(suggestion, index) in suggestions"
        :key="suggestion.mapbox_id"
        role="option"
        :aria-selected="index === activeIndex"
        :class="{ active: index === activeIndex }"
        @mousedown.prevent="selectSuggestion(suggestion)"
      >
        <span class="suggestion-name">{{ suggestion.name }}</span>
        <span class="suggestion-place">{{ suggestion.place_formatted }}</span>
      </li>
    </ul>
    <p v-if="lookupError" class="lookup-error" role="alert">{{ lookupError }}</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { retrieveAddress, suggestAddresses } from '../api/mapbox'

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  required: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'select'])

const suggestions = ref([])
const activeIndex = ref(-1)
const lookupError = ref('')
const isOpen = computed(() => suggestions.value.length > 0)
let sessionToken = newSessionToken()
let searchTimer = null
let blurTimer = null
let controller = null

// One session covers every /suggest call plus the final /retrieve, so Mapbox bills it once
function newSessionToken() {
  return crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function closeSuggestions() {
  suggestions.value = []
  activeIndex.value = -1
}

function handleInput(event) {
  const query = event.target.value
  emit('update:modelValue', query)
  lookupError.value = ''
  clearTimeout(searchTimer)
  controller?.abort()
  if (query.trim().length < 3) {
    closeSuggestions()
    return
  }
  // Wait for a pause in typing to stay well under the 10 requests/second limit
  searchTimer = setTimeout(() => loadSuggestions(query), 250)
}

async function loadSuggestions(query) {
  controller = new AbortController()
  const { signal } = controller
  try {
    const results = await suggestAddresses(query, sessionToken, signal)
    if (signal.aborted) return
    suggestions.value = results
    activeIndex.value = -1
  } catch (error) {
    if (signal.aborted) return
    closeSuggestions()
    lookupError.value = error.message
  }
}

async function selectSuggestion(suggestion) {
  closeSuggestions()
  let address
  try {
    address = await retrieveAddress(suggestion, sessionToken)
  } catch (error) {
    lookupError.value = error.message
    return
  } finally {
    sessionToken = newSessionToken()
  }
  emit('update:modelValue', address.addressLine)
  emit('select', address)
}

function handleKeydown(event) {
  if (!isOpen.value) return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % suggestions.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = activeIndex.value <= 0 ? suggestions.value.length - 1 : activeIndex.value - 1
  } else if (event.key === 'Enter' && activeIndex.value >= 0) {
    event.preventDefault()
    selectSuggestion(suggestions.value[activeIndex.value])
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
.address-autocomplete { position: relative; }
.address-autocomplete input { width: 100%; }
.suggestion-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
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
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
}
.suggestion-list li.active, .suggestion-list li:hover { background: var(--surface-2); }
.suggestion-name { font-size: 13px; font-weight: 600; color: var(--text); }
.suggestion-place { font-size: 11.5px; color: var(--text-muted); }
.lookup-error { color: var(--danger); font-size: 12px; margin: 6px 0 0; }
</style>
