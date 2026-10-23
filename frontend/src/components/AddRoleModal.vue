<template>
  <div class="role-modal-backdrop">
    <div v-if="mode === 'choice'" class="role-modal role-confirmation">
      <div class="role-confirmation-icon">!</div>
      <h2>No {{ roleLabel }} profile</h2>
      <p class="role-modal-intro">
        Your account does not currently have a {{ roleLabel }} profile, so you cannot switch to this role yet.
      </p>
      <p class="role-confirmation-question">
        Create a new profile, or link a {{ roleLabel }} account you already own.
      </p>

      <div class="role-modal-actions">
        <button type="button" class="cancel-button" @click="$emit('cancel')">
          Not now
        </button>
        <button type="button" class="create-button" @click="mode = 'create'">
          Register as {{ roleLabel }}
        </button>
        <button type="button" class="create-button" @click="mode = 'link'">
          Link existing {{ roleLabel }} account
        </button>
      </div>
    </div>

    <form v-else-if="mode === 'link'" class="role-modal" @submit.prevent="submitLink">
      <h2>Link existing {{ roleLabel }} account</h2>
      <p class="role-modal-intro">
        Enter that account's email and password once. After linking, you can switch without entering them again. The email can be different from your current account.
      </p>
      <div class="field">
        <label for="linked-account-email">{{ roleLabel }} account email</label>
        <input id="linked-account-email" v-model="credentials.email" type="email" autocomplete="username" :disabled="pending" required>
      </div>
      <div class="field">
        <label for="linked-account-password">{{ roleLabel }} account password</label>
        <input id="linked-account-password" v-model="credentials.password" type="password" autocomplete="current-password" :disabled="pending" required>
      </div>
      <p v-if="error" class="role-modal-error" role="alert">{{ error }}</p>
      <div class="role-modal-actions">
        <button type="button" class="cancel-button" :disabled="pending" @click="backToChoice">Back</button>
        <button type="submit" class="create-button" :disabled="pending">
          {{ pending ? 'Linking…' : 'Link account and switch' }}
        </button>
      </div>
    </form>

    <form v-else class="role-modal" @submit.prevent="submitForm">
      <h2>Create {{ roleLabel }} profile</h2>
      <p class="role-modal-intro">
        Your existing email, password and phone number will be reused.
      </p>

      <template v-if="targetRole === 'recipient'">
        <div class="field">
          <label>Your name</label>
          <input v-model="form.recipientName" type="text" required>
        </div>
        <div class="field">
          <label>Organisation name <span>(optional)</span></label>
          <input v-model="form.recipientOrgName" type="text">
        </div>
      </template>

      <template v-else>
        <div class="field">
          <label>Donor organisation name</label>
          <input v-model="form.donorOrgName" type="text" required>
        </div>
        <div class="field">
          <label>Pickup address</label>
          <AddressAutocomplete
            v-model="form.addressLine"
            placeholder="Start typing the street address"
            required
            @select="applyAddress"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Suburb</label>
            <input v-model="form.suburb" type="text" required>
          </div>
          <div class="field">
            <label>State / territory</label>
            <select v-model="form.region" required>
              <option value="" disabled>Select state</option>
              <option v-for="state in australianStates" :key="state" :value="state">
                {{ state }}
              </option>
            </select>
          </div>
        </div>
      </template>

      <p v-if="error" class="role-modal-error">{{ error }}</p>

      <div class="role-modal-actions">
        <button type="button" class="cancel-button" :disabled="pending" @click="backToChoice">
          Back
        </button>
        <button type="submit" class="create-button" :disabled="pending">
          {{ pending ? 'Creating…' : `Create ${roleLabel} profile` }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AddressAutocomplete from './AddressAutocomplete.vue'

const props = defineProps({
  targetRole: { type: String, required: true },
  pending: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['submit', 'cancel'])
const australianStates = ['ACT', 'NSW', 'NT', 'QLD', 'SA', 'TAS', 'VIC', 'WA']
const roleLabel = computed(() => props.targetRole === 'donor' ? 'Donor' : 'Recipient')
const mode = ref('choice')
const credentials = reactive({ email: '', password: '' })

function backToChoice() {
  credentials.password = ''
  mode.value = 'choice'
}

function submitLink() {
  emit('submit', {
    mode: 'link',
    targetRole: props.targetRole,
    email: credentials.email.trim(),
    password: credentials.password,
  })
  credentials.password = ''
}

const form = reactive({
  donorOrgName: '',
  addressLine: '',
  suburb: '',
  region: '',
  recipientName: '',
  recipientOrgName: '',
})

function applyAddress(address) {
  if (address.suburb) form.suburb = address.suburb
  if (address.stateRegion) form.region = address.stateRegion
}

function submitForm() {
  emit('submit', {
    targetRole: props.targetRole,
    donorOrgName: form.donorOrgName,
    addressLine: form.addressLine,
    suburb: form.suburb,
    region: form.region,
    recipientName: form.recipientName,
    recipientOrgName: form.recipientOrgName,
  })
}
</script>

<style scoped>
.role-modal-backdrop { position: fixed; inset: 0; z-index: 500; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(15, 20, 17, .5); }
.role-modal { width: min(520px, 100%); max-height: 90vh; overflow-y: auto; padding: 26px; border-radius: 14px; background: var(--surface); box-shadow: 0 18px 50px rgba(0, 0, 0, .2); }
.role-modal h2 { margin: 0 0 6px; font-size: 21px; }
.role-modal-intro { margin: 0 0 20px; color: var(--text-muted); font-size: 13px; }
.role-confirmation { width: min(640px, 100%); text-align: center; }
.role-confirmation-icon { display: grid; width: 46px; height: 46px; margin: 0 auto 14px; place-items: center; border-radius: 50%; background: #f4ead2; color: #8a6514; font-size: 24px; font-weight: 800; }
.role-confirmation-question { margin: 0; color: var(--text); font-size: 14px; font-weight: 700; }
.role-confirmation .role-modal-actions { flex-wrap: nowrap; justify-content: center; }
.role-confirmation .role-modal-actions button { flex-shrink: 0; white-space: nowrap; }
@media (max-width: 600px) {
  .role-confirmation .role-modal-actions { justify-content: flex-start; overflow-x: auto; }
}
.field { flex: 1; margin-bottom: 15px; }
.field-row { display: flex; gap: 12px; }
.field label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 700; }
.field label span { color: var(--text-soft); font-weight: 500; }
.field input, .field select { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; background: #fff; font-size: 14px; }
.role-modal-error { color: #a33; font-size: 13px; }
.role-modal-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.cancel-button, .create-button { padding: 9px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; }
.cancel-button { border: 1px solid var(--border); background: #fff; color: var(--text); }
.create-button { border: 1px solid var(--primary); background: var(--primary); color: #fff; }
.cancel-button:disabled, .create-button:disabled { cursor: not-allowed; opacity: .6; }
</style>
