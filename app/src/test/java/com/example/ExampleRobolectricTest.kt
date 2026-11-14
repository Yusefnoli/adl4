package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.export.InvoiceBitmapRenderer
import com.example.model.InvoiceDraft
import com.example.model.InvoiceProductItem
import com.example.model.InvoiceTemplate
import com.example.model.StoreProfile
import com.example.ui.theme.FaturatiTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app_name string matches Faturati`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("فاتورتي", appName)
  }

  @Test
  fun `renderInvoiceBitmap generates 1200px wide bitmap for free and pro templates`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val draft = InvoiceDraft(
      invoiceNumber = "INV-2026",
      customerName = "سالم علي",
      items = listOf(
        InvoiceProductItem(id = 1, name = "ساعة ذكية", quantity = 2, unitPriceText = "150"),
        InvoiceProductItem(id = 2, name = "حقيبة جلدية", quantity = 1, unitPriceText = "95.5"),
      ),
      deliveryFeeText = "15",
      notes = "شكراً لتسوقكم",
    )

    val freeProfile = StoreProfile(
      storeName = "متجر النور",
      isProActivated = false,
      selectedTemplate = InvoiceTemplate.CLASSIC,
    )
    val freeBitmap = InvoiceBitmapRenderer.renderInvoiceBitmap(context, freeProfile, draft)
    assertEquals(1200, freeBitmap.width)
    assertTrue(freeBitmap.height >= 1200)
    freeBitmap.recycle()

    val proProfile = freeProfile.copy(
      isProActivated = true,
      selectedTemplate = InvoiceTemplate.LUXURY,
    )
    val proBitmap = InvoiceBitmapRenderer.renderInvoiceBitmap(context, proProfile, draft)
    assertEquals(1200, proBitmap.width)
    assertTrue(proBitmap.height >= 1200)
    proBitmap.recycle()
  }

  @Test
  fun `pro activation flow unlocks with LIBYA2026 and removes watermark`() {
    composeTestRule.setContent {
      FaturatiTheme {
        FaturatiApp()
      }
    }

    // Check Preview initially shows watermark
    composeTestRule.onNodeWithTag("nav_tab_preview").performClick()
    composeTestRule.onNodeWithTag("invoice_watermark_footer")
      .performScrollTo()
      .assertIsDisplayed()

    // Go to Pro tab and test invalid then valid code
    composeTestRule.onNodeWithTag("nav_tab_pro").performClick()
    composeTestRule.onNodeWithTag("activation_code_input")
      .performScrollTo()
      .performTextInput("WRONG")
    composeTestRule.onNodeWithTag("activate_pro_button")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithTag("activation_error_text")
      .performScrollTo()
      .assertIsDisplayed()

    // Replace with valid code LIBYA2026 and verify unlock + watermark removal
    composeTestRule.onNodeWithTag("activation_code_input")
      .performScrollTo()
      .performTextReplacement("LIBYA2026")
    composeTestRule.onNodeWithTag("activate_pro_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("activation_success_banner")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("activation_success_banner")
      .performScrollTo()
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_preview").performClick()
    composeTestRule.onNodeWithTag("invoice_watermark_footer").assertDoesNotExist()
  }
}
