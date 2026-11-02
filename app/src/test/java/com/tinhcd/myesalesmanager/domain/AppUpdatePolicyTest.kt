package com.tinhcd.myesalesmanager.domain

import com.tinhcd.myesalesmanager.domain.model.AppUpdateNeed
import com.tinhcd.myesalesmanager.domain.model.AppUpdatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdatePolicyTest {

    private fun policy(latest: Int?, min: Int?) = AppUpdatePolicy(latest, min, url = null, note = null)

    @Test
    fun `a build at the latest needs nothing`() {
        assertEquals(AppUpdateNeed.NONE, policy(latest = 3, min = 2).check(3))
    }

    @Test
    fun `a build behind the latest is offered the update`() {
        assertEquals(AppUpdateNeed.OPTIONAL, policy(latest = 3, min = 2).check(2))
    }

    @Test
    fun `a build behind the minimum must update`() {
        assertEquals(AppUpdateNeed.REQUIRED, policy(latest = 3, min = 2).check(1))
    }

    @Test
    fun `the minimum wins when it is above the latest`() {
        // A misconfiguration, but the safe reading: the server has said this
        // build is no longer supported.
        assertEquals(AppUpdateNeed.REQUIRED, policy(latest = 1, min = 2).check(1))
    }

    @Test
    fun `nothing configured needs nothing`() {
        assertEquals(AppUpdateNeed.NONE, policy(latest = null, min = null).check(1))
    }

    @Test
    fun `read from settings, blanks and junk are null`() {
        val p = AppUpdatePolicy.from(
            mapOf(
                "manager_version_latest" to " 4 ",
                "manager_version_min" to "abc",
                "manager_update_url" to "",
                "manager_update_note" to "  Sửa lỗi  ",
            ),
        )
        assertEquals(4, p.latestCode)
        assertNull(p.minCode)
        assertNull(p.url)
        assertEquals("Sửa lỗi", p.note)
    }

    @Test
    fun `the SFA keys are not read as the manager's`() {
        val p = AppUpdatePolicy.from(mapOf("sfa_version_min" to "9"))
        assertNull(p.minCode)
    }
}
