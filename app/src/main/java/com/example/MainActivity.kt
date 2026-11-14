package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.export.InvoiceBitmapRenderer
import com.example.ui.InvoiceUiState
import com.example.ui.InvoiceViewModel
import com.example.ui.MainTab
import com.example.ui.UiEvent
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.PreviewExportScreen
import com.example.ui.screens.ProActivationScreen
import com.example.ui.theme.FaturatiTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FaturatiTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          FaturatiApp()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturatiApp(
  viewModel: InvoiceViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var pendingSafSourceUri by remember { mutableStateOf<Uri?>(null) }
  val safCreateDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("image/png"),
  ) { destUri ->
    val sourceUri = pendingSafSourceUri
    pendingSafSourceUri = null
    if (destUri != null && sourceUri != null) {
      viewModel.writeTempFileToSafUri(sourceUri, destUri)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is UiEvent.ShowSnackbar -> {
          snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
        is UiEvent.LaunchShare -> {
          InvoiceBitmapRenderer.shareInvoiceImage(context, event.uri, event.target)
        }
        is UiEvent.LaunchSafSave -> {
          pendingSafSourceUri = event.tempFileUri
          safCreateDocumentLauncher.launch(event.suggestedFileName)
        }
      }
    }
  }

  if (uiState.currentTab != MainTab.EDITOR) {
    BackHandler {
      viewModel.selectTab(MainTab.EDITOR)
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              )
              if (uiState.storeProfile.isProActivated) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                  shape = MaterialTheme.shapes.small,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Verified,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "PRO",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                  }
                }
              }
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          FaturatiBottomBar(
            currentTab = uiState.currentTab,
            isProActivated = uiState.storeProfile.isProActivated,
            onSelectTab = viewModel::selectTab,
          )
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavigationRail) {
          FaturatiNavigationRail(
            currentTab = uiState.currentTab,
            isProActivated = uiState.storeProfile.isProActivated,
            onSelectTab = viewModel::selectTab,
          )
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          FaturatiTabContent(
            uiState = uiState,
            viewModel = viewModel,
          )
        }
      }
    }
  }
}

@Composable
private fun FaturatiTabContent(
  uiState: InvoiceUiState,
  viewModel: InvoiceViewModel,
) {
  when (uiState.currentTab) {
    MainTab.EDITOR -> EditorScreen(
      uiState = uiState,
      onStoreNameChange = viewModel::updateStoreName,
      onStorePhoneChange = viewModel::updateStorePhone,
      onCurrencySelect = viewModel::updateCurrency,
      onLogoPicked = viewModel::onLogoPicked,
      onRemoveLogo = viewModel::removeLogo,
      onCustomerNameChange = viewModel::updateCustomerName,
      onInvoiceNumberChange = viewModel::updateInvoiceNumber,
      onRegenerateInvoiceNumber = viewModel::regenerateInvoiceNumber,
      onAddProduct = viewModel::addProduct,
      onRemoveProduct = viewModel::removeProduct,
      onProductNameChange = viewModel::updateProductName,
      onProductPriceChange = viewModel::updateProductPrice,
      onIncrementQty = viewModel::incrementProductQty,
      onDecrementQty = viewModel::decrementProductQty,
      onDeliveryFeeChange = viewModel::updateDeliveryFee,
      onDiscountChange = viewModel::updateDiscount,
      onNotesChange = viewModel::updateNotes,
      onGoToPreview = { viewModel.selectTab(MainTab.PREVIEW) },
    )
    MainTab.PREVIEW -> PreviewExportScreen(
      uiState = uiState,
      onSelectTemplate = viewModel::selectTemplate,
      onGoToProActivation = viewModel::goToProActivation,
      onDismissLockedPrompt = viewModel::dismissLockedPrompt,
      onShare = viewModel::exportAndShare,
      onSaveToGallery = viewModel::saveToGallery,
    )
    MainTab.PRO -> ProActivationScreen(
      uiState = uiState,
      onCodeChange = viewModel::updateActivationCode,
      onActivateClick = viewModel::submitActivationCode,
      onDeactivateDemoClick = viewModel::deactivateProDemo,
    )
  }
}

