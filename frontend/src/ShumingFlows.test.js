import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import AuthIndex from './views/auth/AuthIndex.vue'
import DonorIndex from './views/donor/DonorIndex.vue'
import RecipientIndex from './views/recipient/RecipientIndex.vue'
import AdminIndex from './views/admin/AdminIndex.vue'
import router from './router'
import { storeSession, getStoredSession } from './api/client'
import { recipientRequestRow, donorRequestRow, canCancelPickup, cancelBlockedReason, isPickupOpen } from './api/pickupRequests'

const { push, reloadBrowse } = vi.hoisted(() => ({ push: vi.fn(), reloadBrowse: vi.fn() }))
vi.mock('vue-router', async original => ({ ...(await original()), useRouter: () => ({ push }) }))
const Browse = defineComponent({
  name: 'BrowseDonations', emits: ['request'], template: '<div />',
  setup(_, { expose }) { expose({ reload: reloadBrowse }) },
})
const slot = { timeslotId: 99, startTime: '2099-02-03T12:00:00', endTime: '2099-02-03T13:00:00', remainingSlot: 2 }
const record = status => ({
  requestId: 123, status, listingName: 'Test food', quantityRequested: 2, note: 'Lunch',
  recipientName: 'Recipient contact', recipientOrg: 'Test kitchen', recipientPhone: '0400000000',
  donorName: 'Test donor', listingQuantity: 10, timeslotStart: slot.startTime, timeslotEnd: slot.endTime,
  pickupAddress: '1 Test Street', pickupCode: '012345',
})
const response = (body, status = 200) => new Response(status === 204 ? null : JSON.stringify(body), { status })
let wrapper
beforeEach(async () => {
  wrapper = null
  localStorage.clear()
  push.mockReset()
  reloadBrowse.mockReset()
  vi.spyOn(window, 'alert').mockImplementation(() => {})
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  await router.push('/auth')
})
afterEach(() => wrapper?.unmount())

async function adminForm() {
  wrapper = mount(AuthIndex, { global: { stubs: { AddressAutocomplete: true } } })
  await wrapper.get('.admin-secret-link').trigger('click')
  expect(wrapper.get('input[type="email"]').element.value).toBe('')
  expect(wrapper.get('input[type="password"]').element.value).toBe('')
  await wrapper.get('input[type="email"]').setValue('staff@example.com')
  await wrapper.get('input[type="password"]').setValue('Password123!')
}

describe('administrator authentication', () => {
  it('sends role=admin and saves the returned session before opening the console', async () => {
    const session = { accessToken: 'staff-token', tokenType: 'Bearer', user: { role: 'admin', name: 'Staff' } }
    const fetchMock = vi.fn(async () => response(session))
    vi.stubGlobal('fetch', fetchMock)
    await adminForm()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/auth/login')
    expect(JSON.parse(options.body)).toEqual({ email: 'staff@example.com', password: 'Password123!', role: 'admin' })
    expect(getStoredSession()).toEqual(session)
    expect(push).toHaveBeenCalledWith('/admin')
  })

  it.each([
    [{ error: 'Invalid password' }, 401],
    [{ accessToken: 'wrong-role', user: { role: 'recipient' } }, 200],
    [{ user: { role: 'admin' } }, 200],
  ])('does not enter the console with a failed or invalid response %j', async (body, status) => {
    vi.stubGlobal('fetch', vi.fn(async () => response(body, status)))
    await adminForm()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(push).not.toHaveBeenCalled()
    expect(getStoredSession()).toBeNull()
    expect(window.alert).toHaveBeenCalled()
    expect(wrapper.get('button[type="submit"]').element.disabled).toBe(false)
  })

  it('disables duplicate login requests', async () => {
    let resolve
    const fetchMock = vi.fn(() => new Promise(done => { resolve = done }))
    vi.stubGlobal('fetch', fetchMock)
    await adminForm()
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')
    expect(fetchMock).toHaveBeenCalledOnce()
    expect(wrapper.get('button[type="submit"]').element.disabled).toBe(true)
    resolve(response({ error: 'Wrong password' }, 401))
    await flushPromises()
  })

  it.each([null, { user: { role: 'admin' } }, { accessToken: 'token', user: { role: 'donor' } }])(
    'redirects direct admin navigation without the required session %j', async session => {
      if (session) storeSession(session)
      await router.push('/admin')
      expect(router.currentRoute.value.path).toBe('/auth')
    },
  )
  it('allows direct navigation with an admin session', async () => {
    storeSession({ accessToken: 'token', user: { role: 'admin' } })
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/admin')
  })

  it.each([200, 500])('clears local admin authentication after logout returns HTTP %i', async status => {
    storeSession({ accessToken: 'token', user: { role: 'admin' } })
    const fetchMock = vi.fn(async () => response({ error: 'Logout failed' }, status))
    vi.stubGlobal('fetch', fetchMock)
    wrapper = mount(AdminIndex, { global: { stubs: { ManageListings: true, AccountManagement: true, MisuseReports: true } } })
    await wrapper.get('.sb-logout').trigger('click')
    await flushPromises()
    const logoutCall = fetchMock.mock.calls.find(([url]) => url === '/api/auth/logout')
    expect(logoutCall).toBeDefined()
    expect(logoutCall[1].headers.Authorization).toBe('Bearer token')
    expect(getStoredSession()).toBeNull()
    expect(push).toHaveBeenCalledWith('/auth')
  })
})

