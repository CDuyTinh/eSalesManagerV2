package com.tinhcd.myesalesmanager.feature.report

import android.content.Context
import com.tinhcd.myesalesmanager.feature.common.titleRes
import com.tinhcd.myesalesmanager.R
import com.tinhcd.myesalesmanager.feature.common.title
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.core.content.FileProvider
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.domain.model.ActivitiesReport
import com.tinhcd.myesalesmanager.domain.model.OrdersReport
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod
import com.tinhcd.myesalesmanager.domain.model.SalesReport
import com.tinhcd.myesalesmanager.domain.model.SkuReport
import com.tinhcd.myesalesmanager.domain.model.TrackingReport
import com.tinhcd.myesalesmanager.domain.model.TradeReport
import com.tinhcd.myesalesmanager.domain.model.TrainingReport
import com.tinhcd.myesalesmanager.domain.model.WorkReport
import java.io.File

/**
 * The report's main table, header first — what a manager pastes into a
 * spreadsheet or sends up the line. Amounts stay plain numbers so the
 * spreadsheet can add them up.
 */
fun ReportData.table(): List<List<String>> = when (this) {
    is ActivitiesReport -> listOf(
        listOf("Nhân viên", "Mã KH", "Khách hàng", "Trong tuyến", "Trạng thái", "Vào", "Ra", "Phút", "Doanh số"),
    ) + rows.map {
        listOf(
            it.salesperson?.name.orEmpty(), it.customerCode, it.customerName, if (it.planned) "Có" else "Không",
            it.status, formatDateTime(it.checkInAt), formatDateTime(it.checkOutAt), it.minutes?.toString().orEmpty(),
            it.orderAmount.toString(),
        )
    }
    is SalesReport -> listOf(listOf("Mã NV", "Nhân viên", "Số đơn", "Doanh số", "Chỉ tiêu")) +
        members.map { listOf(it.code, it.name, it.orders.toString(), it.revenue.toString(), it.target?.toString().orEmpty()) }
    is OrdersReport -> listOf(listOf("Số đơn", "Ngày", "Nhân viên", "Mã KH", "Khách hàng", "Trạng thái", "Số dòng", "Tổng tiền")) +
        items.map {
            listOf(it.orderNo, it.orderDate, it.salesperson?.name.orEmpty(), it.customerCode, it.customerName, it.status,
                it.lineCount.toString(), it.totalAmount.toString())
        }
    is SkuReport -> listOf(listOf("Mã SP", "Sản phẩm", "ĐVT", "SL đã xác nhận", "SL chờ", "Tổng SL", "Thành tiền")) +
        items.map {
            listOf(it.code, it.name, it.uom, it.qtyConfirmed.toString(), it.qtyPending.toString(), it.qtyTotal.toString(),
                it.amountTotal.toString())
        }
    is TradeReport -> listOf(listOf("Loại", "Nhân viên", "Ngày đăng ký", "Trạng thái", "Suất", "Mã KH", "Khách hàng", "Chương trình", "Mức")) +
        items.map {
            listOf(it.kind, it.salesperson?.name.orEmpty(), it.registeredAt, it.status, it.portion.toString(), it.customerCode,
                it.customerName, "${it.programCode} – ${it.programName}", it.levelName.orEmpty())
        }
    is WorkReport -> listOf(listOf("Nhân viên", "Nội dung", "Loại", "Trạng thái", "Từ", "Đến", "Đối tượng", "Kết quả")) +
        items.map {
            listOf(it.salesperson?.name.orEmpty(), it.content, it.categoryName, it.status, formatDateTime(it.fromAt),
                formatDateTime(it.toAt), it.objectCount.toString(), it.outcome.orEmpty())
        }
    is TrainingReport -> listOf(listOf("Mã NV", "Nhân viên", "Kế hoạch", "Đã huấn luyện", "Chưa", "Điểm")) +
        members.map { listOf(it.code, it.name, it.plans.toString(), it.trained.toString(), it.untrained.toString(), it.score?.toString().orEmpty()) }
    is TrackingReport -> listOf(listOf("Mã NV", "Nhân viên", "Vào", "Ra", "Kế hoạch", "Đã ghé", "Số đơn", "Doanh số", "Pin %")) +
        members.map {
            listOf(it.code, it.name, formatDateTime(it.clockIn), formatDateTime(it.clockOut), it.planned.toString(),
                it.visited.toString(), it.orders.toString(), it.revenue.toString(), it.batteryPct?.toString().orEmpty())
        }
}

/** RFC 4180: quote every cell, double the quotes inside. */
private fun csv(rows: List<List<String>>): String =
    rows.joinToString("\r\n") { row -> row.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } }

/**
 * Writes the report as CSV and opens the share sheet (Zalo, mail, Drive…).
 * The byte-order mark is what makes Excel read the Vietnamese as UTF-8.
 */
fun shareReport(context: Context, kind: ReportKind, period: ReportPeriod, data: ReportData) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val span = if (period.from == period.to) period.from else "${period.from}_${period.to}"
    val file = File(dir, "bao-cao-${kind.code}-$span.csv")
    file.writeText("\uFEFF" + csv(data.table()), Charsets.UTF_8)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "${context.getString(kind.titleRes())} ($span)")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_report)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
