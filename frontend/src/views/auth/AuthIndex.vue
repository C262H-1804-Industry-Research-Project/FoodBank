<template>
  <div class="login-wrapper">
    <!-- Left branding banner -->
    <div class="login-brand">
      <div>
        <div class="brand-mark">Second Table</div>
        <div class="brand-tagline">Surplus food, matched with someone who needs it — before it expires.</div>
      </div>
      <div class="status-track">
        <div class="st-title">Every donation's journey</div>
        <div class="st-item on"><div class="dot"></div><span>Available</span></div>
        <div class="st-item"><div class="dot"></div><span>Requested</span></div>
        <div class="st-item"><div class="dot"></div><span>Reserved</span></div>
        <div class="st-item"><div class="dot"></div><span>Collected</span></div>
      </div>
      <div class="brand-foot">
        A coordination tool for the South Australian Food Rescue Network. Donors list surplus food; approved recipients reserve and collect it.
      </div>
    </div>

    <!-- Right interaction panel -->
    <div class="login-panel">
      <div class="login-card">
        <!-- Top Auth tabs switch -->
        <div class="auth-tabs">
          <button :class="{ active: currentTab === 'signin' }" @click="switchTab('signin')">Log in</button>
          <button :class="{ active: currentTab === 'signup' }" @click="switchTab('signup')">Create account</button>
        </div>

        <!-- 1. LOGIN FORM -->
        <form v-if="currentTab === 'signin'" @submit.prevent="handleLogin">
          <div class="field">
            <label class="field-label">Log in as</label>
            <div class="role-button-group">
              <button
                type="button"
                class="role-select-btn"
                :class="{ active: loginRole === 'recipient' }"
                @click="loginRole = 'recipient'"
              >
                <div class="role-icon">🤲</div>
                <div class="role-btn-title">Recipient</div>
                <div class="role-btn-desc">Collect surplus food</div>
              </button>
              <button
                type="button"
                class="role-select-btn"
                :class="{ active: loginRole === 'donor' }"
                @click="loginRole = 'donor'"
              >
                <div class="role-icon">🍲</div>
                <div class="role-btn-title">Donor</div>
                <div class="role-btn-desc">Donate surplus food</div>
              </button>
            </div>
          </div>

          <div class="field">
            <label class="field-label">Email</label>
            <input 
              v-model="loginForm.account" 
              type="email" 
              placeholder="you@organisation.org" 
              required
            >
          </div>
          <div class="field">
            <label class="field-label">Password</label>
            <input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="••••••••" 
              required
            >
          </div>

          <button type="submit" class="btn-primary">
            Log in as {{ loginRole === 'donor' ? 'Donor' : 'Recipient' }}
          </button>
          <span class="link-muted">Forgot your password? <a href="#">Reset it</a></span>
          
          <div class="form-note">
            Selected role: <strong>{{ loginRole.toUpperCase() }}</strong>. Submitting will direct you to the corresponding portal.
          </div>
        </form>

        <!-- 2. MULTI-STEP REGISTER FORM -->
        <div v-else>
          <!-- Step 1: Select Role via Left/Right Buttons -->
          <div v-if="signupStep === 1">
            <div class="field">
              <label class="field-label">I'm signing up as</label>
              <div class="role-button-group">
                <button
                  type="button"
                  class="role-select-btn"
                  :class="{ active: signupForm.role === 'recipient' }"
                  @click="signupForm.role = 'recipient'"
                >
                  <div class="role-icon">🤲</div>
                  <div class="role-btn-title">Recipient</div>
                  <div class="role-btn-desc">I collect food for community</div>
                </button>
                <button
                  type="button"
                  class="role-select-btn"
                  :class="{ active: signupForm.role === 'donor' }"
                  @click="signupForm.role = 'donor'"
                >
                  <div class="role-icon">🍲</div>
                  <div class="role-btn-title">Donor</div>
                  <div class="role-btn-desc">I have surplus food to give</div>
                </button>
              </div>
            </div>

            <button 
              type="button" 
              class="btn-primary" 
              :disabled="!signupForm.role"
              @click="signupStep = 2"
            >
              Next step →
            </button>

            <div class="form-note">
              Select whether you are donating or receiving food. You will configure your organisation profile next.
            </div>
          </div>

          <!-- Step 2: Fill in details for selected role -->
          <form v-else-if="signupStep === 2" @submit.prevent="handleRegister">
            <div class="step-header">
              <span class="role-tag">Role: {{ signupForm.role.toUpperCase() }}</span>
              <button type="button" class="btn-text-back" @click="signupStep = 1">← Change role</button>
            </div>

            <!-- Donor specific fields with 3-part address -->
            <div v-if="signupForm.role === 'donor'" class="role-fields">
              <div class="rf-title">Donor details</div>
              <div class="field">
                <label class="field-label">Organisation name</label>
                <input v-model="signupForm.donorOrg" type="text" placeholder="Green Leaf Café" required>
              </div>

              <!-- 3-Part Address Breakdown -->
              <div class="address-fieldset">
                <label class="field-label">Organisation Address</label>
                <div class="field">
                  <input v-model="signupForm.donorStreet" type="text" placeholder="Street address & unit (e.g. 14 Rundle St)" required>
                </div>
                <div class="field-row">
                  <div class="field">
                    <input v-model="signupForm.donorCityPostcode" type="text" placeholder="City / Suburb, Postcode (e.g. Adelaide, 5000)" required>
                  </div>
                  <div class="field">
                    <input v-model="signupForm.donorCountry" type="text" placeholder="Country (e.g. Australia)" required>
                  </div>
                </div>
                <div class="field-hint">Your exact pickup address can be adjusted per listing.</div>
              </div>
            </div>

            <!-- Recipient specific fields -->
            <div v-if="signupForm.role === 'recipient'" class="role-fields">
              <div class="rf-title">Recipient details</div>
              <div class="field">
                <label class="field-label">Your name</label>
                <input v-model="signupForm.recipientName" type="text" placeholder="Priya Nair" required>
              </div>
              <div class="field">
                <label class="field-label">Organisation name <span class="opt-label">(optional)</span></label>
                <input v-model="signupForm.recipientOrg" type="text" placeholder="Community Kitchen East">
              </div>
              <div class="field">
                <label class="field-label">Phone number</label>
                <input v-model="signupForm.phone" type="text" placeholder="04xx xxx xxx" required>
              </div>
            </div>

            <!-- Common account credentials -->
            <div class="field">
              <label class="field-label">Email</label>
              <input v-model="signupForm.email" type="email" placeholder="you@organisation.org" required>
            </div>
            <div class="field">
              <label class="field-label">Password</label>
              <input v-model="signupForm.password" type="password" placeholder="Create a password" required>
            </div>

            <button type="submit" class="btn-primary">Create account</button>
            <div class="form-note">
              Organisation accounts are reviewed by an admin before they can publish or request donations.
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentTab = ref('signin')
const loginRole = ref('recipient')
const signupStep = ref(1)