async function donorPanel(records, fail = false) {
  storeSession({ accessToken: 'donor-token', user: { role: 'donor', donorId: 5 } })
  const fetchMock = vi.fn(async (url, options) => {
    if (url.endsWith('/listings')) return response([])
    if (url.endsWith('/settings')) return response({ donorId: 5 })
    if (options.method === 'GET') return response(records)
    if (fail) return response({ error: 'Request already processed' }, 409)
    const id = Number(url.split('/').at(-2))
    const row = records.find(r => r.requestId === id)
    row.status = { approve: 'APPROVED', reject: 'REJECTED', complete: 'COMPLETED' }[url.split('/').at(-1)]
    return response(row)
  })
  vi.stubGlobal('fetch', fetchMock)
  wrapper = mount(DonorIndex, { global: { stubs: {
    MyListings: true, NewListing: true, DonationHistory: true, DonorReports: true, ReportList: true,
  } } })
  await wrapper.findAll('.sb-item')[2].trigger('click')
  await flushPromises()
  return fetchMock
}

describe('donor pickup review', () => {
  it('loads real rows and supports all three filters', async () => {
    await donorPanel([record('PENDING'), { ...record('APPROVED'), requestId: 124 }])
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.text()).toContain('Test kitchen')
    expect(wrapper.text()).toContain('0400000000')
    await wrapper.findAll('.status-tabs button')[0].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    await wrapper.findAll('.status-tabs button')[2].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
  })

  it.each([['.btn-approve', 'approve'], ['.btn-decline', 'reject']])('persists %s using the correct endpoint', async (button, action) => {
    const fetchMock = await donorPanel([record('PENDING')])
    await wrapper.get(button).trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith(`/api/donors/5/requests/123/${action}`, expect.objectContaining({ method: 'PATCH' }))
    expect(wrapper.find('tbody').exists()).toBe(false)
    await wrapper.findAll('.status-tabs button')[0].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
  })

  it('confirms handover of an approved request using the complete endpoint', async () => {
    const fetchMock = await donorPanel([record('APPROVED')])
    await wrapper.findAll('.status-tabs button')[2].trigger('click')
    await wrapper.get('.btn-verify-code').trigger('click')
    expect(wrapper.get('.code-display').text()).toBe('012345')
    await wrapper.get('.btn-confirm-handover').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith('/api/donors/5/requests/123/complete', expect.objectContaining({ method: 'PATCH' }))
    await wrapper.findAll('.status-tabs button')[0].trigger('click')
    expect(wrapper.get('tbody').text()).toContain('Completed')
    expect(wrapper.find('.btn-verify-code').exists()).toBe(false)
  })

  it('shows the handover deadline on approved rows and no handover action once expired', async () => {
    await donorPanel([
      { ...record('APPROVED'), handoverDeadline: '2026-10-21T11:15:00' },
      { ...record('EXPIRED'), requestId: 124 },
    ])
    await wrapper.findAll('.status-tabs button')[2].trigger('click')
    expect(wrapper.get('.handover-by').text()).toBe('Handover by 11:15')
    await wrapper.findAll('.status-tabs button')[3].trigger('click')
    expect(wrapper.get('tbody').text()).toContain('Not collected')
    expect(wrapper.find('.btn-verify-code').exists()).toBe(false)
  })

  it('shows the empty message instead of sample rows when the donor has no requests', async () => {
    await donorPanel([])
    expect(wrapper.find('tbody').exists()).toBe(false)
    expect(wrapper.text()).toContain('No requests found for this filter tab.')
  })

  it('preserves the pending row if PATCH fails', async () => {
    await donorPanel([record('PENDING')], true)
    await wrapper.get('.btn-approve').trigger('click')
    await flushPromises()
    expect(window.alert).toHaveBeenCalledWith('Request already processed')
    expect(wrapper.get('.btn-approve').element.disabled).toBe(false)
    expect(wrapper.get('tbody').text()).toContain('Pending')
  })

  it('does not decline when confirmation is cancelled', async () => {
    const fetchMock = await donorPanel([record('PENDING')])
    window.confirm.mockReturnValue(false)
    await wrapper.get('.btn-decline').trigger('click')
    expect(fetchMock.mock.calls.filter(([, options]) => options.method === 'PATCH')).toHaveLength(0)
  })

  it('returns to a valid page after the last request on page two is processed', async () => {
    await donorPanel(Array.from({ length: 6 }, (_, index) => ({ ...record('PENDING'), requestId: 123 + index })))
    await wrapper.findAll('.page-number-btn')[1].trigger('click')
    await wrapper.get('.btn-approve').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(5)
    expect(wrapper.get('.page-number-btn.active').text()).toBe('1')
  })
})

