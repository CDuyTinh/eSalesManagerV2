package com.tinhcd.myesalesmanager.domain.model

/**
 * Which builds head office wants in use, from app_setting
 * (`manager_version_latest`, `manager_version_min`, `manager_update_url`,
 * `manager_update_note`).
 *
 * The app is not published on a store, so it cannot ask one what is current
 * (the legacy's new_version_plus / in_app_update); head office says instead.
 * Version codes, not names: they only ever go up, and "1.10" against "1.9" is
 * a comparison a string gets wrong.
 */
data class AppUpdatePolicy(
    val latestCode: Int?,
    val minCode: Int?,
    /** Where to get the new build; null opens the app's store page. */
    val url: String?,
    /** What changed, in a line or two, for the dialog. */
    val note: String?,
) {
    companion object {
        /** Read from the bootstrap settings; anything missing or unreadable is null. */
        fun from(settings: Map<String, String>) = AppUpdatePolicy(
            latestCode = settings["manager_version_latest"]?.trim()?.toIntOrNull(),
            minCode = settings["manager_version_min"]?.trim()?.toIntOrNull(),
            url = settings["manager_update_url"]?.trim()?.takeIf { it.isNotEmpty() },
            note = settings["manager_update_note"]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun check(currentCode: Int): AppUpdateNeed = when {
        minCode != null && currentCode < minCode -> AppUpdateNeed.REQUIRED
        latestCode != null && currentCode < latestCode -> AppUpdateNeed.OPTIONAL
        else -> AppUpdateNeed.NONE
    }
}

enum class AppUpdateNeed {
    NONE,

    /** A newer build exists; the manager may carry on with this one. */
    OPTIONAL,

    /** This build is below the minimum head office supports: update to continue. */
    REQUIRED,
}
