package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.InvoiceDraft
import com.example.model.InvoiceTemplate
import com.example.model.StoreProfile
import com.example.model.formatAmount
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ShareTarget {
  SYSTEM,
  WHATSAPP,
  FACEBOOK,
}

sealed interface GallerySaveOutcome {
  data class SavedMediaStore(val uri: Uri) : GallerySaveOutcome
  data class RequiresSafCreateDocument(val fileName: String, val tempFileUri: Uri) : GallerySaveOutcome
}

object InvoiceBitmapRenderer {

  private const val CANVAS_WIDTH = 1200
  private const val PADDING = 64f

  suspend fun renderInvoiceBitmap(
    context: Context,
    storeProfile: StoreProfile,
    draft: InvoiceDraft,
  ): Bitmap = withContext(Dispatchers.IO) {
    val template = if (storeProfile.selectedTemplate.isPro && !storeProfile.isProActivated) {
      InvoiceTemplate.CLASSIC
    } else {
      storeProfile.selectedTemplate
    }

    val cairoTypeface = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
    val tajawalTypeface = ResourcesCompat.getFont(context, R.font.tajawal) ?: Typeface.DEFAULT

    val storeTitle = storeProfile.storeName.trim().ifBlank {
      context.getString(R.string.invoice_default_store_name)
    }
    val itemsToRender = draft.items.filter { it.name.isNotBlank() || it.unitPrice > 0.0 }
      .ifEmpty { draft.items }

    val contentWidth = CANVAS_WIDTH - (PADDING * 2)

    // Pre-measure dynamic heights so the bitmap fits the invoice cleanly
    val itemNameColWidth = (contentWidth * 0.43f).toInt()
    val itemNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = tajawalTypeface
      textSize = 31f
      color = Color.parseColor("#18201C")
    }

    val itemLayouts = itemsToRender.mapIndexed { idx, item ->
      val label = item.name.trim().ifBlank { "${context.getString(R.string.invoice_default_item_name)} ${idx + 1}" }
      buildRtlLayout(label, itemNamePaint, itemNameColWidth, Layout.Alignment.ALIGN_NORMAL)
    }

