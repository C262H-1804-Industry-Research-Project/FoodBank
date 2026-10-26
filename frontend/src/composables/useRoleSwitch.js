import { ref } from 'vue'
import { addRole, linkAccount, storeSession, switchRole } from '../api/client'

export function useRoleSwitch({ currentRole, targetRole, router }) {
  const switchingRole = ref(false)
  const showAddRoleModal = ref(false)
  const addingRole = ref(false)
  const addRoleError = ref('')

  const roleLabel = role => role.charAt(0).toUpperCase() + role.slice(1)

  async function completeSwitch() {
    const newSession = await switchRole(targetRole)

    if (
      !newSession?.accessToken ||
      newSession?.user?.role !== targetRole
    ) {
      throw new Error('The role switch response is invalid.')
    }

    storeSession(newSession)
    await router.push(`/${targetRole}`)
  }

  async function handleSwitchRole() {
    if (switchingRole.value) return

    const confirmed = window.confirm(
      `Are you sure you want to switch from ${roleLabel(currentRole)} to ${roleLabel(targetRole)}?`
    )
    if (!confirmed) return

    switchingRole.value = true

    try {
      await completeSwitch()
    } catch (error) {
      if (error.message === `${roleLabel(targetRole)} profile does not exist`) {
        addRoleError.value = ''
        showAddRoleModal.value = true
      } else {
        window.alert(error.message)
      }
    } finally {
      switchingRole.value = false
    }
  }

  function closeAddRoleModal() {
    if (addingRole.value) return
    showAddRoleModal.value = false
    addRoleError.value = ''
  }

  async function handleCreateRole(details) {
    if (addingRole.value) return

    addingRole.value = true
    addRoleError.value = ''

    try {
      if (details.mode === 'link') {
        await linkAccount({ targetRole, email: details.email, password: details.password })
      } else {
        await addRole(details)
      }
      await completeSwitch()
      showAddRoleModal.value = false
    } catch (error) {
      addRoleError.value = error.message
    } finally {
      addingRole.value = false
    }
  }

  return {
    switchingRole,
    showAddRoleModal,
    addingRole,
    addRoleError,
    handleSwitchRole,
    handleCreateRole,
    closeAddRoleModal,
  }
}
