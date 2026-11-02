package com.tinhcd.myesalesmanager.feature

import androidx.compose.ui.text.AnnotatedString
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.Position
import com.tinhcd.myesalesmanager.domain.model.TargetLine
import com.tinhcd.myesalesmanager.domain.model.TargetSheet
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.TargetRepository
import com.tinhcd.myesalesmanager.feature.targets.TargetsViewModel
import com.tinhcd.myesalesmanager.feature.targets.ThousandsTransformation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TargetsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val month = LocalDate.of(2026, 11, 1)

    private val rep = TeamMember("rep-1", "nvbh01", "Tran Van Nam", Position.SR, phone = null, branchId = "b1")

    private val sheet = TargetSheet(
        month = month,
        canEdit = true,
        revenueTarget = 150_000_000,
        orderTarget = 120,
        categories = listOf(
            TargetLine("ngk", "NGK", "Nuoc giai khat", 120_000_000),
            TargetLine("bk", "BK", "Banh keo", null),
        ),
        products = listOf(
            TargetLine("coca", "NGK001", "Coca-Cola", 45_000_000),
            TargetLine("pepsi", "NGK002", "Pepsi", null),
        ),
    )

    private val repository = mockk<TargetRepository>()
    private val auth = mockk<AuthRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { auth.team } returns flowOf(listOf(rep))
        coEvery { repository.sheet(any(), any()) } returns DataResult.Success(sheet)
        coEvery { repository.save(any(), any(), any(), any(), any(), any()) } returns DataResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = TargetsViewModel(repository, auth)

    @Test
    fun `opens on the first rep and fills the form from the sheet`() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals("rep-1", s.repId)
        assertEquals("150000000", s.revenue)
        assertEquals("120", s.orders)
        assertEquals(mapOf("ngk" to "120000000", "bk" to ""), s.categories)
        // Only the products that have a target are on the form.
        assertEquals(mapOf("coca" to "45000000"), s.products)
    }

    @Test
    fun `typed amounts keep digits only, without leading zeros`() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.setRevenue("0012.500.000đ")
        assertEquals("12500000", vm.state.value.revenue)
        vm.setOrders("1234567890")
        assertEquals("123456", vm.state.value.orders)
    }

    @Test
    fun `save sends only the figures above zero`() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.setCategory("ngk", "0")
        vm.setCategory("bk", "15000000")
        vm.addProduct("pepsi")
        vm.removeProduct("coca")

        val categories = slot<Map<String, Long>>()
        val products = slot<Map<String, Long>>()
        vm.save()
        advanceUntilIdle()

        coVerify {
            repository.save("rep-1", month, 150_000_000, 120, capture(categories), capture(products))
        }
        assertEquals(mapOf("bk" to 15_000_000L), categories.captured)
        // Added but left empty: no target.
        assertEquals(emptyMap<String, Long>(), products.captured)
        assertEquals("Đã lưu chỉ tiêu", vm.state.value.message)
    }

    @Test
    fun `a refused save says why and keeps the form`() = runTest(dispatcher) {
        coEvery { repository.save(any(), any(), any(), any(), any(), any()) } returns
            DataResult.Failure(AppError.Rule("server", "tháng đã qua, không sửa chỉ tiêu được"))
        val vm = newViewModel()
        advanceUntilIdle()
        vm.setRevenue("1")

        vm.save()
        advanceUntilIdle()

        assertEquals("tháng đã qua, không sửa chỉ tiêu được", vm.state.value.error)
        assertEquals("1", vm.state.value.revenue)
        assertNull(vm.state.value.message)
    }

    @Test
    fun `changing month reloads that month`() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.shiftMonth(-1)
        advanceUntilIdle()

        coVerify { repository.sheet(LocalDate.of(2026, 10, 1), "rep-1") }
    }

    @Test
    fun `amounts show grouped by thousands`() {
        assertEquals("1.250.000", ThousandsTransformation.filter(AnnotatedString("1250000")).text.text)
        assertEquals("999", ThousandsTransformation.filter(AnnotatedString("999")).text.text)
        assertEquals("", ThousandsTransformation.filter(AnnotatedString("")).text.text)
    }

    @Test
    fun `the cursor maps across the separators`() {
        val mapping = ThousandsTransformation.filter(AnnotatedString("1250000")).offsetMapping
        // "1250000" -> "1.250.000"
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1))
        assertEquals(9, mapping.originalToTransformed(7))
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(7, mapping.transformedToOriginal(9))
    }
}
