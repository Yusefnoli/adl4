package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.export.ShareTarget
import com.example.model.InvoiceTemplate
import com.example.ui.InvoiceUiState
import com.example.ui.components.InvoicePreviewCard
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.WhatsAppGreen

@Composable
fun PreviewExportScreen(
  uiState: InvoiceUiState,
  onSelectTemplate: (InvoiceTemplate) -> Unit,
  onGoToProActivation: () -> Unit,
  onDismissLockedPrompt: () -> Unit,
  onShare: (ShareTarget) -> Unit,
  onSaveToGallery: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Template Selector Section
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.choose_template_title),
          style = MaterialTheme.typography.titleMedium,
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          InvoiceTemplate.entries.forEach { template ->
            val isSelected = uiState.storeProfile.selectedTemplate == template
            val isLocked = template.isPro && !uiState.storeProfile.isProActivated

            TemplateSelectorCard(
              template = template,
              isSelected = isSelected,
              isLocked = isLocked,
              onClick = { onSelectTemplate(template) },
            )
          }
        }
      }

      // Locked Pro Template Callout
      AnimatedVisibility(visible = uiState.lockedTemplatePrompt != null) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WorkspacePremium,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.locked_template_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = onGoToProActivation,
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                  ),
                  modifier = Modifier.testTag("unlock_templates_cta"),
                ) {
                  Text(
                    text = stringResource(R.string.unlock_now_button),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
                TextButton(onClick = onDismissLockedPrompt) {
                  Text(text = "إغلاق", style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }

      // 2. Live Invoice Preview Card
      InvoicePreviewCard(
        storeProfile = uiState.storeProfile,
        draft = uiState.draft,
      )

      // 3. Export & Social Share Actions Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.export_share_title),
            style = MaterialTheme.typography.titleMedium,
          )

          Button(
            onClick = { onShare(ShareTarget.SYSTEM) },
            enabled = !uiState.isExporting,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("export_share_primary_button"),
          ) {
            if (uiState.isExporting) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.export_share_primary),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onShare(ShareTarget.WHATSAPP) },
              enabled = !uiState.isExporting,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("share_whatsapp_button"),
            ) {
              Icon(imageVector = Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = stringResource(R.string.share_whatsapp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
              )
            }

            Button(
              onClick = { onShare(ShareTarget.FACEBOOK) },
              enabled = !uiState.isExporting,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = FacebookBlue,
                contentColor = Color.White,
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("share_facebook_button"),
            ) {
              Icon(imageVector = Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = stringResource(R.string.share_facebook),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
              )
            }

            OutlinedButton(
              onClick = onSaveToGallery,
              enabled = !uiState.isExporting,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("save_gallery_button"),
            ) {
              Icon(imageVector = Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.save_to_gallery),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun TemplateSelectorCard(
  template: InvoiceTemplate,
  isSelected: Boolean,
  isLocked: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .width(142.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("template_card_${template.id}"),
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
      MaterialTheme.colorScheme.surface
    },
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Two-dot color swatch
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(template.headerBgColor),
          )
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(template.accentColor),
          )
        }

        if (isLocked) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = stringResource(R.string.badge_pro),
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
          )
        } else if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Text(
        text = stringResource(template.titleRes),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
      )

      Text(
        text = stringResource(if (template.isPro) R.string.badge_pro else R.string.badge_free),
        style = MaterialTheme.typography.labelSmall,
        color = if (template.isPro) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
      )
    }
  }
}
