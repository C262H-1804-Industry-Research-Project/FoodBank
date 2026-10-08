import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// Each test loads SuburbPicker with Mapbox switched on or off and fake suggestions
async function mountPicker({ lookupEnabled = true, suggestions = [], modelValue = [] } = {}) {
  vi.resetModules()
  const suggestAddresses = vi.fn(async () => suggestions)
  vi.doMock('../api/mapbox', async importOriginal => ({
    ...(await importOriginal()),
    isAddressLookupEnabled: lookupEnabled,
    suggestAddresses,
  }))
  const { default: SuburbPicker } = await import('./SuburbPicker.vue')
  const wrapper = mount(SuburbPicker, {
    props: {
      modelValue,
      // Behave like v-model so the pills update
      'onUpdate:modelValue': value => wrapper.setProps({ modelValue: value }),
    },
    attachTo: document.body,
  })
  return { wrapper, suggestAddresses }
}

const suburbSuggestion = (name, state, featureType = 'locality') => ({
  name, feature_type: featureType, context: { region: { region_code: state } },
})

async function typeAndWaitForSuggestions(wrapper, text) {
  await wrapper.find('input').setValue(text)
  await vi.advanceTimersByTimeAsync(300)
}

const pillLabels = wrapper => wrapper.findAll('.suburb-pill').map(pill => pill.text().replace('×', '').trim())
const lastEmitted = wrapper => wrapper.emitted('update:modelValue')?.at(-1)?.[0]

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  vi.doUnmock('../api/mapbox')
  document.body.innerHTML = ''
})

describe('SuburbPicker with Mapbox', () => {
  it('shows suburb suggestions from Mapbox while typing', async () => {
    const { wrapper, suggestAddresses } = await mountPicker({
      suggestions: [suburbSuggestion('Richmond', 'VIC'), suburbSuggestion('Richmond', 'NSW')],
    })

    await typeAndWaitForSuggestions(wrapper, 'rich')

    expect(suggestAddresses).toHaveBeenCalledWith('rich', expect.any(String), expect.any(AbortSignal), 'locality,place')
    expect(wrapper.findAll('[role=option]').map(option => option.text())).toEqual(['Richmond, VIC', 'Richmond, NSW'])
  })

  it('waits for two letters before searching', async () => {
    const { wrapper, suggestAddresses } = await mountPicker()

    await typeAndWaitForSuggestions(wrapper, 'r')

    expect(suggestAddresses).not.toHaveBeenCalled()
  })

  it('only searches once typing pauses', async () => {
    const { wrapper, suggestAddresses } = await mountPicker()
    const input = wrapper.find('input')

    await input.setValue('ri')
    await input.setValue('ric')
    await input.setValue('rich')
    await vi.advanceTimersByTimeAsync(300)

    expect(suggestAddresses).toHaveBeenCalledTimes(1)
    expect(suggestAddresses.mock.calls[0][0]).toBe('rich')
  })

  it('merges duplicate suggestions and skips ones without a suburb', async () => {
    const { wrapper } = await mountPicker({
      suggestions: [
        suburbSuggestion('Glenelg', 'SA'),
        suburbSuggestion('Glenelg', 'SA', 'place'),
        { name: '', feature_type: 'locality', context: {} },
      ],
    })

    await typeAndWaitForSuggestions(wrapper, 'glen')

    expect(wrapper.findAll('[role=option]')).toHaveLength(1)
  })

  it('adds a pill when a suggestion is clicked', async () => {
    const { wrapper } = await mountPicker({ suggestions: [suburbSuggestion('Glenelg', 'SA')] })

    await typeAndWaitForSuggestions(wrapper, 'glen')
    await wrapper.find('[role=option]').trigger('mousedown')

    expect(lastEmitted(wrapper)).toEqual([{ suburb: 'Glenelg', region: 'SA' }])
    expect(pillLabels(wrapper)).toEqual(['Glenelg, SA'])
    expect(wrapper.find('input').element.value).toBe('')
  })

  it('picks the highlighted suggestion with the arrow keys and Enter', async () => {
    const { wrapper } = await mountPicker({
      suggestions: [suburbSuggestion('Richmond', 'VIC'), suburbSuggestion('Richmond', 'NSW')],
    })
    const input = wrapper.find('input')

    await typeAndWaitForSuggestions(wrapper, 'rich')
    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'Enter' })

    expect(lastEmitted(wrapper)).toEqual([{ suburb: 'Richmond', region: 'NSW' }])
  })

  it('does not suggest suburbs that are already selected', async () => {
    const { wrapper } = await mountPicker({
      modelValue: [{ suburb: 'Richmond', region: 'VIC' }],
      suggestions: [suburbSuggestion('Richmond', 'VIC'), suburbSuggestion('Richmond', 'NSW')],
    })

    await typeAndWaitForSuggestions(wrapper, 'rich')

    expect(wrapper.findAll('[role=option]').map(option => option.text())).toEqual(['Richmond, NSW'])
  })

  it('removes a pill with its × button', async () => {
    const { wrapper } = await mountPicker({
      modelValue: [{ suburb: 'Glenelg', region: 'SA' }, { suburb: 'Richmond', region: 'VIC' }],
    })

    await wrapper.find('[aria-label="Remove Glenelg, SA"]').trigger('click')

    expect(lastEmitted(wrapper)).toEqual([{ suburb: 'Richmond', region: 'VIC' }])
  })

  it('removes the last pill with Backspace in an empty box', async () => {
    const { wrapper } = await mountPicker({
      modelValue: [{ suburb: 'Glenelg', region: 'SA' }, { suburb: 'Richmond', region: 'VIC' }],
    })

    await wrapper.find('input').trigger('keydown', { key: 'Backspace' })

    expect(lastEmitted(wrapper)).toEqual([{ suburb: 'Glenelg', region: 'SA' }])
  })

  it('does not add free text when Mapbox is available', async () => {
    const { wrapper } = await mountPicker()

    await wrapper.find('input').setValue('Glenelg')
    await wrapper.find('input').trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })
})

describe('SuburbPicker without Mapbox', () => {
  it('adds the typed name with Enter, matching any state', async () => {
    const { wrapper, suggestAddresses } = await mountPicker({ lookupEnabled: false })

    await typeAndWaitForSuggestions(wrapper, '  Glenelg ')
    await wrapper.find('input').trigger('keydown', { key: 'Enter' })

    expect(suggestAddresses).not.toHaveBeenCalled()
    expect(lastEmitted(wrapper)).toEqual([{ suburb: 'Glenelg', region: '' }])
    expect(pillLabels(wrapper)).toEqual(['Glenelg'])
  })

  it('ignores Enter in an empty box', async () => {
    const { wrapper } = await mountPicker({ lookupEnabled: false })

    await wrapper.find('input').trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })
})
