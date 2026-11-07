package com.tinhcd.myesalesmanager.feature.approval

import app.cash.turbine.test
import com.tinhcd.myesalesmanager.R
import com.tinhcd.myesalesmanager.core.ui.AppStrings
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.LeaveApproval
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Duyệt nghỉ phép through the shared approval list: load a status, ask,
 * confirm, and the list reloads with the ruling said in the snackbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveApprovalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ApprovalRepository>()
    private val strings = mockk<AppStrings>().also {
        every { it(R.string.status_approved, *anyVararg()) } returns "Đã duyệt"
        every { it(R.string.mgv_1, *anyVararg()) } returns "Đã từ chối"
        every { it(R.string.error_network, *anyVararg()) } returns "Không có kết nối mạng"
    }

    private val leave = LeaveApproval(
        id = "l1", salesperson = StaffBrief("s1", "nvbh01", "Tran Van Nam"), typeName = "Nghỉ phép năm",
        isPaid = true, fromDate = "2026-11-20", toDate = "2026-11-20", days = 1, reason = "Việc gia đình",
        status = ApprovalStatus.PENDING, decisionNote = null, decidedAt = null,
    )
    private val pendingPage = ApprovalPage(mapOf(ApprovalStatus.PENDING to 1), listOf(leave))
    private val emptyPage = ApprovalPage(mapOf(ApprovalStatus.PENDING to 0, ApprovalStatus.APPROVED to 1), emptyList<LeaveApproval>())

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = LeaveApprovalViewModel(strings, repository)

    @Test
    fun `loads the pending requests, through a loading state`() = runTest(dispatcher) {
        coEvery { repository.leaves(ApprovalStatus.PENDING) } returns DataResult.Success(pendingPage)
        val vm = viewModel()

        vm.state.test {
            assertTrue(awaitItem().loading)
            vm.refresh()
            // StateFlow drops the repeated loading state, so the next item is the list.
            val loaded = awaitItem()
            assertFalse(loaded.loading)
            assertEquals(listOf(leave), loaded.items)
            assertEquals(1, loaded.counts[ApprovalStatus.PENDING])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `approving rules, reloads and says so`() = runTest(dispatcher) {
        coEvery { repository.leaves(ApprovalStatus.PENDING) } returnsMany listOf(DataResult.Success(pendingPage), DataResult.Success(emptyPage))
        coEvery { repository.decideLeave("l1", any()) } returns DataResult.Success(Unit)
        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()

        vm.ask(PendingDecision("l1", approve = true, title = "Tran Van Nam nghỉ 20/11/2026"))
        vm.confirm(note = "Đồng ý", qty = null); advanceUntilIdle()

        coVerify { repository.decideLeave("l1", match { it.approve && it.note == "Đồng ý" }) }
        val state = vm.state.value
        assertNull(state.pending)
        assertEquals("Đã duyệt", state.message)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `rejecting says rejected`() = runTest(dispatcher) {
        coEvery { repository.leaves(any()) } returns DataResult.Success(pendingPage)
        coEvery { repository.decideLeave("l1", any()) } returns DataResult.Success(Unit)
        val vm = viewModel()

        vm.ask(PendingDecision("l1", approve = false, title = "x"))
        vm.confirm(note = "Trùng lịch", qty = null); advanceUntilIdle()

        coVerify { repository.decideLeave("l1", match { !it.approve && it.note == "Trùng lịch" }) }
        assertEquals("Đã từ chối", vm.state.value.message)
    }

    @Test
    fun `a failed ruling keeps the dialog and reports the error`() = runTest(dispatcher) {
        coEvery { repository.leaves(any()) } returns DataResult.Success(pendingPage)
        coEvery { repository.decideLeave(any(), any()) } returns DataResult.Failure(AppError.Network())
        val vm = viewModel()

        vm.ask(PendingDecision("l1", approve = true, title = "x"))
        vm.confirm(note = "", qty = null); advanceUntilIdle()

        assertEquals("l1", vm.state.value.pending?.id)
        assertFalse(vm.state.value.deciding)
        assertEquals("Không có kết nối mạng", vm.state.value.message)
    }

    @Test
    fun `switching status reloads that status`() = runTest(dispatcher) {
        coEvery { repository.leaves(ApprovalStatus.PENDING) } returns DataResult.Success(pendingPage)
        coEvery { repository.leaves(ApprovalStatus.APPROVED) } returns DataResult.Success(emptyPage)
        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()

        vm.selectStatus(ApprovalStatus.APPROVED); advanceUntilIdle()

        coVerify { repository.leaves(ApprovalStatus.APPROVED) }
        assertEquals(ApprovalStatus.APPROVED, vm.state.value.status)
        assertTrue(vm.state.value.items.isEmpty())
    }
}