function switchTab(tab) {
  currentTab.value = tab
  if (tab === 'signup') {
    signupStep.value = 1
  }
}

const loginForm = reactive({
  account: '',
  password: ''
})

const signupForm = reactive({
  role: 'recipient',
  donorOrg: '',
  donorStreet: '',
  donorCityPostcode: '',
  donorCountry: 'Australia',
  recipientName: '',
  recipientOrg: '',
  phone: '',
  email: '',
  password: ''
})

function handleLogin() {
  if (loginRole.value === 'donor') {
    router.push('/donor')
  } else {
    router.push('/recipient')
  }
}

function handleRegister() {
  if (signupForm.role === 'donor') {
    router.push('/donor')
  } else {
    router.push('/recipient')
  }
}
</script>

<style scoped>
.login-wrapper { display: flex; min-height: 100vh; }
.login-brand { flex: 0 0 40%; background: linear-gradient(160deg, var(--primary-dark), var(--primary)); color: #EFF5F1; padding: 64px 56px; display: flex; flex-direction: column; justify-content: space-between; }
.brand-mark { font-family: var(--font-serif); font-size: 26px; font-weight: 600; letter-spacing: .01em; }
.brand-tagline { font-family: var(--font-serif); font-size: 28px; line-height: 1.35; max-width: 380px; margin-top: 28px; font-weight: 500; color: #F4F8F5; }
.status-track { margin-top: 40px; }
.status-track .st-title { font-size: 12px; text-transform: uppercase; letter-spacing: .08em; color: #9FC0AF; margin-bottom: 18px; font-weight: 700; }
.st-item { display: flex; align-items: center; gap: 14px; padding: 9px 0; position: relative; }
.st-item .dot { width: 10px; height: 10px; border-radius: 50%; background: #4C7A65; flex: 0 0 10px; z-index: 1; }
.st-item.on .dot { background: var(--accent); box-shadow: 0 0 0 4px rgba(198,143,46,.25); }
.st-item:not(:last-child)::after { content: ''; position: absolute; left: 4.5px; top: 24px; width: 1px; height: 26px; background: #3A564C; }
.st-item span { font-size: 14px; color: #CFE0D5; font-weight: 500; }
.st-item.on span { color: #fff; font-weight: 700; }
.brand-foot { font-size: 12px; color: #7FA091; line-height: 1.6; }
.login-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px; }
.login-card { width: 100%; max-width: 420px; }
.auth-tabs { display: flex; gap: 4px; background: var(--surface-2); border-radius: 10px; padding: 4px; margin-bottom: 24px; }
.auth-tabs button { flex: 1; padding: 10px; border: none; border-radius: 8px; background: transparent; color: var(--text-muted); font-weight: 700; font-size: 13.5px; cursor: pointer; }
.auth-tabs button.active { background: var(--surface); color: var(--primary-dark); box-shadow: 0 1px 3px rgba(0,0,0,.08); }

.role-button-group { display: flex; gap: 12px; margin-bottom: 6px; }
.role-select-btn {
  flex: 1;
  padding: 14px 10px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--r-md);
  text-align: center;
  cursor: pointer;
  transition: all .15s ease;
}
.role-select-btn:hover { border-color: var(--primary); }
.role-select-btn.active {
  border-color: var(--primary);
  background: var(--primary-tint);
  box-shadow: 0 2px 6px rgba(31, 93, 80, 0.12);
}
.role-icon { font-size: 22px; margin-bottom: 4px; }
.role-btn-title { font-size: 13.5px; font-weight: 800; color: var(--text); }
.role-select-btn.active .role-btn-title { color: var(--primary-dark); }
.role-btn-desc { font-size: 11px; color: var(--text-soft); margin-top: 2px; }

.field-label { font-size: 12.5px; font-weight: 700; color: var(--text); margin-bottom: 6px; display: block; }
.field { margin-bottom: 14px; }
.field input, .field select, .field textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font-size: 14px; }
.field input:focus, .field select:focus, .field textarea:focus { outline: 2px solid var(--primary); outline-offset: 1px; border-color: var(--primary); }
.field-hint { font-size: 11.5px; color: var(--text-soft); margin-top: 4px; }

.field-row { display: flex; gap: 10px; }
.field-row .field { flex: 1; }

.address-fieldset {
  background: var(--surface-2);
  padding: 12px 14px;
  border-radius: var(--r-sm);
  margin-bottom: 14px;
  border: 1px solid var(--border);
}
.address-fieldset .field-label { color: var(--primary-dark); }

.role-fields { border-top: 1px dashed var(--border); padding-top: 16px; margin-top: 4px; }
.role-fields .rf-title { font-size: 12px; font-weight: 700; color: var(--primary-dark); text-transform: uppercase; letter-spacing: .05em; margin-bottom: 12px; }
.opt-label { font-weight: 500; color: var(--text-soft); }

.btn-primary { width: 100%; padding: 12px; border: none; border-radius: var(--r-sm); background: var(--primary); color: #fff; font-weight: 700; font-size: 14.5px; margin-top: 6px; cursor: pointer; }
.btn-primary:hover:not(:disabled) { background: var(--primary-dark); }
.btn-primary:disabled { background: var(--border); color: var(--text-soft); cursor: not-allowed; }

.link-muted { font-size: 12.5px; color: var(--text-muted); text-align: center; display: block; margin-top: 14px; }
.link-muted a { color: var(--primary); font-weight: 600; text-decoration: none; }
.form-note { font-size: 11.5px; color: var(--text-soft); line-height: 1.6; margin-top: 16px; background: var(--surface-2); padding: 10px 12px; border-radius: var(--r-sm); }

.step-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.role-tag { font-size: 11.5px; font-weight: 800; background: var(--primary-tint); color: var(--primary-dark); padding: 4px 10px; border-radius: 20px; }
.btn-text-back { background: none; border: none; color: var(--primary); font-weight: 700; font-size: 12.5px; cursor: pointer; padding: 0; }
.btn-text-back:hover { text-decoration: underline; }

@media (max-width: 980px) { .login-wrapper { flex-direction: column; } .login-brand { flex: none; min-height: auto; padding: 40px 28px; } }
</style>