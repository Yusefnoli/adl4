package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CurrencyOption
import com.example.model.InvoiceProductItem
import com.example.model.formatAmount
import com.example.ui.InvoiceUiState
import java.io.File

@Composable
fun EditorScreen(
  uiState: InvoiceUiState,
  onStoreNameChange: (String) -> Unit,
  onStorePhoneChange: (String) -> Unit,
  onCurrencySelect: (String) -> Unit,
  onLogoPicked: (android.net.Uri) -> Unit,
  onRemoveLogo: () -> Unit,
  onCustomerNameChange: (String) -> Unit,
  onInvoiceNumberChange: (String) -> Unit,
  onRegenerateInvoiceNumber: () -> Unit,
  onAddProduct: () -> Unit,
  onRemoveProduct: (Long) -> Unit,
  onProductNameChange: (Long, String) -> Unit,
  onProductPriceChange: (Long, String) -> Unit,
  onIncrementQty: (Long) -> Unit,
  onDecrementQty: (Long) -> Unit,
  onDeliveryFeeChange: (String) -> Unit,
  onDiscountChange: (String) -> Unit,
  onNotesChange: (String) -> Unit,
  onGoToPreview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onLogoPicked(uri)
    }
  }

  var showExtraStoreDetails by remember { mutableStateOf(false) }
  var showExtraTotals by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .widthIn(max = 680.dp)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Store Identity Card (Name + Logo + Phone/Currency)
      item(key = "store_card") {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.section_store_info),
                  style = MaterialTheme.typography.titleMedium,
                )
              }

              TextButton(
                onClick = { showExtraStoreDetails = !showExtraStoreDetails },
                modifier = Modifier.testTag("toggle_store_details_button"),
              ) {
                Text(text = stringResource(R.string.store_more_details))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = if (showExtraStoreDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              // Logo Picker Box
              val logoPath = uiState.storeProfile.logoFilePath
              val hasLogo = !logoPath.isNullOrBlank() && File(logoPath).exists()

              Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                  modifier = Modifier
                    .size(74.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    }
                    .testTag("pick_logo_button"),
                  shape = RoundedCornerShape(18.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                ) {
                  if (hasLogo) {
                    AsyncImage(
                      model = File(logoPath!!),
                      contentDescription = stringResource(R.string.store_logo_cd),
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    Column(
                      modifier = Modifier.fillMaxSize(),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.store_logo_add),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = stringResource(R.string.store_logo_add),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                      )
                    }
                  }
                }

                if (hasLogo) {
                  Surface(
                    onClick = onRemoveLogo,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                      .size(24.dp)
                      .testTag("remove_logo_button"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.store_logo_remove),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(14.dp),
                      )
                    }
                  }
                }
              }

              OutlinedTextField(
                value = uiState.storeProfile.storeName,
                onValueChange = onStoreNameChange,
                label = { Text(stringResource(R.string.store_name_label)) },
                placeholder = { Text(stringResource(R.string.store_name_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                  .weight(1f)
                  .testTag("store_name_input"),
              )
            }

            AnimatedVisibility(visible = showExtraStoreDetails) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                  value = uiState.storeProfile.storePhone,
                  onValueChange = onStorePhoneChange,
                  label = { Text(stringResource(R.string.store_phone_label)) },
                  placeholder = { Text(stringResource(R.string.store_phone_placeholder)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Phone, contentDescription = null)
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_phone_input"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = stringResource(R.string.currency_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    CurrencyOption.entries.forEach { option ->
                      val selected = uiState.storeProfile.currencySymbol == option.symbol
                      FilterChip(
                        selected = selected,
                        onClick = { onCurrencySelect(option.symbol) },
                        label = { Text("${option.symbol} (${option.label})") },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.testTag("currency_chip_${option.name.lowercase()}"),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 2. Customer & Invoice Number Card
      item(key = "meta_card") {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = uiState.draft.customerName,
                onValueChange = onCustomerNameChange,
                label = { Text(stringResource(R.string.customer_name_label)) },
                placeholder = { Text(stringResource(R.string.customer_name_placeholder)) },
                leadingIcon = {
                  Icon(imageVector = Icons.Outlined.PersonOutline, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1.3f)
                  .testTag("customer_name_input"),
              )

              OutlinedTextField(
                value = uiState.draft.invoiceNumber,
                onValueChange = onInvoiceNumberChange,
                label = { Text(stringResource(R.string.invoice_number_label)) },
                trailingIcon = {
                  IconButton(
                    onClick = onRegenerateInvoiceNumber,
                    modifier = Modifier.testTag("regenerate_invoice_number_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Refresh,
                      contentDescription = stringResource(R.string.regenerate_invoice_number_cd),
                    )
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("invoice_number_input"),
              )
            }
          }
        }
      }

      // 3. Products Section Header
      item(key = "products_header") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.ReceiptLong,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.section_products),
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Text(
            text = stringResource(R.string.items_count_format, uiState.draft.items.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 4. Empty state or Product Cards
      if (uiState.draft.items.isEmpty()) {
        item(key = "products_empty") {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp),
              )
              Text(
                text = stringResource(R.string.products_empty_title),
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = stringResource(R.string.products_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        itemsIndexed(
          items = uiState.draft.items,
          key = { _, item -> item.id },
        ) { index, item ->
          ProductItemEditorCard(
            index = index,
            item = item,
            currencySymbol = uiState.storeProfile.currencySymbol,
            onNameChange = { onProductNameChange(item.id, it) },
            onPriceChange = { onProductPriceChange(item.id, it) },
            onIncrementQty = { onIncrementQty(item.id) },
            onDecrementQty = { onDecrementQty(item.id) },
            onDelete = { onRemoveProduct(item.id) },
          )
        }
      }

      // 5. Add Product CTA
      item(key = "add_product_cta") {
        FilledTonalButton(
          onClick = onAddProduct,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("add_product_button"),
        ) {
          Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.add_product_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      // 6. Optional Delivery / Discount / Notes Card
      item(key = "extras_card") {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showExtraTotals = !showExtraTotals }
                .padding(vertical = 6.dp)
                .testTag("toggle_extras_button"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.section_summary_extras),
                style = MaterialTheme.typography.titleSmall,
              )
              Icon(
                imageVector = if (showExtraTotals) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            AnimatedVisibility(visible = showExtraTotals) {
              Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OutlinedTextField(
                    value = uiState.draft.deliveryFeeText,
                    onValueChange = onDeliveryFeeChange,
                    label = { Text(stringResource(R.string.delivery_fee_label)) },
                    leadingIcon = {
                      Icon(imageVector = Icons.Outlined.LocalShipping, contentDescription = null)
                    },
                    suffix = { Text(uiState.storeProfile.currencySymbol) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("delivery_fee_input"),
                  )

                  OutlinedTextField(
                    value = uiState.draft.discountText,
                    onValueChange = onDiscountChange,
                    label = { Text(stringResource(R.string.discount_label)) },
                    leadingIcon = {
                      Icon(imageVector = Icons.Outlined.Sell, contentDescription = null)
                    },
                    suffix = { Text(uiState.storeProfile.currencySymbol) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("discount_input"),
                  )
                }

                OutlinedTextField(
                  value = uiState.draft.notes,
                  onValueChange = onNotesChange,
                  label = { Text(stringResource(R.string.invoice_notes_label)) },
                  placeholder = { Text(stringResource(R.string.invoice_notes_placeholder)) },
                  maxLines = 3,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_notes_input"),
                )
              }
            }
          }
        }
      }

      item(key = "bottom_spacer") {
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    // Sticky Bottom Grand Total & Preview CTA Bar
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      shadowElevation = 8.dp,
    ) {
      Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
          modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = stringResource(R.string.grand_total_title),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = formatAmount(uiState.draft.grandTotal, uiState.storeProfile.currencySymbol),
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("editor_grand_total_text"),
            )
          }

          Button(
            onClick = onGoToPreview,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("go_to_preview_button"),
          ) {
            Icon(imageVector = Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.preview_and_export_cta),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProductItemEditorCard(
  index: Int,
  item: InvoiceProductItem,
  currencySymbol: String,
  onNameChange: (String) -> Unit,
  onPriceChange: (String) -> Unit,
  onIncrementQty: () -> Unit,
  onDecrementQty: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.testTag("product_card_$index"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = item.name,
          onValueChange = onNameChange,
          label = { Text("${stringResource(R.string.product_name_label)} #${index + 1}") },
          placeholder = { Text(stringResource(R.string.product_name_placeholder)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          modifier = Modifier
            .weight(1f)
            .testTag("product_name_input_$index"),
        )

        IconButton(
          onClick = onDelete,
          colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("delete_product_button_$index"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.delete_product_cd),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Price input
        OutlinedTextField(
          value = item.unitPriceText,
          onValueChange = onPriceChange,
          label = { Text(stringResource(R.string.product_price_label)) },
          suffix = { Text(currencySymbol) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("product_price_input_$index"),
        )

        // Quantity Stepper
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = stringResource(R.string.product_qty_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          ) {
            IconButton(
              onClick = onDecrementQty,
              modifier = Modifier
                .size(48.dp)
                .testTag("decrement_qty_button_$index"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.decrease_qty_cd),
                modifier = Modifier.size(18.dp),
              )
            }
            Text(
              text = item.quantity.toString(),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .testTag("product_qty_text_$index"),
            )
            IconButton(
              onClick = onIncrementQty,
              modifier = Modifier
                .size(48.dp)
                .testTag("increment_qty_button_$index"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.increase_qty_cd),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // Live item subtotal pill
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
          .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.product_subtotal_label),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text = formatAmount(item.lineTotal, currencySymbol),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("product_subtotal_text_$index"),
        )
      }
    }
  }
}
