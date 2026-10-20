<template>
  <div class="landing-page">
    <!-- 1. Top Global Navigation -->
    <header class="top-nav">
      <div class="nav-container">
        <div class="nav-brand">Second Table</div>
        <div class="nav-tagline">South Australian Food Rescue & Redistribution Network</div>
      </div>
    </header>

    <!-- 2. Hero Banner with Key Info (Full Width, Centered) -->
    <section class="hero-banner">
      <div class="hero-inner">
        <h1 class="hero-title">Surplus Food, Delivered to Those in Need</h1>
        <p class="hero-lead">
          A dedicated community platform connecting local food businesses and community charities.
          List excess edible inventory before expiry, reserve pickup slots, and reduce waste with dignity.
        </p>

        <!-- Every donation journey steps displayed horizontally -->
        <div class="workflow-strip">
          <div class="wf-step">
            <span class="wf-num">1</span>
            <div class="wf-text">
              <strong>Available</strong>
              <span>Donors post excess food & slots</span>
            </div>
          </div>
          <div class="wf-arrow">→</div>
          <div class="wf-step">
            <span class="wf-num">2</span>
            <div class="wf-text">
              <strong>Requested</strong>
              <span>Charities apply for community meals</span>
            </div>
          </div>
          <div class="wf-arrow">→</div>
          <div class="wf-step">
            <span class="wf-num">3</span>
            <div class="wf-text">
              <strong>Reserved</strong>
              <span>Donor approves & coordinates pickup</span>
            </div>
          </div>
          <div class="wf-arrow">→</div>
          <div class="wf-step">
            <span class="wf-num">4</span>
            <div class="wf-text">
              <strong>Collected</strong>
              <span>Surplus rescued on schedule</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. Centered Action Portal (Login / Register / Hidden Admin) -->
    <main class="portal-main">
      <div class="auth-card">
        <!-- Normal User Mode (Login & Register tabs) -->
        <template v-if="!showAdminLogin">
          <!-- Top Auth Tabs Switch -->
          <div class="auth-tabs">
            <button :class="{ active: currentTab === 'signin' }" @click="switchTab('signin')">
              Log In
            </button>
            <button :class="{ active: currentTab === 'signup' }" @click="switchTab('signup')">
              Create Account
            </button>
          </div>

          <!-- TAB 1: LOGIN FORM -->
          <form v-if="currentTab === 'signin'" @submit.prevent="handleLogin">
            <div class="field">
              <label class="field-label">Select your role</label>
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
              <label class="field-label">Email address</label>
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

            <button type="submit" class="btn-primary-large">
              Log in as {{ loginRole === 'donor' ? 'Donor' : 'Recipient' }}
            </button>
            
            <div class="form-sub-links">
              <span>Forgot your password? <a href="#">Reset it</a></span>
            </div>

            <div class="form-note">
              Selected portal: <strong>{{ loginRole.toUpperCase() }}</strong>. Submitting will authenticate and redirect to your dashboard.
            </div>
          </form>

          <!-- TAB 2: MULTI-STEP REGISTER FORM -->
          <div v-else>
            <!-- Step 1: Select Role -->
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
                  <button
                    type="button"
                    class="role-select-btn"
                    :class="{ active: signupForm.role === 'both' }"
                    @click="signupForm.role = 'both'"
                  >
                    <div class="role-icon">🔄</div>
                    <div class="role-btn-title">Both</div>
                    <div class="role-btn-desc">I want to donate and collect food</div>
                  </button>
                </div>
              </div>

              <button 
                type="button" 
                class="btn-primary-large" 
                :disabled="!signupForm.role"
                @click="signupStep = 2"
              >
                Next step →
              </button>

              <div class="form-note">
                Select whether you want to donate food, receive food, or use both roles.
                You will configure your profile details on the next step.
              </div>
            </div>

            <!-- Step 2: Role Details -->
            <form v-else-if="signupStep === 2" @submit.prevent="handleRegister">
              <div class="step-header">
                <span class="role-tag">ROLE: {{ signupForm.role.toUpperCase() }}</span>
                <button type="button" class="btn-text-back" @click="signupStep = 1">← Change role</button>
              </div>

              <!-- Donor specific fields -->
              <div
                v-if="signupForm.role === 'donor' || signupForm.role === 'both'"
                class="role-fields"
              >
                <div class="rf-title">Donor Organisation Details</div>
                <div class="field">
                  <label class="field-label">Organisation name</label>
                  <input v-model="signupForm.donorOrg" type="text" placeholder="e.g. Green Leaf Café" required>
                </div>
                <div class="field">
                  <label class="field-label">Contact phone number</label>
                  <input
                    v-model="signupForm.phone"
                    type="text"
                    placeholder="04xx xxx xxx"
                    :required="signupForm.role === 'both'"
                  >
                </div>

                <!-- 3-Part Address Breakdown -->
                <div class="address-fieldset">
                  <label class="field-label">Pickup Address</label>
                  <div class="field">
                    <AddressAutocomplete
                      v-model="signupForm.donorStreet"
                      placeholder="Start typing the street address (e.g. 14 Rundle St)"
                      required
                      @select="applyDonorAddress"
                    />
                  </div>
                  <div class="field-row">
                    <div class="field">
                      <input v-model="signupForm.donorSuburb" type="text" placeholder="Suburb (e.g. Adelaide)" required>
                    </div>
                    <div class="field">
                      <select v-model="signupForm.donorState" required>
                        <option value="" disabled>State / territory</option>
                        <option v-for="state in australianStates" :key="state" :value="state">{{ state }}</option>
                      </select>
                    </div>
                  </div>
                  <div class="field-hint">Your exact pickup address can be adjusted per listing.</div>
                </div>
              </div>

              <!-- Recipient specific fields -->
              <div
                v-if="signupForm.role === 'recipient' || signupForm.role === 'both'"
                class="role-fields"
              >
                <div class="rf-title">Recipient Organisation Details</div>
                <div class="field">
                  <label class="field-label">Your name</label>
                  <input v-model="signupForm.recipientName" type="text" placeholder="e.g. Priya Nair" required>
                </div>
                <div class="field">
                  <label class="field-label">Organisation name <span class="opt-label">(optional)</span></label>
                  <input v-model="signupForm.recipientOrg" type="text" placeholder="e.g. Community Kitchen East">
                </div>
                <div v-if="signupForm.role === 'recipient'" class="field">
                  <label class="field-label">Phone number</label>
                  <input v-model="signupForm.phone" type="text" placeholder="04xx xxx xxx" required>
                </div>
              </div>

              <!-- Credentials -->
              <div class="field">
                <label class="field-label">Account email</label>
                <input v-model="signupForm.email" type="email" placeholder="you@organisation.org" required>
              </div>
              <div class="field">
                <label class="field-label">Account password</label>
                <input v-model="signupForm.password" type="password" placeholder="Create a strong password" required>
              </div>

              <button type="submit" class="btn-primary-large">Create account</button>
              <div class="form-note">
                Organisation accounts are reviewed by an administrator before publishing or requesting donations.
              </div>
            </form>
          </div>
        </template>

        <!-- Hidden Admin Login Mode -->
        <template v-else>
          <div class="admin-header">
            <span class="admin-badge">Staff & Moderation</span>
            <h2>Administrator Portal</h2>
            <p>Authorized access only. System activity is logged.</p>
          </div>

          <form @submit.prevent="handleAdminLogin">
            <div class="field">
              <label class="field-label">Admin email</label>
              <input v-model="adminUser" type="email" placeholder="admin@example.com" required>
            </div>
            <div class="field">
              <label class="field-label">Admin password</label>
              <input v-model="adminPassword" type="password" placeholder="••••••••" required>
            </div>

            <button type="submit" class="btn-danger-large" :disabled="adminPending">
              {{ adminPending ? 'Signing in…' : 'Verify & Enter Console' }}
            </button>
            <button type="button" class="btn-text-link" :disabled="adminPending" @click="showAdminLogin = false">
              ← Back to public user login
            </button>
          </form>
        </template>
      </div>

      <!-- Bottom discreet link for Admin login toggle -->
      <footer class="portal-footer">
        <button 
          v-if="!showAdminLogin" 
          type="button" 
          class="admin-secret-link" 
          @click="showAdminLogin = true"
        >
          🔒 Administrator Access
        </button>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  login,
  registerBoth,
  registerDonor,
  registerRecipient,
  storeSession,
} from '../../api/client'
import AddressAutocomplete from '../../components/AddressAutocomplete.vue'

