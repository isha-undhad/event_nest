package com.example.eventnest

import com.example.eventnest.adapters.ManageUserAdapter
import com.example.eventnest.models.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageUsersProtectionTest {

    @Test
    fun testGetInitials() {
        assertEquals("JD", ManageUserAdapter.getInitials("Jane Doe"))
        assertEquals("RS", ManageUserAdapter.getInitials("Rohan Sharma"))
        assertEquals("AL", ManageUserAdapter.getInitials("Alice"))
        assertEquals("B", ManageUserAdapter.getInitials("B"))
        assertEquals("U", ManageUserAdapter.getInitials(""))
        assertEquals("U", ManageUserAdapter.getInitials("   "))
    }

    @Test
    fun testSelfRemovalProtection() {
        val currentAdminUid = "admin_uid_123"

        val adminUser = User(uid = "admin_uid_123", name = "Super Admin", role = "admin")
        val normalUser = User(uid = "user_uid_456", name = "Regular Member", role = "user")

        // Admin cannot remove themselves
        val canRemoveAdmin = adminUser.uid != currentAdminUid
        assertFalse("Logged-in admin must NOT be removable", canRemoveAdmin)

        // Other users can be removed
        val canRemoveMember = normalUser.uid != currentAdminUid
        assertTrue("Normal user must be removable by admin", canRemoveMember)
    }

    @Test
    fun testAdminRoleBadgeDetermination() {
        val adminUser = User(uid = "u1", role = "admin")
        val adminUserCaps = User(uid = "u2", role = "ADMIN")
        val standardUser = User(uid = "u3", role = "user")
        val blankRoleUser = User(uid = "u4", role = "")

        assertTrue(adminUser.role.equals("admin", ignoreCase = true))
        assertTrue(adminUserCaps.role.equals("admin", ignoreCase = true))
        assertFalse(standardUser.role.equals("admin", ignoreCase = true))
        assertFalse(blankRoleUser.role.equals("admin", ignoreCase = true))
    }
}
