package com.tinhcd.myesalesmanager.feature.approval

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tinhcd.myesalesmanager.core.ui.theme.MyeSalesTheme
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The approval card and the decision dialog, on Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "vi")
class ApprovalUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a pending request offers approve and reject`() {
        var approved = 0
        var rejected = 0
        compose.setContent {
            MyeSalesTheme {
                ApprovalCard(ApprovalStatus.PENDING, decisionNote = null, onApprove = { approved++ }, onReject = { rejected++ }) {
                    Text("Tran Van Nam")
                }
            }
        }

        compose.onNodeWithText("Duyệt").performClick()
        compose.onNodeWithText("Từ chối").performClick()

        assertEquals(1, approved)
        assertEquals(1, rejected)
    }

    @Test
    fun `a decided request shows its note and no buttons`() {
        compose.setContent {
            MyeSalesTheme {
                ApprovalCard(ApprovalStatus.APPROVED, decisionNote = "Đồng ý", onApprove = {}, onReject = {}) {
                    Text("Tran Van Nam")
                }
            }
        }

        compose.onNodeWithText("Ghi chú duyệt: Đồng ý").assertIsDisplayed()
        compose.onNodeWithText("Duyệt").assertDoesNotExist()
    }

    @Test
    fun `rejecting needs a reason before it can be confirmed`() {
        var confirmed: String? = null
        compose.setContent {
            MyeSalesTheme {
                DecisionDialog(
                    pending = PendingDecision("l1", approve = false, title = "Tran Van Nam nghỉ 20/11/2026"),
                    busy = false,
                    onConfirm = { note, _ -> confirmed = note },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Xác nhận").assertIsNotEnabled()
        compose.onNodeWithText("Lý do từ chối *").performTextInput("Trùng lịch")
        compose.onNodeWithText("Xác nhận").assertIsEnabled().performClick()

        assertEquals("Trùng lịch", confirmed)
    }
}