const router = useRouter()
const australianStates = ['ACT', 'NSW', 'NT', 'QLD', 'SA', 'TAS', 'VIC', 'WA']
const currentTab = ref('signin')
const loginRole = ref('recipient')
const signupStep = ref(1)
const showAdminLogin = ref(false)

const adminUser = ref('')
const adminPassword = ref('')
const adminPending = ref(false)

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
  donorSuburb: '',
  donorState: '',
  recipientName: '',
  recipientOrg: '',
  phone: '',
  email: '',
  password: ''
})

function applyDonorAddress(address) {
  if (address.suburb) signupForm.donorSuburb = address.suburb
  if (address.stateRegion) signupForm.donorState = address.stateRegion
}

async function handleLogin() {
  try {
    const session = await login({
      email: loginForm.account,
      password: loginForm.password,
      role: loginRole.value,
    })
    completeLogin(session)
  } catch (error) {
    window.alert(error.message)
  }
}

async function handleRegister() {
  try {
    if (signupForm.role === 'donor') {
      await registerDonor({
        email: signupForm.email,
        password: signupForm.password,
        phone: signupForm.phone,
        orgName: signupForm.donorOrg,
        addressLine: signupForm.donorStreet,
        suburb: signupForm.donorSuburb,
        region: signupForm.donorState,
      })
    } else if (signupForm.role === 'recipient') {
      await registerRecipient({
        name: signupForm.recipientName,
        phone: signupForm.phone,
        orgName: signupForm.recipientOrg,
        email: signupForm.email,
        password: signupForm.password,
      })
    } else {
      await registerBoth({
        email: signupForm.email,
        password: signupForm.password,
        phone: signupForm.phone,
        donorOrgName: signupForm.donorOrg,
        addressLine: signupForm.donorStreet,
        suburb: signupForm.donorSuburb,
        region: signupForm.donorState,
        recipientName: signupForm.recipientName,
        recipientOrgName: signupForm.recipientOrg,
      })
    }

    // Sign in immediately using the same credentials before opening the portal.
    const initialRole = signupForm.role === 'both' ? 'donor' : signupForm.role
    const session = await login({
      email: signupForm.email,
      password: signupForm.password,
      role: initialRole,
    })
    completeLogin(session)
  } catch (error) {
    window.alert(error.message)
  }
}

