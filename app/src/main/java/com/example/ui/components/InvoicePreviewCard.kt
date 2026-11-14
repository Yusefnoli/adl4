package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.InvoiceDraft
import com.example.model.InvoiceTemplate
import com.example.model.StoreProfile
import com.example.model.formatAmount
import java.io.File

@Composable
fun InvoicePreviewCard(
  storeProfile: StoreProfile,
  draft: InvoiceDraft,
  modifier: Modifier = Modifier,
) {
  val template = if (storeProfile.selectedTemplate.isPro && !storeProfile.isProActivated) {
    InvoiceTemplate.CLASSIC
  } else {
    storeProfile.selectedTemplate
  }

  val paperColor = template.paperBgColor
  val headerBgColor = template.headerBgColor
  val accentColor = template.accentColor
  val headerTextColor = template.headerTextColor

  val storeTitle = storeProfile.storeName.trim().ifBlank {
    stringResource(R.string.invoice_default_store_name)
  }

  val metaCardColor = when (template) {
    InvoiceTemplate.LUXURY -> Color(0xFFF5F0E6)
    InvoiceTemplate.BOUTIQUE -> Color(0xFFFBEFE9)
    InvoiceTemplate.TECH -> Color(0xFFEFF6FF)
    InvoiceTemplate.CLASSIC -> Color(0xFFF1F5F3)
  }

  val summaryBgColor = when (template) {
    InvoiceTemplate.LUXURY -> Color(0xFF0F172A)
    InvoiceTemplate.BOUTIQUE -> Color(0xFFFBEFE9)
    InvoiceTemplate.TECH -> Color(0xFFEFF6FF)
    InvoiceTemplate.CLASSIC -> Color(0xFFECFDF5)
  }

  val itemsToRender = draft.items.filter { it.name.isNotBlank() || it.unitPrice > 0.0 }
    .ifEmpty { draft.items }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("invoice_preview_card"),
    shape = RoundedCornerShape(18.dp),
    color = paperColor,
    shadowElevation = 6.dp,
    border = BorderStroke(
      width = if (template == InvoiceTemplate.BOUTIQUE) 2.dp else 1.dp,
      color = if (template == InvoiceTemplate.BOUTIQUE) accentColor.copy(alpha = 0.35f) else Color(0xFFE2E8F0),
    ),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header Banner
      val headerModifier = if (template == InvoiceTemplate.TECH) {
        Modifier
          .fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(headerBgColor, accentColor)))
      } else {
        Modifier
          .fillMaxWidth()
          .background(headerBgColor)
      }

      Column(modifier = headerModifier) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!storeProfile.logoFilePath.isNullOrBlank() && File(storeProfile.logoFilePath).exists()) {
              AsyncImage(
                model = File(storeProfile.logoFilePath),
                contentDescription = stringResource(R.string.store_logo_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
              )
            } else {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = storeTitle.firstOrNull()?.toString() ?: "ف",
                  style = MaterialTheme.typography.headlineSmall,
                  color = headerTextColor,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = storeTitle,
                style = MaterialTheme.typography.titleLarge,
                color = headerTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              if (storeProfile.storePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = storeProfile.storePhone.trim(),
                  style = MaterialTheme.typography.bodySmall,
                  color = headerTextColor.copy(alpha = 0.85f),
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = stringResource(R.string.invoice_doc_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (template == InvoiceTemplate.LUXURY) accentColor else headerTextColor,
          )
        }

        if (template == InvoiceTemplate.LUXURY || template == InvoiceTemplate.BOUTIQUE) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .background(accentColor),
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Meta Card
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(metaCardColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.invoice_doc_customer),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF64748B),
            )
            Text(
              text = draft.customerName.trim().ifBlank { "عميل المتجر" },
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF0F172A),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${stringResource(R.string.invoice_doc_number)} ${draft.invoiceNumber}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF0F172A),
            )
            Text(
              text = "${stringResource(R.string.invoice_doc_date)} ${draft.dateFormatted}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF64748B),
            )
          }
        }

        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor)
            .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.invoice_col_item),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.weight(0.42f),
          )
          Text(
            text = stringResource(R.string.invoice_col_qty),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.14f),
          )
          Text(
            text = stringResource(R.string.invoice_col_price),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.20f),
          )
          Text(
            text = stringResource(R.string.invoice_col_total),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.24f),
          )
        }

        // Table Rows
        Column {
          itemsToRender.forEachIndexed { idx, item ->
            val itemName = item.name.trim().ifBlank {
              "${stringResource(R.string.invoice_default_item_name)} ${idx + 1}"
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (idx % 2 == 1) Color(0x0A0F172A) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = itemName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E293B),
                modifier = Modifier.weight(0.42f),
              )
              Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.14f),
              )
              Text(
                text = formatAmount(item.unitPrice, storeProfile.currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.20f),
              )
              Text(
                text = formatAmount(item.lineTotal, storeProfile.currencySymbol),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF0F172A),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.24f),
              )
            }
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
          }
        }

        // Summary Box
        val isDarkSummary = template == InvoiceTemplate.LUXURY
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(summaryBgColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (draft.deliveryFee > 0.0 || draft.discount > 0.0) {
            SummaryRow(
              label = stringResource(R.string.subtotal_title),
              value = formatAmount(draft.subtotal, storeProfile.currencySymbol),
              labelColor = if (isDarkSummary) Color(0xFFCBD5E1) else Color(0xFF475569),
              valueColor = if (isDarkSummary) Color.White else Color(0xFF0F172A),
            )
          }
          if (draft.deliveryFee > 0.0) {
            SummaryRow(
              label = stringResource(R.string.delivery_fee_label),
              value = "+ ${formatAmount(draft.deliveryFee, storeProfile.currencySymbol)}",
              labelColor = if (isDarkSummary) Color(0xFFCBD5E1) else Color(0xFF475569),
              valueColor = if (isDarkSummary) Color.White else Color(0xFF0F172A),
            )
          }
          if (draft.discount > 0.0) {
            SummaryRow(
              label = stringResource(R.string.discount_label),
              value = "- ${formatAmount(draft.discount, storeProfile.currencySymbol)}",
              labelColor = if (isDarkSummary) Color(0xFFCBD5E1) else Color(0xFF475569),
              valueColor = if (isDarkSummary) Color(0xFFFCA5A5) else Color(0xFFBA1A1A),
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.grand_total_title),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isDarkSummary) accentColor else headerBgColor,
            )
            Text(
              text = formatAmount(draft.grandTotal, storeProfile.currencySymbol),
              style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
              color = if (isDarkSummary) Color.White else headerBgColor,
            )
          }
        }

        // Notes
        if (draft.notes.isNotBlank()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x0D0F172A))
              .padding(12.dp),
          ) {
            Text(
              text = draft.notes.trim(),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF475569),
            )
          }
        }

        // Watermark footer (shown only on Free version; completely removed when Pro is activated!)
        if (!storeProfile.isProActivated) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp, bottom = 2.dp)
              .testTag("invoice_watermark_footer"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.ReceiptLong,
              contentDescription = null,
              tint = Color(0xFF64748B),
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.invoice_watermark_text),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF64748B),
            )
          }
        } else {
          // Small invisible/non-intrusive indicator in Pro? No, prompt says remove logo from bottom!
          Spacer(modifier = Modifier.height(2.dp))
        }
      }
    }
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
  labelColor: Color,
  valueColor: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = valueColor,
    )
  }
}
