package com.example.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

enum class InvoiceTemplate(
  val id: String,
  @StringRes val titleRes: Int,
  val isPro: Boolean,
  val headerBgHex: Long,
  val accentHex: Long,
  val paperBgHex: Long,
  val headerTextHex: Long,
) {
  CLASSIC(
    id = "classic",
    titleRes = R.string.template_classic,
    isPro = false,
    headerBgHex = 0xFF0F5132,
    accentHex = 0xFF198754,
    paperBgHex = 0xFFFFFFFF,
    headerTextHex = 0xFFFFFFFF,
  ),
  LUXURY(
    id = "luxury",
    titleRes = R.string.template_luxury,
    isPro = true,
    headerBgHex = 0xFF0F172A,
    accentHex = 0xFFC89B3C,
    paperBgHex = 0xFFFFFDF8,
    headerTextHex = 0xFFF8FAFC,
  ),
  BOUTIQUE(
    id = "boutique",
    titleRes = R.string.template_boutique,
    isPro = true,
    headerBgHex = 0xFF8C3B2B,
    accentHex = 0xFFD97757,
    paperBgHex = 0xFFFFF9F5,
    headerTextHex = 0xFFFFFFFF,
  ),
  TECH(
    id = "tech",
    titleRes = R.string.template_tech,
    isPro = true,
    headerBgHex = 0xFF1E3A8A,
    accentHex = 0xFF2563EB,
    paperBgHex = 0xFFF8FAFC,
    headerTextHex = 0xFFFFFFFF,
  );

  val headerBgColor: Color get() = Color(headerBgHex)
  val accentColor: Color get() = Color(accentHex)
  val paperBgColor: Color get() = Color(paperBgHex)
  val headerTextColor: Color get() = Color(headerTextHex)

  companion object {
    fun fromId(id: String?): InvoiceTemplate =
      entries.firstOrNull { it.id == id } ?: CLASSIC
  }
}

enum class CurrencyOption(val symbol: String, val label: String) {
  LYD("د.ل", "دينار ليبي"),
  SAR("ر.س", "ريال سعودي"),
  AED("د.إ", "درهم إماراتي"),
  EGP("ج.م", "جنيه مصري"),
  KWD("د.ك", "دينار كويتي"),
  USD("$", "دولار"),
}

data class InvoiceProductItem(
  val id: Long,
  val name: String = "",
  val quantity: Int = 1,
  val unitPriceText: String = "",
) {
  val unitPrice: Double
    get() = unitPriceText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

  val lineTotal: Double
    get() = quantity.coerceAtLeast(1) * unitPrice
}

data class StoreProfile(
  val storeName: String = "",
  val storePhone: String = "",
  val logoFilePath: String? = null,
  val currencySymbol: String = CurrencyOption.LYD.symbol,
  val isProActivated: Boolean = false,
  val selectedTemplate: InvoiceTemplate = InvoiceTemplate.CLASSIC,
)

data class InvoiceDraft(
  val invoiceNumber: String = generateInvoiceNumber(),
  val dateFormatted: String = currentFormattedDate(),
  val customerName: String = "",
  val items: List<InvoiceProductItem> = listOf(InvoiceProductItem(id = 1L)),
  val deliveryFeeText: String = "",
  val discountText: String = "",
  val notes: String = "",
) {
  val subtotal: Double
    get() = items.sumOf { it.lineTotal }

  val deliveryFee: Double
    get() = deliveryFeeText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

  val discount: Double
    get() = discountText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

  val grandTotal: Double
    get() = max(0.0, subtotal + deliveryFee - discount)

  val validItemCount: Int
    get() = items.count { it.name.isNotBlank() || it.unitPrice > 0.0 }

  companion object {
    fun generateInvoiceNumber(): String {
      val num = Random.nextInt(1000, 9999)
      return "INV-$num"
    }

    fun currentFormattedDate(): String {
      val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.US)
      return formatter.format(Date())
    }
  }
}

fun formatAmount(amount: Double, currencySymbol: String): String {
  val formatted = if (amount % 1.0 == 0.0) {
    String.format(Locale.US, "%.0f", amount)
  } else {
    String.format(Locale.US, "%.2f", amount)
  }
  return "$formatted $currencySymbol"
}

object ProActivationValidator {
  const val VALID_CODE = "LIBYA2026"

  fun isCodeValid(input: String): Boolean =
    input.trim().equals(VALID_CODE, ignoreCase = true)
}