function completeLogin(session) {
  if (!session?.accessToken || !session?.user?.role) {
    throw new Error('The login response is missing session details.')
  }
  storeSession(session)
  router.push(session.user.role === 'donor' ? '/donor' : '/recipient')
}

async function handleAdminLogin() {
  if (adminPending.value) return
  adminPending.value = true
  try {
    const session = await login({ email: adminUser.value.trim(), password: adminPassword.value, role: 'admin' })
    if (!session?.accessToken || session?.user?.role !== 'admin') {
      throw new Error('Administrator login returned an invalid session.')
    }
    storeSession(session)
    await router.push('/admin')
  } catch (error) {
    window.alert(error.message)
  } finally {
    adminPending.value = false
  }
}
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F4EFEB;
  color: var(--text);
}

/* Global Top Navigation */
.top-nav {
  background: #FFFFFF;
  border-bottom: 1px solid var(--border);
  padding: 16px 40px;
}
.nav-container {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.nav-brand {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 700;
  color: var(--primary-dark);
}
.nav-tagline {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
}

/* Hero Banner & Workflow Strip */
.hero-banner {
  background: linear-gradient(145deg, #162420 0%, #243831 100%);
  color: #FFFFFF;
  padding: 50px 32px 42px;
  text-align: center;
}
.hero-inner {
  max-width: 940px;
  margin: 0 auto;
}
.hero-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}
.hero-lead {
  font-size: 16px;
  line-height: 1.6;
  color: #D3E0D9;
  max-width: 760px;
  margin: 0 auto 32px;
}

.workflow-strip {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--r-md);
  padding: 16px 24px;
  flex-wrap: wrap;
}
.wf-step {
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
}
.wf-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 13.5px;
  flex: 0 0 28px;
}
.wf-text strong {
  display: block;
  font-size: 14px;
  color: #FFFFFF;
}
.wf-text span {
  font-size: 12px;
  color: #B5CCC0;
}
.wf-arrow {
  color: #7A998B;
  font-weight: 700;
  font-size: 16px;
}