function recipientPortal(fetchMock) {
  storeSession({ accessToken: 'recipient-token', user: { role: 'recipient' } })
  vi.stubGlobal('fetch', fetchMock)
  wrapper = mount(RecipientIndex, { global: { stubs: { BrowseDonations: Browse, CollectionHistory: true, ReportIssue: true } } })
}
async function openRequest(slots = [slot]) {
  wrapper.findComponent(Browse).vm.$emit('request', { id: 10, name: 'Test food', donor: 'Test donor', capacity: 5, timeslots: slots })
  await wrapper.vm.$nextTick()
}
async function openMine() {
  await wrapper.findAll('.sb-item')[1].trigger('click')
  await flushPromises()
}

describe('recipient requests', () => {
  it.each(['PENDING', 'APPROVED'])('submits the selected ID, amount and note and respects returned %s', async status => {
    let records = []
    const fetchMock = vi.fn(async (_, options) => {
      if (options.method === 'GET') return response(records)
      records = [record(status)]
      return response(records[0], 201)
    })
    recipientPortal(fetchMock)
    await openRequest([{ ...slot, timeslotId: 98, remainingSlot: 0 }, slot])
    expect(wrapper.get('select').element.value).toBe('99')
    await wrapper.get('input[type="number"]').setValue('2')
    await wrapper.get('textarea').setValue('  Lunch  ')
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ timeslotId: 99, quantityRequested: 2, note: 'Lunch' })
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer recipient-token')
    expect(wrapper.find('.modal-box').exists()).toBe(false)
    expect(reloadBrowse).toHaveBeenCalledOnce()
    await openMine()
    expect(wrapper.text()).toContain(status === 'APPROVED' ? 'Reserved' : 'Requested')
    expect(wrapper.text()).toContain('2099-02-03: 12:00 - 13:00')
    expect(wrapper.find('code').exists()).toBe(status === 'APPROVED')
    if (status === 'APPROVED') expect(wrapper.text()).toContain('1 Test Street')
  })

  it.each(['', '0', '-1', '6'])('rejects invalid quantity %s without sending a request', async value => {
    const fetchMock = vi.fn()
    recipientPortal(fetchMock)
    await openRequest()
    await wrapper.get('input[type="number"]').setValue(value)
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(window.alert).toHaveBeenCalled()
  })

  it('refuses unavailable timeslots and oversized notes', async () => {
    const fetchMock = vi.fn()
    recipientPortal(fetchMock)
    await openRequest([{ ...slot, remainingSlot: 0 }])
    await wrapper.get('input[type="number"]').setValue('1')
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    expect(fetchMock).not.toHaveBeenCalled()
    await wrapper.get('.modal-close-row .btn-outline').trigger('click')
    await openRequest()
    await wrapper.get('input[type="number"]').setValue('1')
    await wrapper.get('textarea').setValue('a'.repeat(501))
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('keeps the form when the server rejects submission', async () => {
    recipientPortal(vi.fn(async () => response({ error: 'No places left' }, 409)))
    await openRequest()
    await wrapper.get('input[type="number"]').setValue('1')
    await wrapper.get('textarea').setValue('Keep this note')
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    await flushPromises()
    expect(window.alert).toHaveBeenCalledWith('No places left')
    expect(wrapper.get('textarea').element.value).toBe('Keep this note')
    expect(wrapper.get('.modal-close-row .btn-primary').element.disabled).toBe(false)
    expect(reloadBrowse).not.toHaveBeenCalled()
  })

  it('prevents duplicate submissions and clears inputs when reopening', async () => {
    let resolve
    const fetchMock = vi.fn(() => new Promise(done => { resolve = done }))
    recipientPortal(fetchMock)
    await openRequest()
    await wrapper.get('input[type="number"]').setValue('2')
    await wrapper.get('textarea').setValue('Old note')
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    await wrapper.get('.modal-close-row .btn-primary').trigger('click')
    expect(fetchMock).toHaveBeenCalledOnce()
    resolve(response(record('PENDING'), 201))
    await flushPromises()
    await openRequest([{ ...slot, timeslotId: 100 }])
    expect(wrapper.get('select').element.value).toBe('100')
    expect(wrapper.get('input[type="number"]').element.value).toBe('')
    expect(wrapper.get('textarea').element.value).toBe('')
  })

  it.each(['PENDING', 'APPROVED'])('cancels a future %s request using DELETE and refreshes persisted status', async status => {
    let saved = record(status)
    const fetchMock = vi.fn(async (_, options) => {
      if (options.method === 'GET') return response([saved])
      saved = { ...saved, status: 'CANCELLED', pickupCode: null, pickupAddress: null }
      return response(null, 204)
    })
    recipientPortal(fetchMock)
    await openMine()
    await wrapper.get('tbody button').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith('/api/requests/123', expect.objectContaining({ method: 'DELETE' }))
    expect(wrapper.text()).toContain('Cancelled')
    expect(wrapper.find('code').exists()).toBe(false)
  })

  it('does not change the row when cancellation fails', async () => {
    recipientPortal(vi.fn(async (_, options) => options.method === 'GET'
      ? response([record('PENDING')]) : response({ error: 'Pickup started' }, 409)))
    await openMine()
    await wrapper.get('tbody button').trigger('click')
    await flushPromises()
    expect(window.alert).toHaveBeenCalledWith('Pickup started')
    expect(wrapper.get('tbody').text()).toContain('Requested')
  })

  it('disables Cancel with a reason for requests that can no longer be cancelled', async () => {
    const past = { timeslotStart: '2020-01-01T10:00:00', timeslotEnd: '2020-01-01T11:00:00' }
    const fetchMock = vi.fn(async () => response([
      { ...record('PENDING'), requestId: 1 },
      { ...record('APPROVED'), ...past, requestId: 2 },
      { ...record('CANCELLED'), requestId: 3 },
      { ...record('REJECTED'), requestId: 4 },
    ]))
    recipientPortal(fetchMock)
    await openMine()
    const cells = wrapper.findAll('tbody tr').map(row => row.findAll('td').at(-1))
    expect(cells.map(cell => cell.text())).toEqual(['Cancel', 'Cancel', 'Cancel', 'Cancel'])
    expect(wrapper.find('tbody').text()).not.toContain('Details')
    expect(cells.map(cell => cell.get('button').attributes('disabled') !== undefined)).toEqual([false, true, true, true])
    expect(cells.map(cell => cell.get('span').attributes('title'))).toEqual(
      ['', 'Pickup time has passed', 'Already cancelled', 'Request was declined'])
    await cells[1].get('button').trigger('click')
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('shows Cancelling only on the request being cancelled', async () => {
    let resolve
    recipientPortal(vi.fn((_, options) => options.method === 'GET'
      ? Promise.resolve(response([{ ...record('PENDING'), requestId: 1 }, { ...record('APPROVED'), requestId: 2 }]))
      : new Promise(done => { resolve = done })))
    await openMine()
    await wrapper.findAll('tbody button')[0].trigger('click')
    const buttons = wrapper.findAll('tbody button')
    expect(buttons.map(button => button.text())).toEqual(['Cancelling…', 'Cancel'])
    expect(buttons.every(button => button.attributes('disabled') !== undefined)).toBe(true)
    resolve(response(null, 204)); await flushPromises()
  })

  it('shows load errors and retries, instead of displaying demo data', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(response({ error: 'Unavailable' }, 500))
      .mockResolvedValueOnce(response([]))
    recipientPortal(fetchMock)
    await openMine()
    expect(wrapper.get('[role="alert"]').text()).toContain('Unavailable')
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('No pickup requests yet.')
  })
})

describe('request display and time boundaries', () => {
  it.each(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'])('maps %s and hides private pickup details until approved', status => {
    const row = recipientRequestRow(record(status))
    expect(row.code).toBe(status === 'APPROVED' ? '012345' : null)
    expect(row.address).toBe(status === 'APPROVED' ? '1 Test Street' : null)
    expect(donorRequestRow(record(status)).phone).toBe('0400000000')
  })
  it('labels expired requests and tells donor declines apart from automatic rejections', () => {
    const expired = recipientRequestRow(record('EXPIRED'))
    expect([expired.status, expired.code, expired.address]).toEqual(['Expired', null, null])
    expect(cancelBlockedReason(expired)).toBe('Pickup window has ended')
    expect(donorRequestRow(record('EXPIRED'))).toMatchObject({ status: 'Expired', statusClass: 'status-expired' })

    const auto = { ...record('REJECTED'), rejectionReason: 'NOT_REVIEWED_IN_TIME' }
    expect(recipientRequestRow(auto).status).toBe('Not reviewed in time')
    expect(cancelBlockedReason(recipientRequestRow(auto))).toBe('Not reviewed before pickup')
    expect(donorRequestRow(auto).status).toBe('Auto-rejected')

    const declined = { ...record('REJECTED'), rejectionReason: 'DECLINED_BY_DONOR' }
    expect(recipientRequestRow(declined).status).toBe('Declined')
    expect(donorRequestRow(declined).status).toBe('Declined')
  })

  it('shows what is left of the listing on donor request rows', () => {
    expect(donorRequestRow({ ...record('PENDING'), availableQuantity: 8 }).totalStock).toBe('8 of 10 units')
    expect(donorRequestRow(record('PENDING')).totalStock).toBe('10 units')
  })

  it('permits active cancellation strictly before pickup starts', () => {
    const start = new Date(slot.startTime).getTime()
    for (const status of ['PENDING', 'APPROVED']) {
      const row = recipientRequestRow(record(status))
      expect(canCancelPickup(row, start - 1)).toBe(true)
      expect(canCancelPickup(row, start)).toBe(false)
    }
    expect(canCancelPickup(recipientRequestRow(record('CANCELLED')), start - 1)).toBe(false)
    expect(canCancelPickup(recipientRequestRow(record('COMPLETED')), start - 1)).toBe(false)
    expect(isPickupOpen(slot, start)).toBe(false)
    expect(isPickupOpen({ ...slot, remainingSlot: 0 }, start - 1)).toBe(false)
  })
})