    val notesPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = tajawalTypeface
      textSize = 28f
      color = Color.parseColor("#475569")
    }
    val notesLayout = if (draft.notes.isNotBlank()) {
      buildRtlLayout(draft.notes.trim(), notesPaint, (contentWidth - 48f).toInt(), Layout.Alignment.ALIGN_NORMAL)
    } else null

    val headerHeight = when (template) {
      InvoiceTemplate.LUXURY -> 320f
      InvoiceTemplate.TECH -> 300f
      InvoiceTemplate.BOUTIQUE -> 310f
      InvoiceTemplate.CLASSIC -> 290f
    }
    val metaSectionHeight = 165f
    val tableHeaderHeight = 76f
    val rowsHeight = itemLayouts.sumOf { maxOf(82, it.height + 34) }.toFloat()
    val extraRowsCount = (if (draft.deliveryFee > 0.0) 1 else 0) +
      (if (draft.discount > 0.0) 1 else 0) +
      (if (draft.deliveryFee > 0.0 || draft.discount > 0.0) 1 else 0)
    val summaryHeight = 155f + (extraRowsCount * 54f)
    val notesHeight = notesLayout?.let { it.height + 68f } ?: 0f
    val watermarkHeight = if (!storeProfile.isProActivated) 110f else 56f

    val totalHeight = (headerHeight + 36f + metaSectionHeight + 28f + tableHeaderHeight +
      rowsHeight + 36f + summaryHeight + 28f + notesHeight + watermarkHeight + 36f).toInt()
      .coerceAtLeast(1200)

    val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Paper background
    val paperColor = template.paperBgHex.toIntColor()
    val headerBgColor = template.headerBgHex.toIntColor()
    val accentColor = template.accentHex.toIntColor()
    val headerTextColor = template.headerTextHex.toIntColor()

    canvas.drawColor(paperColor)

    // Outer subtle border for Boutique / Classic
    if (template == InvoiceTemplate.BOUTIQUE) {
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.argb(55, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
      }
      canvas.drawRoundRect(RectF(24f, 24f, CANVAS_WIDTH - 24f, totalHeight - 24f), 28f, 28f, borderPaint)
    }

    // 1. Header Banner
    val headerRect = RectF(0f, 0f, CANVAS_WIDTH.toFloat(), headerHeight)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    if (template == InvoiceTemplate.TECH) {
      headerPaint.shader = LinearGradient(
        0f, 0f, CANVAS_WIDTH.toFloat(), headerHeight,
        headerBgColor, accentColor, Shader.TileMode.CLAMP
      )
    } else {
      headerPaint.color = headerBgColor
    }
    canvas.drawRect(headerRect, headerPaint)

    // Decorative accent bar below header for Luxury & Boutique
    if (template == InvoiceTemplate.LUXURY || template == InvoiceTemplate.BOUTIQUE) {
      val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
      canvas.drawRect(0f, headerHeight - 10f, CANVAS_WIDTH.toFloat(), headerHeight, stripePaint)
    }

    // Store Logo (Top-Right in RTL)
    val logoSize = 148f
    val logoRight = CANVAS_WIDTH - PADDING
    val logoLeft = logoRight - logoSize
    val logoTop = (headerHeight - logoSize) / 2f
    val logoRect = RectF(logoLeft, logoTop, logoRight, logoTop + logoSize)

    val logoBitmap = storeProfile.logoFilePath?.let { path ->
      runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
    if (logoBitmap != null) {
      drawRoundedLogo(canvas, logoBitmap, logoRect, accentColor)
      logoBitmap.recycle()
    } else {
      // Crisp monogram badge if no logo uploaded
      val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 255, 255, 255)
      }
      canvas.drawRoundRect(logoRect, 34f, 34f, badgeBg)
      val initialChar = storeTitle.firstOrNull()?.toString() ?: "ف"
      val initialPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = cairoTypeface
        textSize = 62f
        color = headerTextColor
        textAlign = Paint.Align.CENTER
      }
      val fm = initialPaint.fontMetrics
      val centerY = logoRect.centerY() - (fm.ascent + fm.descent) / 2f
      canvas.drawText(initialChar, logoRect.centerX(), centerY, initialPaint)
    }

    // Store Name & Phone (Next to Logo, aligned right in RTL)
    val storeTextRight = logoLeft - 32f
    val storeTextWidth = (storeTextRight - 400f).toInt().coerceAtLeast(320)
    val storeNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 48f
      color = headerTextColor
    }
    val storeNameLayout = buildRtlLayout(storeTitle, storeNamePaint, storeTextWidth, Layout.Alignment.ALIGN_NORMAL)
    val storeNameY = logoTop + 18f
    drawLayoutAt(canvas, storeNameLayout, storeTextRight - storeTextWidth, storeNameY)

    if (storeProfile.storePhone.isNotBlank()) {
      val phonePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tajawalTypeface
        textSize = 28f
        color = Color.argb(215, Color.red(headerTextColor), Color.green(headerTextColor), Color.blue(headerTextColor))
      }
      val phoneLayout = buildRtlLayout(
        storeProfile.storePhone.trim(),
        phonePaint,
        storeTextWidth,
        Layout.Alignment.ALIGN_NORMAL
      )
      drawLayoutAt(canvas, phoneLayout, storeTextRight - storeTextWidth, storeNameY + storeNameLayout.height + 10f)
    }

    // "فاتورة مبيعات" badge on the left side of the header
    val docTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 36f
      color = if (template == InvoiceTemplate.LUXURY) accentColor else headerTextColor
    }
    val docTitleLayout = buildRtlLayout(
      context.getString(R.string.invoice_doc_title),
      docTitlePaint,
      320,
      Layout.Alignment.ALIGN_OPPOSITE
    )
    drawLayoutAt(canvas, docTitleLayout, PADDING, headerHeight / 2f - docTitleLayout.height / 2f)

    // 2. Meta Card (Customer, Invoice Number, Date)
    var currentY = headerHeight + 36f
    val metaRect = RectF(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY + metaSectionHeight)
    val metaBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = when (template) {
        InvoiceTemplate.LUXURY -> Color.parseColor("#F5F0E6")
        InvoiceTemplate.BOUTIQUE -> Color.parseColor("#FBEFE9")
        InvoiceTemplate.TECH -> Color.parseColor("#EFF6FF")
        InvoiceTemplate.CLASSIC -> Color.parseColor("#F1F5F3")
      }
    }
    canvas.drawRoundRect(metaRect, 22f, 22f, metaBgPaint)

    val metaLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 27f
      color = Color.parseColor("#475569")
    }
    val metaValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 30f
      color = Color.parseColor("#0F172A")
    }

    val halfWidth = ((contentWidth - 64f) / 2f).toInt()
    val rightColX = CANVAS_WIDTH - PADDING - 28f - halfWidth
    val leftColX = PADDING + 28f

    val customerDisplay = draft.customerName.trim().ifBlank { "عميل المتجر" }
    val customerText = "${context.getString(R.string.invoice_doc_customer)} $customerDisplay"
    val customerLayout = buildRtlLayout(customerText, metaValuePaint, halfWidth, Layout.Alignment.ALIGN_NORMAL)
    drawLayoutAt(canvas, customerLayout, rightColX, currentY + 34f)

    val invNumText = "${context.getString(R.string.invoice_doc_number)} ${draft.invoiceNumber}"
    val invNumLayout = buildRtlLayout(invNumText, metaValuePaint, halfWidth, Layout.Alignment.ALIGN_OPPOSITE)
    drawLayoutAt(canvas, invNumLayout, leftColX, currentY + 26f)

    val dateText = "${context.getString(R.string.invoice_doc_date)} ${draft.dateFormatted}"
    val dateLayout = buildRtlLayout(dateText, metaLabelPaint, halfWidth, Layout.Alignment.ALIGN_OPPOSITE)
    drawLayoutAt(canvas, dateLayout, leftColX, currentY + 88f)

    // 3. Products Table Header
    currentY += metaSectionHeight + 28f
    val tableHeaderRect = RectF(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY + tableHeaderHeight)
    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = accentColor
    }
    canvas.drawRoundRect(tableHeaderRect, 18f, 18f, tableHeaderPaint)

    val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 29f
      color = Color.WHITE
    }

    val col1W = itemNameColWidth
    val col2W = (contentWidth * 0.14f).toInt()
    val col3W = (contentWidth * 0.20f).toInt()
    val col4W = (contentWidth * 0.23f).toInt()

    val col1X = CANVAS_WIDTH - PADDING - 24f - col1W
    val col2X = col1X - col2W
    val col3X = col2X - col3W
    val col4X = PADDING + 24f

    val th1 = buildRtlLayout(context.getString(R.string.invoice_col_item), thPaint, col1W, Layout.Alignment.ALIGN_NORMAL)
    val th2 = buildRtlLayout(context.getString(R.string.invoice_col_qty), thPaint, col2W, Layout.Alignment.ALIGN_CENTER)
    val th3 = buildRtlLayout(context.getString(R.string.invoice_col_price), thPaint, col3W, Layout.Alignment.ALIGN_CENTER)
    val th4 = buildRtlLayout(context.getString(R.string.invoice_col_total), thPaint, col4W, Layout.Alignment.ALIGN_OPPOSITE)

    val thCenterY = currentY + (tableHeaderHeight - th1.height) / 2f
    drawLayoutAt(canvas, th1, col1X, thCenterY)
    drawLayoutAt(canvas, th2, col2X, thCenterY)
    drawLayoutAt(canvas, th3, col3X, thCenterY)
    drawLayoutAt(canvas, th4, col4X, thCenterY)

    currentY += tableHeaderHeight

    // 4. Table Rows
    val rowAltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(10, 15, 23, 42)
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#E2E8F0")
      strokeWidth = 2f
    }
    val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = tajawalTypeface
      textSize = 30f
      color = Color.parseColor("#334155")
    }
    val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 30f
      color = Color.parseColor("#0F172A")
    }

    itemsToRender.forEachIndexed { idx, item ->
      val nameLayout = itemLayouts[idx]
      val rowHeight = maxOf(82, nameLayout.height + 34).toFloat()
      if (idx % 2 == 1) {
        canvas.drawRect(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY + rowHeight, rowAltPaint)
      }

      val qtyLayout = buildRtlLayout(item.quantity.toString(), cellPaint, col2W, Layout.Alignment.ALIGN_CENTER)
      val priceLayout = buildRtlLayout(
        formatAmount(item.unitPrice, storeProfile.currencySymbol),
        cellPaint,
        col3W,
        Layout.Alignment.ALIGN_CENTER
      )
      val totalLayout = buildRtlLayout(
        formatAmount(item.lineTotal, storeProfile.currencySymbol),
        cellBoldPaint,
        col4W,
        Layout.Alignment.ALIGN_OPPOSITE
      )

      drawLayoutAt(canvas, nameLayout, col1X, currentY + (rowHeight - nameLayout.height) / 2f)
      drawLayoutAt(canvas, qtyLayout, col2X, currentY + (rowHeight - qtyLayout.height) / 2f)
      drawLayoutAt(canvas, priceLayout, col3X, currentY + (rowHeight - priceLayout.height) / 2f)
      drawLayoutAt(canvas, totalLayout, col4X, currentY + (rowHeight - totalLayout.height) / 2f)

      currentY += rowHeight
      canvas.drawLine(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY, dividerPaint)
    }

    // 5. Totals & Summary Box
    currentY += 36f
    val summaryRect = RectF(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY + summaryHeight)
    val summaryBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = when (template) {
        InvoiceTemplate.LUXURY -> Color.parseColor("#0F172A")
        InvoiceTemplate.BOUTIQUE -> Color.parseColor("#FBEFE9")
        InvoiceTemplate.TECH -> Color.parseColor("#EFF6FF")
        InvoiceTemplate.CLASSIC -> Color.parseColor("#ECFDF5")
      }
    }
    canvas.drawRoundRect(summaryRect, 24f, 24f, summaryBgPaint)

    val isDarkSummary = template == InvoiceTemplate.LUXURY
    val sumLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = tajawalTypeface
      textSize = 30f
      color = if (isDarkSummary) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
    }
    val sumValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 31f
      color = if (isDarkSummary) Color.WHITE else Color.parseColor("#0F172A")
    }

    var sumY = currentY + 28f
    val sumWidth = (contentWidth - 64f).toInt()
    val sumX = PADDING + 32f

    if (draft.deliveryFee > 0.0 || draft.discount > 0.0) {
      drawSummaryLine(
        canvas,
        context.getString(R.string.subtotal_title),
        formatAmount(draft.subtotal, storeProfile.currencySymbol),
        sumLabelPaint,
        sumValPaint,
        sumX,
        sumY,
        sumWidth,
      )
      sumY += 54f
    }

    if (draft.deliveryFee > 0.0) {
      drawSummaryLine(
        canvas,
        context.getString(R.string.delivery_fee_label),
        "+ ${formatAmount(draft.deliveryFee, storeProfile.currencySymbol)}",
        sumLabelPaint,
        sumValPaint,
        sumX,
        sumY,
        sumWidth,
      )
      sumY += 54f
    }

    if (draft.discount > 0.0) {
      drawSummaryLine(
        canvas,
        context.getString(R.string.discount_label),
        "- ${formatAmount(draft.discount, storeProfile.currencySymbol)}",
        sumLabelPaint,
        sumValPaint,
        sumX,
        sumY,
        sumWidth,
      )
      sumY += 54f
    }

    val grandLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 38f
      color = if (isDarkSummary) accentColor else headerBgColor
    }
    val grandValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = cairoTypeface
      textSize = 44f
      color = if (isDarkSummary) Color.WHITE else headerBgColor
    }

    drawSummaryLine(
      canvas,
      context.getString(R.string.grand_total_title),
      formatAmount(draft.grandTotal, storeProfile.currencySymbol),
      grandLabelPaint,
      grandValuePaint,
      sumX,
      sumY + 8f,
      sumWidth,
    )

    currentY += summaryHeight

    // 6. Notes Box (if present)
    if (notesLayout != null) {
      currentY += 28f
      val notesRect = RectF(PADDING, currentY, CANVAS_WIDTH - PADDING, currentY + notesHeight)
      val notesBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(14, 15, 23, 42)
      }
      canvas.drawRoundRect(notesRect, 18f, 18f, notesBg)
      drawLayoutAt(canvas, notesLayout, PADDING + 24f, currentY + 28f)
      currentY += notesHeight
    }

    // 7. Conditional App Watermark (Removed completely when Pro is activated!)
    if (!storeProfile.isProActivated) {
      val wmY = totalHeight - 86f
      val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tajawalTypeface
        textSize = 25f
        color = Color.parseColor("#64748B")
      }
      val wmLayout = buildRtlLayout(
        context.getString(R.string.invoice_watermark_text),
        wmPaint,
        contentWidth.toInt(),
        Layout.Alignment.ALIGN_CENTER
      )
      drawLayoutAt(canvas, wmLayout, PADDING, wmY)
    }

    bitmap
  }

  suspend fun exportInvoiceToCacheUri(
    context: Context,
    storeProfile: StoreProfile,
    draft: InvoiceDraft,
  ): Uri = withContext(Dispatchers.IO) {
    val bitmap = renderInvoiceBitmap(context, storeProfile, draft)
    val exportDir = File(context.cacheDir, "invoices").apply { mkdirs() }
    // Keep cache clean
    exportDir.listFiles()?.forEach { it.delete() }
    val safeNumber = draft.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val outFile = File(exportDir, "invoice_${safeNumber}_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { stream ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    bitmap.recycle()
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
  }

  suspend fun saveInvoiceToGallery(
    context: Context,
    storeProfile: StoreProfile,
    draft: InvoiceDraft,
  ): GallerySaveOutcome = withContext(Dispatchers.IO) {
    val safeNumber = draft.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val fileName = "Faturati_${safeNumber}_${System.currentTimeMillis()}.png"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val bitmap = renderInvoiceBitmap(context, storeProfile, draft)
      val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Faturati")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Unable to insert MediaStore record")
      resolver.openOutputStream(uri)?.use { out: OutputStream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }
      bitmap.recycle()
      values.clear()
      values.put(MediaStore.Images.Media.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      GallerySaveOutcome.SavedMediaStore(uri)
    } else {
      val tempUri = exportInvoiceToCacheUri(context, storeProfile, draft)
      GallerySaveOutcome.RequiresSafCreateDocument(fileName, tempUri)
    }
  }

  fun shareInvoiceImage(context: Context, uri: Uri, target: ShareTarget) {
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val targetPackages = when (target) {
      ShareTarget.WHATSAPP -> listOf("com.whatsapp", "com.whatsapp.w4b")
      ShareTarget.FACEBOOK -> listOf("com.facebook.katana", "com.facebook.orca")
      ShareTarget.SYSTEM -> emptyList()
    }

    val matchedPackage = targetPackages.firstOrNull { pkg -> isPackageInstalled(context, pkg) }
    if (matchedPackage != null) {
      val directIntent = Intent(baseIntent).apply { setPackage(matchedPackage) }
      context.startActivity(directIntent)
    } else {
      val chooser = Intent.createChooser(baseIntent, context.getString(R.string.share_chooser_title))
      context.startActivity(chooser)
    }
  }

  private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
      }
      true
    } catch (_: PackageManager.NameNotFoundException) {
      false
    }
  }

  private fun drawSummaryLine(
    canvas: Canvas,
    label: String,
    value: String,
    labelPaint: TextPaint,
    valuePaint: TextPaint,
    x: Float,
    y: Float,
    width: Int,
  ) {
    val labelLayout = buildRtlLayout(label, labelPaint, (width * 0.55f).toInt(), Layout.Alignment.ALIGN_NORMAL)
    val valueLayout = buildRtlLayout(value, valuePaint, (width * 0.45f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
    drawLayoutAt(canvas, labelLayout, x + (width * 0.45f), y)
    drawLayoutAt(canvas, valueLayout, x, y)
  }

  private fun drawRoundedLogo(
    canvas: Canvas,
    logo: Bitmap,
    rect: RectF,
    borderColor: Int,
  ) {
    val targetSize = rect.width().toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(logo, targetSize, targetSize, true)
    val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val matrix = android.graphics.Matrix()
    matrix.setTranslate(rect.left, rect.top)
    shader.setLocalMatrix(matrix)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.shader = shader
    }
    canvas.drawRoundRect(rect, 32f, 32f, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 5f
      color = Color.argb(180, 255, 255, 255)
    }
    canvas.drawRoundRect(rect, 32f, 32f, borderPaint)
    if (scaled != logo) scaled.recycle()
  }

  private fun buildRtlLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    alignment: Layout.Alignment,
  ): StaticLayout {
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
      .setAlignment(alignment)
      .setTextDirection(TextDirectionHeuristics.RTL)
      .setLineSpacing(0f, 1.12f)
      .setIncludePad(false)
      .build()
  }

  private fun drawLayoutAt(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun Long.toIntColor(): Int = this.toInt()
}
