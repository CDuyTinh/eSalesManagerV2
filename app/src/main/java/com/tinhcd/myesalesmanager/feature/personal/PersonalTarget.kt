package com.tinhcd.myesalesmanager.feature.personal

/** The manager's own work, reached from the Khác tab and the bell. */
enum class PersonalTarget(val title: String, val subtitle: String) {
    WORK_DAY("Chấm công", "Vào / ra ngày làm việc"),
    LEAVE("Nghỉ phép của tôi", "Xin nghỉ, theo dõi đơn"),
    NOTES("Ghi chú công việc", "Việc cần làm của tôi"),
    NOTIFICATIONS("Thông báo", "Yêu cầu chờ duyệt, kết quả"),
}
