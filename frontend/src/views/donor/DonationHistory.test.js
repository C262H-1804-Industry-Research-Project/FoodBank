import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DonationHistory from './DonationHistory.vue'

const row = (id, category = 'Produce') => ({
  id, category, status: 'Completed', foodName: `Carrots ${id}`, quantity: 3,
  collectedBy: 'Community Kitchen', completedDate: '2026-09-02 10:00',
})

describe('DonationHistory', () => {
  it('counts and filters explicit outcomes without treating missing status as failure', async () => {
    const wrapper = mount(DonationHistory, { props: { historyList: [
      row(1), { ...row(2), status: 'Failed' }, { ...row(3), status: undefined },
    ] } })
    const tabs = wrapper.findAll('.status-tabs button')
    expect(tabs.map(tab => tab.text())).toEqual(['All (3)', 'Successful (1)', 'Failed (1)'])
    await tabs[1].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.find('tbody').text()).toContain('Carrots 1')
    await tabs[2].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.find('tbody').text()).toContain('Carrots 2')
    await wrapper.setProps({ historyList: [row(1)] })
    expect(wrapper.find('tbody').exists()).toBe(false)
    expect(wrapper.text()).toContain('No donation records')
  })

  it('renders records, paginates and resets the page when filtering', async () => {
    const wrapper = mount(DonationHistory, { props: { historyList: Array.from({ length: 6 }, (_, i) => row(i)) } })
    expect(wrapper.findAll('tbody tr')).toHaveLength(5)
    await wrapper.findAll('.page-btn')[1].trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    await wrapper.find('input').setValue('Carrots 0')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.text()).toContain('Carrots 0')
    await wrapper.setProps({ historyList: [row(7, 'Other category')] })
    await wrapper.find('input').setValue('')
    await wrapper.find('select').setValue('Other category')
    expect(wrapper.text()).toContain('Carrots 7')
  })

  it('shows loading, errors with retry, and an empty state', async () => {
    const wrapper = mount(DonationHistory, { props: { historyList: [], loading: true } })
    expect(wrapper.find('[role="status"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('No completed')
    await wrapper.setProps({ loading: false, error: 'Connection failed' })
    expect(wrapper.find('[role="alert"]').text()).toContain('Connection failed')
    await wrapper.find('[role="alert"] button').trigger('click')
    expect(wrapper.emitted('reload')).toHaveLength(1)
    await wrapper.setProps({ error: '' })
    expect(wrapper.text()).toContain('No donation records')
  })
})