@Composable
private fun FaturatiBottomBar(
  currentTab: MainTab,
  isProActivated: Boolean,
  onSelectTab: (MainTab) -> Unit,
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
  ) {
    NavigationBarItem(
      selected = currentTab == MainTab.EDITOR,
      onClick = { onSelectTab(MainTab.EDITOR) },
      icon = {
        Icon(
          imageVector = if (currentTab == MainTab.EDITOR) Icons.Rounded.EditNote else Icons.Outlined.EditNote,
          contentDescription = null,
        )
      },
      label = { Text(stringResource(R.string.nav_editor)) },
      modifier = Modifier.testTag("nav_tab_editor"),
    )
    NavigationBarItem(
      selected = currentTab == MainTab.PREVIEW,
      onClick = { onSelectTab(MainTab.PREVIEW) },
      icon = {
        Icon(
          imageVector = if (currentTab == MainTab.PREVIEW) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
          contentDescription = null,
        )
      },
      label = { Text(stringResource(R.string.nav_preview)) },
      modifier = Modifier.testTag("nav_tab_preview"),
    )
    NavigationBarItem(
      selected = currentTab == MainTab.PRO,
      onClick = { onSelectTab(MainTab.PRO) },
      icon = {
        BadgedBox(
          badge = {
            if (!isProActivated) {
              Badge(containerColor = MaterialTheme.colorScheme.tertiary)
            }
          },
        ) {
          Icon(
            imageVector = if (currentTab == MainTab.PRO) Icons.Rounded.WorkspacePremium else Icons.Outlined.WorkspacePremium,
            contentDescription = null,
          )
        }
      },
      label = { Text(stringResource(R.string.nav_pro)) },
      modifier = Modifier.testTag("nav_tab_pro"),
    )
  }
}

@Composable
private fun FaturatiNavigationRail(
  currentTab: MainTab,
  isProActivated: Boolean,
  onSelectTab: (MainTab) -> Unit,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxHeight(),
  ) {
    Spacer(modifier = Modifier.weight(0.2f))
    NavigationRailItem(
      selected = currentTab == MainTab.EDITOR,
      onClick = { onSelectTab(MainTab.EDITOR) },
      icon = {
        Icon(
          imageVector = if (currentTab == MainTab.EDITOR) Icons.Rounded.EditNote else Icons.Outlined.EditNote,
          contentDescription = null,
        )
      },
      label = { Text(stringResource(R.string.nav_editor)) },
      modifier = Modifier.testTag("rail_tab_editor"),
    )
    NavigationRailItem(
      selected = currentTab == MainTab.PREVIEW,
      onClick = { onSelectTab(MainTab.PREVIEW) },
      icon = {
        Icon(
          imageVector = if (currentTab == MainTab.PREVIEW) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
          contentDescription = null,
        )
      },
      label = { Text(stringResource(R.string.nav_preview)) },
      modifier = Modifier.testTag("rail_tab_preview"),
    )
    NavigationRailItem(
      selected = currentTab == MainTab.PRO,
      onClick = { onSelectTab(MainTab.PRO) },
      icon = {
        BadgedBox(
          badge = {
            if (!isProActivated) {
              Badge(containerColor = MaterialTheme.colorScheme.tertiary)
            }
          },
        ) {
          Icon(
            imageVector = if (currentTab == MainTab.PRO) Icons.Rounded.WorkspacePremium else Icons.Outlined.WorkspacePremium,
            contentDescription = null,
          )
        }
      },
      label = { Text(stringResource(R.string.nav_pro)) },
      modifier = Modifier.testTag("rail_tab_pro"),
    )
    Spacer(modifier = Modifier.weight(1f))
  }
}
