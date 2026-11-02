package com.tinhcd.myesalesmanager.domain

import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.KpiThresholds
import com.tinhcd.myesalesmanager.domain.model.ProductTarget
import com.tinhcd.myesalesmanager.domain.model.percent
import com.tinhcd.myesalesmanager.feature.report.RangePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DashboardFiguresTest {

    @Test
    fun `percent rounds to the nearest whole`() {
        assertEquals(71, percent(162_654_600, 230_000_000))
        assertEquals(100, percent(5, 5))
        assertEquals(200, percent(2, 1))
    }

    @Test
    fun `no target is no percentage, not zero`() {
        assertNull(percent(10, null))
        assertNull(percent(10, 0))
    }

    @Test
    fun `a product target's achievement is sold over target`() {
        assertEquals(90, ProductTarget(isGroup = true, name = "NGK", target = 180_000_000, actual = 162_654_600).achievement)
    }

    @Test
    fun `thresholds colour by band, edges inclusive`() {
        val kpi = KpiThresholds(green = 100, blue = 70)
        assertEquals(Kpi.Green, kpi.ramp(100))
        assertEquals(Kpi.Blue, kpi.ramp(99))
        assertEquals(Kpi.Blue, kpi.ramp(70))
        assertEquals(Kpi.Orange, kpi.ramp(69))
        assertEquals(Kpi.Slate, kpi.ramp(null))
    }

    @Test
    fun `thresholds read from settings, with the old values as fallback`() {
        assertEquals(KpiThresholds(green = 90, blue = 60),
            KpiThresholds.from(mapOf("dashboard_kpi_green" to "90", "dashboard_kpi_yellow" to "60")))
        assertEquals(KpiThresholds(green = 100, blue = 70), KpiThresholds.from(emptyMap()))
        assertEquals(KpiThresholds(green = 100, blue = 70),
            KpiThresholds.from(mapOf("dashboard_kpi_green" to "x", "dashboard_kpi_yellow" to "")))
    }

    @Test
    fun `a blue threshold above green is held at green`() {
        // Otherwise no figure could ever be blue.
        assertEquals(KpiThresholds(green = 80, blue = 80),
            KpiThresholds.from(mapOf("dashboard_kpi_green" to "80", "dashboard_kpi_yellow" to "95")))
    }

    @Test
    fun `range presets end today, or at the end of last month`() {
        val today = LocalDate.of(2026, 11, 2)
        assertEquals(LocalDate.of(2026, 11, 1) to today, RangePreset.THIS_MONTH.resolve(today))
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), RangePreset.LAST_MONTH.resolve(today))
        assertEquals(LocalDate.of(2026, 10, 27) to today, RangePreset.LAST_7.resolve(today))
        assertEquals(LocalDate.of(2026, 10, 4) to today, RangePreset.LAST_30.resolve(today))
    }

    @Test
    fun `last month from early March is all of February`() {
        assertEquals(LocalDate.of(2028, 2, 1) to LocalDate.of(2028, 2, 29),
            RangePreset.LAST_MONTH.resolve(LocalDate.of(2028, 3, 1)))
    }
}
