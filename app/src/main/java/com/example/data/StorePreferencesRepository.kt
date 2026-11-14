package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CurrencyOption
import com.example.model.InvoiceTemplate
import com.example.model.ProActivationValidator
import com.example.model.StoreProfile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "faturati_prefs")

class StorePreferencesRepository(private val context: Context) {

  private object Keys {
    val STORE_NAME = stringPreferencesKey("store_name")
    val STORE_PHONE = stringPreferencesKey("store_phone")
    val LOGO_FILE_PATH = stringPreferencesKey("logo_file_path")
    val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    val IS_PRO_ACTIVATED = booleanPreferencesKey("is_pro_activated")
    val SELECTED_TEMPLATE = stringPreferencesKey("selected_template")
  }

  val storeProfileFlow: Flow<StoreProfile> = context.dataStore.data.map { prefs ->
    val savedLogo = prefs[Keys.LOGO_FILE_PATH]
    val existingLogo = savedLogo?.takeIf { File(it).exists() }
    val isPro = prefs[Keys.IS_PRO_ACTIVATED] ?: false
    val savedTemplate = InvoiceTemplate.fromId(prefs[Keys.SELECTED_TEMPLATE])
    val effectiveTemplate = if (savedTemplate.isPro && !isPro) InvoiceTemplate.CLASSIC else savedTemplate

    StoreProfile(
      storeName = prefs[Keys.STORE_NAME] ?: "",
      storePhone = prefs[Keys.STORE_PHONE] ?: "",
      logoFilePath = existingLogo,
      currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: CurrencyOption.LYD.symbol,
      isProActivated = isPro,
      selectedTemplate = effectiveTemplate,
    )
  }

  suspend fun updateStoreName(name: String) {
    context.dataStore.edit { it[Keys.STORE_NAME] = name }
  }

  suspend fun updateStorePhone(phone: String) {
    context.dataStore.edit { it[Keys.STORE_PHONE] = phone }
  }

  suspend fun updateCurrency(symbol: String) {
    context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
  }

  suspend fun selectTemplate(template: InvoiceTemplate) {
    context.dataStore.edit { it[Keys.SELECTED_TEMPLATE] = template.id }
  }

  suspend fun activateWithCode(code: String): Boolean {
    if (!ProActivationValidator.isCodeValid(code)) return false
    context.dataStore.edit { it[Keys.IS_PRO_ACTIVATED] = true }
    return true
  }

  suspend fun deactivatePro() {
    context.dataStore.edit {
      it[Keys.IS_PRO_ACTIVATED] = false
      it[Keys.SELECTED_TEMPLATE] = InvoiceTemplate.CLASSIC.id
    }
  }

  suspend fun saveLogoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
      val bitmap = decodeScaledBitmap(uri, maxSize = 512) ?: return@withContext null
      val logoFile = File(context.filesDir, "store_logo_${System.currentTimeMillis()}.png")
      // Clean up older logo files
      context.filesDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("store_logo_") && file.name.endsWith(".png")) {
          file.delete()
        }
      }
      FileOutputStream(logoFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
      }
      bitmap.recycle()
      context.dataStore.edit { it[Keys.LOGO_FILE_PATH] = logoFile.absolutePath }
      logoFile.absolutePath
    }.getOrNull()
  }

  suspend fun removeLogo() {
    withContext(Dispatchers.IO) {
      context.filesDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("store_logo_") && file.name.endsWith(".png")) {
          file.delete()
        }
      }
    }
    context.dataStore.edit { it.remove(Keys.LOGO_FILE_PATH) }
  }

  @Suppress("DEPRECATION")
  private fun decodeScaledBitmap(uri: Uri, maxSize: Int): Bitmap? {
    val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val w = info.size.width
        val h = info.size.height
        val maxDim = maxOf(w, h)
        if (maxDim > maxSize) {
          val ratio = maxSize.toFloat() / maxDim.toFloat()
          decoder.setTargetSize((w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1))
        }
      }
    } else {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    } ?: return null

    val maxDim = maxOf(rawBitmap.width, rawBitmap.height)
    if (maxDim <= maxSize) return rawBitmap
    val ratio = maxSize.toFloat() / maxDim.toFloat()
    val scaled = Bitmap.createScaledBitmap(
      rawBitmap,
      (rawBitmap.width * ratio).toInt().coerceAtLeast(1),
      (rawBitmap.height * ratio).toInt().coerceAtLeast(1),
      true,
    )
    if (scaled != rawBitmap) rawBitmap.recycle()
    return scaled
  }
}
