import { createRouter, createWebHistory } from 'vue-router'
import AuthIndex from '../views/auth/AuthIndex.vue'
import RecipientIndex from '../views/recipient/RecipientIndex.vue'

// Temporary placeholders for remaining steps
const DonorPlaceholder = { template: '<div style="padding:40px;"><h2>Welcome to Donor Console</h2></div>' }
const AdminPlaceholder = { template: '<div style="padding:40px;"><h2>Welcome to Admin Console</h2></div>' }

const routes = [
  { path: '/', redirect: '/auth' },
  { path: '/auth', name: 'Auth', component: AuthIndex },
  { path: '/recipient', name: 'Recipient', component: RecipientIndex },
  { path: '/donor', name: 'Donor', component: DonorPlaceholder },
  { path: '/admin', name: 'Admin', component: AdminPlaceholder }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router