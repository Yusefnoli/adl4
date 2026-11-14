package com.example

import com.example.model.InvoiceDraft
import com.example.model.InvoiceProductItem
import com.example.model.ProActivationValidator
import com.example.model.formatAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun invoice_totals_calculate_correctly() {
    val draft = InvoiceDraft(
      items = listOf(
        InvoiceProductItem(id = 1L, name = "منتج أ", quantity = 3, unitPriceText = "25.5"),
        InvoiceProductItem(id = 2L, name = "منتج ب", quantity = 2, unitPriceText = "100"),
      ),
      deliveryFeeText = "20",
      discountText = "15.5",
    )

    assertEquals(276.5, draft.subtotal, 0.001)
    assertEquals(281.0, draft.grandTotal, 0.001)
    assertEquals("281 د.ل", formatAmount(draft.grandTotal, "د.ل"))
  }

  @Test
  fun discount_never_makes_grand_total_negative() {
    val draft = InvoiceDraft(
      items = listOf(InvoiceProductItem(id = 1L, name = "منتج", quantity = 1, unitPriceText = "50")),
      discountText = "100",
    )
    assertEquals(0.0, draft.grandTotal, 0.001)
  }

  @Test
  fun activation_code_validator_accepts_libya2026_case_insensitive() {
    assertTrue(ProActivationValidator.isCodeValid("LIBYA2026"))
    assertTrue(ProActivationValidator.isCodeValid("  libya2026 "))
    assertFalse(ProActivationValidator.isCodeValid(""))
    assertFalse(ProActivationValidator.isCodeValid("INVALID"))
  }
}