/* Centered Main Portal */
.portal-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 24px;
}
.auth-card {
  width: 100%;
  max-width: 480px;
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  padding: 34px 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

/* Tabs */
.auth-tabs {
  display: flex;
  gap: 6px;
  background: var(--surface-2);
  border-radius: var(--r-sm);
  padding: 5px;
  margin-bottom: 24px;
}
.auth-tabs button {
  flex: 1;
  padding: 11px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: all .15s;
}
.auth-tabs button.active {
  background: #FFFFFF;
  color: var(--primary-dark);
  box-shadow: 0 2px 6px rgba(0,0,0,.08);
}

/* Role Selector Buttons */
.role-button-group {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}
.role-select-btn {
  flex: 1;
  padding: 16px 12px;
  background: #FFFFFF;
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
  box-shadow: 0 2px 8px rgba(31, 93, 80, 0.12);
}
.role-icon { font-size: 26px; margin-bottom: 4px; }
.role-btn-title { font-size: 15px; font-weight: 800; color: var(--text); }
.role-select-btn.active .role-btn-title { color: var(--primary-dark); }
.role-btn-desc { font-size: 12px; color: var(--text-soft); margin-top: 3px; }

/* Fields & Inputs - Enhanced Typography */
.field { margin-bottom: 16px; }
.field-label {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
  display: block;
}
.field input, .field :deep(.address-autocomplete input), .field select, .field textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: #FFFFFF;
  color: var(--text);
  font-size: 14.5px;
  box-sizing: border-box;
}
.field input:focus, .field :deep(.address-autocomplete input:focus), .field select:focus, .field textarea:focus {
  outline: 2px solid var(--primary);
  outline-offset: 1px;
  border-color: var(--primary);
}
.field-hint { font-size: 12px; color: var(--text-soft); margin-top: 4px; }
.field-row { display: flex; gap: 10px; }
.field-row .field { flex: 1; }

.address-fieldset {
  background: var(--surface-2);
  padding: 14px 16px;
  border-radius: var(--r-sm);
  margin-bottom: 16px;
  border: 1px solid var(--border);
}
.address-fieldset .field-label { color: var(--primary-dark); }

.role-fields {
  border-top: 1px dashed var(--border);
  padding-top: 18px;
  margin-top: 4px;
}
.rf-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary-dark);
  text-transform: uppercase;
  letter-spacing: .05em;
  margin-bottom: 14px;
}
.opt-label { font-weight: 500; color: var(--text-soft); }

.btn-primary-large {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: var(--r-sm);
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  font-size: 15.5px;
  margin-top: 6px;
  cursor: pointer;
  transition: background .15s;
}
.btn-primary-large:hover:not(:disabled) { background: var(--primary-dark); }
.btn-primary-large:disabled { background: var(--border); color: var(--text-soft); cursor: not-allowed; }

.btn-danger-large {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: var(--r-sm);
  background: var(--danger);
  color: #fff;
  font-weight: 700;
  font-size: 15.5px;
  margin-top: 6px;
  cursor: pointer;
}

.btn-text-link {
  display: block;
  width: 100%;
  text-align: center;
  background: none;
  border: none;
  margin-top: 14px;
  color: var(--text-muted);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
}
.btn-text-link:hover { text-decoration: underline; }

.form-sub-links {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  margin-top: 14px;
}
.form-sub-links a { color: var(--primary); text-decoration: none; font-weight: 600; }

.form-note {
  font-size: 12.5px;
  color: var(--text-soft);
  line-height: 1.5;
  margin-top: 16px;
  background: var(--surface-2);
  padding: 10px 12px;
  border-radius: var(--r-sm);
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.role-tag {
  font-size: 12px;
  font-weight: 800;
  background: var(--primary-tint);
  color: var(--primary-dark);
  padding: 4px 10px;
  border-radius: 20px;
}
.btn-text-back {
  background: none;
  border: none;
  color: var(--primary);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

/* Admin Mode Styling */
.admin-header {
  margin-bottom: 20px;
}
.admin-badge {
  display: inline-block;
  background: var(--danger-tint);
  color: var(--danger);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: .08em;
  padding: 4px 10px;
  border-radius: 12px;
  margin-bottom: 8px;
}
.admin-header h2 {
  font-size: 24px;
  color: var(--danger);
  margin-bottom: 4px;
}
.admin-header p {
  font-size: 13.5px;
  color: var(--text-muted);
  margin: 0;
}

/* Bottom Secret Admin Entrance */
.portal-footer {
  margin-top: 24px;
}
.admin-secret-link {
  background: none;
  border: none;
  color: var(--text-soft);
  font-size: 13px;
  cursor: pointer;
  opacity: 0.65;
  transition: opacity .15s;
}
.admin-secret-link:hover {
  opacity: 1;
  text-decoration: underline;
}

@media (max-width: 860px) {
  .workflow-strip { flex-direction: column; gap: 8px; }
  .wf-arrow { transform: rotate(90deg); }
  .top-nav { padding: 16px 20px; }
  .hero-banner { padding: 36px 20px; }
  .hero-title { font-size: 28px; }
}
</style>
