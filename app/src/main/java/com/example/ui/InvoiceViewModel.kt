package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.StorePreferencesRepository
import com.example.export.GallerySaveOutcome
import com.example.export.InvoiceBitmapRenderer
import com.example.export.ShareTarget
import com.example.model.InvoiceDraft
import com.example.model.InvoiceProductItem
import com.example.model.InvoiceTemplate
import com.example.model.StoreProfile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
  EDITOR,
  PREVIEW,
  PRO,
}

sealed interface ActivationFeedback {
  data class Success(@StringRes val messageRes: Int) : ActivationFeedback
  data class Error(@StringRes val messageRes: Int) : ActivationFeedback
}

sealed interface UiEvent {
  data class ShowSnackbar(@StringRes val messageRes: Int) : UiEvent
  data class LaunchShare(val uri: Uri, val target: ShareTarget) : UiEvent
  data class LaunchSafSave(val suggestedFileName: String, val tempFileUri: Uri) : UiEvent
}

data class InvoiceUiState(
  val currentTab: MainTab = MainTab.EDITOR,
  val storeProfile: StoreProfile = StoreProfile(),
  val draft: InvoiceDraft = InvoiceDraft(),
  val activationCodeInput: String = "",
  val activationFeedback: ActivationFeedback? = null,
  val lockedTemplatePrompt: InvoiceTemplate? = null,
  val isExporting: Boolean = false,
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = StorePreferencesRepository(application.applicationContext)

  private val currentTab = MutableStateFlow(MainTab.EDITOR)
  private val draftState = MutableStateFlow(InvoiceDraft())
  private val activationInput = MutableStateFlow("")
  private val activationFeedback = MutableStateFlow<ActivationFeedback?>(null)
  private val lockedTemplatePrompt = MutableStateFlow<InvoiceTemplate?>(null)
  private val isExporting = MutableStateFlow(false)

  private var nextProductId = 2L

  private val _events = Channel<UiEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  val uiState: StateFlow<InvoiceUiState> = combine(
    currentTab,
    repository.storeProfileFlow,
    draftState,
    combine(activationInput, activationFeedback, lockedTemplatePrompt, isExporting) { code, feedback, locked, exporting ->
      InternalControls(code, feedback, locked, exporting)
    },
  ) { tab, store, draft, controls ->
    InvoiceUiState(
      currentTab = tab,
      storeProfile = store,
      draft = draft,
      activationCodeInput = controls.code,
      activationFeedback = controls.feedback,
      lockedTemplatePrompt = controls.locked,
      isExporting = controls.exporting,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = InvoiceUiState(),
  )

  private data class InternalControls(
    val code: String,
    val feedback: ActivationFeedback?,
    val locked: InvoiceTemplate?,
    val exporting: Boolean,
  )

  fun selectTab(tab: MainTab) {
    currentTab.value = tab
    if (tab != MainTab.PREVIEW) {
      lockedTemplatePrompt.value = null
    }
  }

  fun updateStoreName(name: String) {
    viewModelScope.launch { repository.updateStoreName(name) }
  }

  fun updateStorePhone(phone: String) {
    viewModelScope.launch { repository.updateStorePhone(phone) }
  }

  fun updateCurrency(symbol: String) {
    viewModelScope.launch { repository.updateCurrency(symbol) }
  }

  fun onLogoPicked(uri: Uri) {
    viewModelScope.launch {
      repository.saveLogoFromUri(uri)
    }
  }

  fun removeLogo() {
    viewModelScope.launch {
      repository.removeLogo()
    }
  }

  fun updateCustomerName(name: String) {
    draftState.update { it.copy(customerName = name) }
  }

  fun updateInvoiceNumber(number: String) {
    draftState.update { it.copy(invoiceNumber = number) }
  }

  fun regenerateInvoiceNumber() {
    draftState.update { it.copy(invoiceNumber = InvoiceDraft.generateInvoiceNumber()) }
  }

  fun addProduct() {
    val newId = nextProductId++
    draftState.update { draft ->
      draft.copy(items = draft.items + InvoiceProductItem(id = newId))
    }
  }

  fun removeProduct(itemId: Long) {
    draftState.update { draft ->
      draft.copy(items = draft.items.filterNot { it.id == itemId })
    }
  }

  fun updateProductName(itemId: Long, name: String) {
    draftState.update { draft ->
      draft.copy(
        items = draft.items.map { item ->
          if (item.id == itemId) item.copy(name = name) else item
        }
      )
    }
  }

  fun updateProductPrice(itemId: Long, priceText: String) {
    val sanitized = sanitizeDecimalInput(priceText)
    draftState.update { draft ->
      draft.copy(
        items = draft.items.map { item ->
          if (item.id == itemId) item.copy(unitPriceText = sanitized) else item
        }
      )
    }
  }

  fun incrementProductQty(itemId: Long) {
    draftState.update { draft ->
      draft.copy(
        items = draft.items.map { item ->
          if (item.id == itemId) item.copy(quantity = (item.quantity + 1).coerceAtMost(999)) else item
        }
      )
    }
  }

  fun decrementProductQty(itemId: Long) {
    draftState.update { draft ->
      draft.copy(
        items = draft.items.map { item ->
          if (item.id == itemId) item.copy(quantity = (item.quantity - 1).coerceAtLeast(1)) else item
        }
      )
    }
  }

  fun updateDeliveryFee(value: String) {
    draftState.update { it.copy(deliveryFeeText = sanitizeDecimalInput(value)) }
  }

  fun updateDiscount(value: String) {
    draftState.update { it.copy(discountText = sanitizeDecimalInput(value)) }
  }

  fun updateNotes(notes: String) {
    draftState.update { it.copy(notes = notes) }
  }

  fun selectTemplate(template: InvoiceTemplate) {
    val current = uiState.value
    if (template.isPro && !current.storeProfile.isProActivated) {
      lockedTemplatePrompt.value = template
      return
    }
    lockedTemplatePrompt.value = null
    viewModelScope.launch { repository.selectTemplate(template) }
  }

  fun dismissLockedPrompt() {
    lockedTemplatePrompt.value = null
  }

  fun goToProActivation() {
    lockedTemplatePrompt.value = null
    currentTab.value = MainTab.PRO
  }

  fun updateActivationCode(code: String) {
    activationInput.value = code
    if (activationFeedback.value != null) {
      activationFeedback.value = null
    }
  }

  fun submitActivationCode() {
    val code = activationInput.value.trim()
    if (code.isEmpty()) {
      activationFeedback.value = ActivationFeedback.Error(R.string.activation_error_empty)
      return
    }
    viewModelScope.launch {
      val activated = repository.activateWithCode(code)
      if (activated) {
        activationFeedback.value = ActivationFeedback.Success(R.string.activation_success_message)
        lockedTemplatePrompt.value = null
      } else {
        activationFeedback.value = ActivationFeedback.Error(R.string.activation_error_invalid)
      }
    }
  }

  fun deactivateProDemo() {
    viewModelScope.launch {
      repository.deactivatePro()
      activationFeedback.value = null
      activationInput.value = ""
    }
  }

  fun exportAndShare(target: ShareTarget) {
    val state = uiState.value
    if (state.isExporting) return
    if (state.draft.validItemCount == 0 && state.storeProfile.storeName.isBlank()) {
      viewModelScope.launch {
        _events.send(UiEvent.ShowSnackbar(R.string.export_empty_warning))
      }
      return
    }

    viewModelScope.launch {
      isExporting.value = true
      runCatching {
        InvoiceBitmapRenderer.exportInvoiceToCacheUri(
          context = getApplication(),
          storeProfile = state.storeProfile,
          draft = state.draft,
        )
      }.onSuccess { uri ->
        _events.send(UiEvent.LaunchShare(uri, target))
      }.onFailure {
        _events.send(UiEvent.ShowSnackbar(R.string.export_error_generic))
      }
      isExporting.value = false
    }
  }

  fun saveToGallery() {
    val state = uiState.value
    if (state.isExporting) return
    if (state.draft.validItemCount == 0 && state.storeProfile.storeName.isBlank()) {
      viewModelScope.launch {
        _events.send(UiEvent.ShowSnackbar(R.string.export_empty_warning))
      }
      return
    }

    viewModelScope.launch {
      isExporting.value = true
      runCatching {
        InvoiceBitmapRenderer.saveInvoiceToGallery(
          context = getApplication(),
          storeProfile = state.storeProfile,
          draft = state.draft,
        )
      }.onSuccess { outcome ->
        when (outcome) {
          is GallerySaveOutcome.SavedMediaStore -> {
            _events.send(UiEvent.ShowSnackbar(R.string.export_success_saved))
          }
          is GallerySaveOutcome.RequiresSafCreateDocument -> {
            _events.send(UiEvent.LaunchSafSave(outcome.fileName, outcome.tempFileUri))
          }
        }
      }.onFailure {
        _events.send(UiEvent.ShowSnackbar(R.string.export_error_generic))
      }
      isExporting.value = false
    }
  }

  fun writeTempFileToSafUri(sourceUri: Uri, destUri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      val context = getApplication<Application>()
      runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          context.contentResolver.openOutputStream(destUri)?.use { output ->
            input.copyTo(output)
          }
        }
      }.onSuccess {
        _events.send(UiEvent.ShowSnackbar(R.string.export_success_saved))
      }.onFailure {
        _events.send(UiEvent.ShowSnackbar(R.string.export_error_generic))
      }
    }
  }

  private fun sanitizeDecimalInput(raw: String): String {
    val normalized = raw
      .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
      .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
      .replace('٨', '8').replace('٩', '9').replace('٫', '.').replace(',', '.')
    val builder = StringBuilder()
    var hasDot = false
    for (ch in normalized) {
      if (ch.isDigit()) {
        builder.append(ch)
      } else if (ch == '.' && !hasDot) {
        hasDot = true
        builder.append(ch)
      }
    }
    return builder.toString()
  }
}
