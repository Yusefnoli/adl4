package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.ActivationFeedback
import com.example.ui.InvoiceUiState

@Composable
fun ProActivationScreen(
  uiState: InvoiceUiState,
  onCodeChange: (String) -> Unit,
  onActivateClick: () -> Unit,
  onDeactivateDemoClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isPro = uiState.storeProfile.isProActivated

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 620.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Hero Status Card
      Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = if (isPro) {
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                  )
                } else {
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                  )
                }
              )
            )
            .padding(22.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (isPro) Icons.Rounded.Verified else Icons.Rounded.WorkspacePremium,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(28.dp),
                )
              }

              Column {
                Text(
                  text = stringResource(R.string.pro_title),
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                  text = stringResource(
                    if (isPro) R.string.pro_status_active else R.string.pro_status_free
                  ),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                  modifier = Modifier.testTag("pro_status_text"),
                )
              }
            }

            Text(
              text = stringResource(R.string.pro_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
            )
          }
        }
      }

      // 2. Pro Benefits Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          ProBenefitRow(
            icon = Icons.Outlined.AutoAwesome,
            text = stringResource(R.string.pro_benefit_templates),
            unlocked = isPro,
          )
          ProBenefitRow(
            icon = Icons.Outlined.HideImage,
            text = stringResource(R.string.pro_benefit_watermark),
            unlocked = isPro,
          )
          ProBenefitRow(
            icon = Icons.Outlined.HighQuality,
            text = stringResource(R.string.pro_benefit_quality),
            unlocked = isPro,
          )
        }
      }

      // 3. Activation Code Input Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (!isPro) {
            OutlinedTextField(
              value = uiState.activationCodeInput,
              onValueChange = onCodeChange,
              label = { Text(stringResource(R.string.activation_code_label)) },
              placeholder = { Text(stringResource(R.string.activation_code_placeholder)) },
              leadingIcon = {
                Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
              },
              singleLine = true,
              isError = uiState.activationFeedback is ActivationFeedback.Error,
              shape = RoundedCornerShape(14.dp),
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
              ),
              keyboardActions = KeyboardActions(onDone = { onActivateClick() }),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("activation_code_input"),
            )

            AnimatedVisibility(visible = uiState.activationFeedback is ActivationFeedback.Error) {
              val errorMsg = (uiState.activationFeedback as? ActivationFeedback.Error)?.messageRes
              if (errorMsg != null) {
                Text(
                  text = stringResource(errorMsg),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.testTag("activation_error_text"),
                )
              }
            }

            Button(
              onClick = onActivateClick,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("activate_pro_button"),
            ) {
              Icon(imageVector = Icons.Rounded.LockOpen, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.activate_button),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          } else {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
              )
              Text(
                text = stringResource(R.string.activation_success_message),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("activation_success_banner"),
              )
            }

            TextButton(
              onClick = onDeactivateDemoClick,
              modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("deactivate_demo_button"),
            ) {
              Text(
                text = stringResource(R.string.deactivate_demo_button),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProBenefitRow(
  icon: ImageVector,
  text: String,
  unlocked: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(
          if (unlocked) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
          }
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (unlocked) Icons.Rounded.CheckCircle else icon,
        contentDescription = null,
        tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(22.dp),
      )
    }

    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
  }
}
