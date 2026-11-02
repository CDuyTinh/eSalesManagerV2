package com.tinhcd.myesalesmanager.domain

import com.tinhcd.myesalesmanager.domain.model.BranchPlace
import com.tinhcd.myesalesmanager.domain.model.PlacePick
import com.tinhcd.myesalesmanager.domain.model.Position
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.branchIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamTest {

    @Test
    fun `an unknown position code reads as a rep, never a manager`() {
        assertEquals(Position.SR, Position.of("admin"))
        assertEquals(Position.SR, Position.of(null))
        assertFalse(Position.of("xx").isManager)
    }

    @Test
    fun `known codes read as themselves`() {
        assertEquals(Position.SS, Position.of("ss"))
        assertEquals(Position.AM, Position.of("am"))
        assertEquals(Position.RM, Position.of("rm"))
        assertTrue(Position.AM.isManager)
    }

    private fun member(id: String, branch: BranchPlace?) =
        TeamMember(id, id, id, Position.SR, phone = null, branchId = branch?.id, branch = branch)

    private val mienDong = BranchPlace("b1", "NPP Mien Dong", areaId = "a1", areaName = "Đông Nam Bộ", regionId = "r1", regionName = "Miền Nam")
    private val dongNai = BranchPlace("b2", "NPP Dong Nai", areaId = "a1", areaName = "Đông Nam Bộ", regionId = "r1", regionName = "Miền Nam")
    private val haNoi = BranchPlace("b3", "NPP Ha Noi", areaId = "a2", areaName = "Hà Nội", regionId = "r2", regionName = "Miền Bắc")
    private val team = listOf(member("1", mienDong), member("2", mienDong), member("3", dongNai), member("4", haNoi), member("5", null))

    @Test
    fun `no place picked means no branch filter`() {
        assertNull((null as PlacePick?).branchIds(team))
    }

    @Test
    fun `a region covers its areas' distributors, once each`() {
        assertEquals(listOf("b1", "b2"), PlacePick.Region("r1").branchIds(team))
    }

    @Test
    fun `an area and a distributor narrow further`() {
        assertEquals(listOf("b3"), PlacePick.Area("a2").branchIds(team))
        assertEquals(listOf("b2"), PlacePick.Branch("b2").branchIds(team))
    }

    @Test
    fun `a place with nobody in it covers nothing`() {
        assertEquals(emptyList<String>(), PlacePick.Region("r9").branchIds(team))
    }
}
