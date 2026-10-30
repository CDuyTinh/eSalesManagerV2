package com.tinhcd.myesalesmanager.domain.model

/**
 * The eight steps of an EDAC coaching day, in order. [perOutlet] steps are
 * recorded outlet by outlet; the others once for the day. Legacy EdacStepIndex.
 */
enum class EdacStep(val index: Int, val title: String, val hint: String, val perOutlet: Boolean) {
    PREPARE(0, "Chuẩn bị", "Đánh giá nhân viên trước khi đi tuyến", false),
    OBSERVE(1, "Quan sát", "Nhân viên bán, giám sát quan sát 8 bước", true),
    EXPLAIN(2, "Giải thích", "Giải thích điểm cần cải thiện", false),
    DEMONSTRATE(3, "Làm mẫu", "Giám sát bán mẫu tại điểm bán", true),
    APPLY(4, "Áp dụng", "Nhân viên áp dụng, giám sát quan sát lại", true),
    EVALUATE(5, "Đánh giá", "Đánh giá từng điểm bán", true),
    END_OF_DAY(6, "Đánh giá cuối ngày", "So kết quả trong ngày với chỉ tiêu", false),
    FINISH(7, "Hoàn tất", "Kết quả huấn luyện và ảnh", false),
    ;

    companion object {
        fun of(index: Int): EdacStep = entries.first { it.index == index }
    }
}

/**
 * Whether the supervisor may open [step] yet. Đánh giá opens with Áp dụng —
 * the legacy app let them run side by side — every other step once the one
 * before is finished.
 */
fun EdacStep.isOpen(currentStep: Int): Boolean =
    if (this == EdacStep.EVALUATE) currentStep >= EdacStep.APPLY.index else currentStep >= index

/** A coaching plan on the supervisor's day, and the training if begun. */
data class EdacSession(
    val planId: String,
    val content: String,
    val fromAt: String,
    val toAt: String,
    val rep: StaffBrief,
    val trainingId: String?,
    val currentStep: Int,
    val completed: Boolean,
)

data class EdacTraining(
    val id: String,
    val trainingDate: String,
    val currentStep: Int,
    val completed: Boolean,
    val checkInAt: String?,
    val checkOutAt: String?,
    val result: String?,
    val supervisorPhotoUrl: String?,
    val salespersonPhotoUrl: String?,
    val rep: StaffBrief,
    val today: TeamToday?,
    val month: TeamMonth?,
    /** Outlets recorded so far on each per-outlet step, by step index. */
    val outletsDone: Map<Int, Int>,
)

data class EdacOutlet(
    val customerId: String,
    val code: String,
    val name: String,
    val address: String?,
    /** On the rep's route that day. */
    val due: Boolean,
    val doneSteps: Set<Int>,
)

enum class EdacKind { SCALE, YES_NO, TEXT }

data class EdacOption(val value: Int, val label: String)

data class EdacAnswer(
    val value: Int? = null,
    val yes: Boolean? = null,
    val text: String? = null,
    val note: String? = null,
)

data class EdacCriterion(
    val id: String,
    val content: String,
    val kind: EdacKind,
    val kpi: String?,
    val required: Boolean,
    val options: List<EdacOption>,
    val answer: EdacAnswer?,
)

/** Step 1's findings, shown at the top of step 2. */
data class EdacObserved(val content: String, val yes: Int, val no: Int)

/** A step's form for the day or for one outlet. */
data class EdacStepForm(
    val step: EdacStep,
    val customer: StaffBrief?,
    val customerAddress: String?,
    val note: String?,
    val criteria: List<EdacCriterion>,
    val observed: List<EdacObserved>,
)

/** One answer in a finished day's summary, already worded by the server. */
data class EdacResultLine(
    val step: Int,
    val customer: String?,
    val content: String,
    val answer: String?,
    val note: String?,
)

data class EdacResult(
    val training: EdacTraining,
    val lines: List<EdacResultLine>,
    /** Làm mẫu notes, per outlet. */
    val notes: List<EdacResultLine>,
)

data class EdacHistoryItem(
    val id: String,
    val trainingDate: String,
    val currentStep: Int,
    val completed: Boolean,
    val result: String?,
    val rep: StaffBrief,
    val outlets: Int,
)
