import { createRouter, createWebHistory } from 'vue-router'
import AuthIndex from '../views/auth/AuthIndex.vue'
import RecipientIndex from '../views/recipient/RecipientIndex.vue'
import DonorIndex from '../views/donor/DonorIndex.vue'
import AdminIndex from '../views/admin/AdminIndex.vue'

const routes = [
  {
    path: '/',
    redirect: '/auth'
  },
  {
    path: '/auth',
    name: 'Auth',
    component: AuthIndex
  },
  {
    path: '/recipient',
    name: 'Recipient',
    component: RecipientIndex
  },
  {
    path: '/donor',
    name: 'Donor',
    component: DonorIndex
  },
  {
    path: '/admin',
    name: 'Admin',
    component: AdminIndex
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router