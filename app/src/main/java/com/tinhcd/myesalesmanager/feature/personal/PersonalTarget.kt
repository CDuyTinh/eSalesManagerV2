package com.tinhcd.myesalesmanager.feature.personal

import androidx.annotation.StringRes
import com.tinhcd.myesalesmanager.R

/** The manager's own work, reached from the Khác tab and the bell. */
enum class PersonalTarget(@StringRes val title: Int, @StringRes val subtitle: Int) {
    WORK_DAY(R.string.personal_work_day, R.string.personal_work_day_sub),
    LEAVE(R.string.personal_leave, R.string.personal_leave_sub),
    NOTES(R.string.personal_notes, R.string.personal_notes_sub),
    NOTIFICATIONS(R.string.personal_notifications, R.string.personal_notifications_sub),
    PASSWORD(R.string.personal_password, R.string.personal_password_sub),
    SETTINGS(R.string.personal_settings, R.string.personal_settings_sub),
}
