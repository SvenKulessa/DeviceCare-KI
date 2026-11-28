package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.security.firewall.AppSecurityPosture
import com.example.security.firewall.FirewallAction
import com.example.security.firewall.NetworkTelemetryStatus
import com.example.security.firewall.RethinkFirewallRule
import com.example.security.firewall.RethinkFirewallRuleEngine
import com.example.security.firewall.RethinkRulePriority
import com.example.security.firewall.TamperSignalSeverity
import com.example.security.malware.Risk
import com.example.ui.theme.MonoTokenTextStyle

@Composable
fun FirewallSecurityRethinkBanner(
  scanResult: InstalledAppsScanResult,
  onFilterManipulation: () -> Unit,
  onFilterFirewallBlock: () -> Unit,
  onOpenRethinkGuide: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("firewall_rethink_summary_banner"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    border =
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Filled.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Manipulationsprüfung, Firewall & Rethink-Schutz",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text =
              "${scanResult.manipulationWarningCount} Apps mit Quell-/Änderungshinweis · " +
                "${scanResult.firewallBlockRecommendedCount} mit Firewall-Spar-/Sperr-Empfehlung · " +
                "${scanResult.recentUpdatesCount} kürzlich geändert (<7 Tage)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onFilterManipulation,
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("filter_manipulation_quick_button"),
        ) {
          Text("Prüfhinweise", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
          onClick = onFilterFirewallBlock,
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("filter_firewall_quick_button"),
        ) {
          Text("Firewall-Sperren", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }

      FilledTonalButton(
        onClick = onOpenRethinkGuide,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("open_rethink_guide_from_apps"),
      ) {
        Icon(
          imageVector = Icons.Outlined.Dns,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Rethink Firewall-, DNS- & Netz-Empfehlungen öffnen",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(15.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTamperAndFirewallCardSection(
  app: InstalledAppPermissionInfo,
  posture: AppSecurityPosture,
) {
  val fwAction = posture.firewallRecommendation.action
  val (fwBg, fwFg) =
    when (fwAction) {
      FirewallAction.BLOCK_ALL ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
      FirewallAction.ALLOW_FOREGROUND_ONLY ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
      FirewallAction.ALLOW_RESTRICTED,
      FirewallAction.ALLOW_WIFI_ONLY ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
      FirewallAction.NO_NETWORK_PERMISSION ->
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

  val hasWarn = posture.hasManipulationWarnings || posture.malwareVerdict.risk >= Risk.MITTEL
  val tamperBg =
    when (posture.highestTamperSeverity) {
      TamperSignalSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
      TamperSignalSeverity.WARN -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
      TamperSignalSeverity.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
  val tamperFg =
    when (posture.highestTamperSeverity) {
      TamperSignalSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
      TamperSignalSeverity.WARN -> MaterialTheme.colorScheme.onTertiaryContainer
      TamperSignalSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // Source + Update freshness chips
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
      ) {
        Text(
          text = "Quelle: ${posture.installerSourceLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
      if (posture.recentlyUpdatedWithin7Days) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Update,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = "Kürzlich geändert (<7 Tage)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }

    // Tamper & Malware Contract Status
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = tamperBg,
      modifier = Modifier.fillMaxWidth().testTag("app_tamper_status_${app.packageName}"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector =
            if (hasWarn) Icons.Outlined.WarningAmber else Icons.Filled.VerifiedUser,
          contentDescription = null,
          tint = tamperFg,
          modifier = Modifier.padding(top = 1.dp).size(16.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          val firstWarning = posture.tamperSignals.firstOrNull { it.severity != TamperSignalSeverity.INFO }
          Text(
            text =
              if (firstWarning != null) "Manipulations-/Quellenhinweis: ${firstWarning.label}"
              else "Manipulationstest: Keine Debug-/Sideload-Auffälligkeit",
            style = MaterialTheme.typography.labelMedium,
            color = tamperFg,
          )
          Text(
            text =
              "Diagnose-Stufe: ${posture.malwareVerdict.risk.name} (rein statisch, kein Hintergrundscan)",
            style = MaterialTheme.typography.bodySmall,
            color = tamperFg.copy(alpha = 0.9f),
          )
        }
      }
    }

    // Firewall Rule Recommendation
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = fwBg,
      modifier = Modifier.fillMaxWidth().testTag("app_firewall_rec_${app.packageName}"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Firewall-Empfehlung: ${fwAction.title}",
            style = MaterialTheme.typography.labelMedium,
            color = fwFg,
            modifier = Modifier.weight(1f),
          )
          Surface(shape = MaterialTheme.shapes.extraSmall, color = fwFg.copy(alpha = 0.14f)) {
            Text(
              text = fwAction.badge,
              style = MonoTokenTextStyle,
              color = fwFg,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = posture.firewallRecommendation.rethinkRuleHint,
          style = MaterialTheme.typography.bodySmall,
          color = fwFg.copy(alpha = 0.92f),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSecurityPostureDetailSection(
  app: InstalledAppPermissionInfo,
  posture: AppSecurityPosture,
  onOpenRethinkGuide: () -> Unit,
  ruleOverrides: Map<String, Boolean> = emptyMap(),
  onToggleRethinkRule: ((String, Boolean) -> Unit)? = null,
  onResetRethinkRules: (() -> Unit)? = null,
  onCopyRethinkRules: ((String) -> Unit)? = null,
) {
  var localOverrides by remember(app.packageName) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  val activeOverrides = if (onToggleRethinkRule != null) ruleOverrides else localOverrides

  val rules =
    remember(app.packageName, posture) {
      if (posture.rethinkFirewallRules.isNotEmpty()) {
        posture.rethinkFirewallRules
      } else {
        RethinkFirewallRuleEngine.suggestRules(
          app = app,
          networkActivity = posture.networkActivityProfile,
          requestedPermissions = emptySet(),
          grantedPermissions = app.grantedDangerousPermissions.mapTo(mutableSetOf()) { it.fullName },
          isSideloaded = posture.isSideloadedOrUnknown,
          isDebuggable = posture.isDebuggable,
          declaresAccessibilityService = posture.declaresAccessibilityService,
          malwareRisk = posture.malwareVerdict.risk,
        )
      }
    }

  val effectiveStates =
    remember(rules, activeOverrides) {
      RethinkFirewallRuleEngine.resolveEffectiveStates(rules, activeOverrides)
    }
  val activeRuleCount = effectiveStates.values.count { it }

  fun toggleRule(ruleId: String, nextEnabled: Boolean) {
    if (onToggleRethinkRule != null) {
      onToggleRethinkRule(ruleId, nextEnabled)
    } else {
      localOverrides = localOverrides.toMutableMap().apply { put(ruleId, nextEnabled) }
    }
  }

  fun resetRules() {
    if (onResetRethinkRules != null) {
      onResetRethinkRules()
    } else {
      localOverrides = emptyMap()
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Card(
      shape = MaterialTheme.shapes.medium,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.BugReport,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Herkunft, Manipulation & Diagnose (statisch)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
        }
        Text(
          text =
            "Installationsquelle: ${posture.installerSourceLabel} · Signer: ${posture.signerCount} (${posture.signerSha256Prefix ?: "—"})",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = posture.malwareVerdict.userExplanation,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Fehlalarm-Einordnung: ${posture.malwareVerdict.falsePositiveRisk}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (posture.tamperSignals.isNotEmpty()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          posture.tamperSignals.forEach { signal ->
            Text(
              text = "• ${signal.label}: ${signal.detail}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Network Activity & Capability Profile Card
    val networkProfile = posture.networkActivityProfile
    Card(
      modifier = Modifier.fillMaxWidth().testTag("app_network_activity_card_${app.packageName}"),
      shape = MaterialTheme.shapes.medium,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
      border =
        BorderStroke(
          1.dp,
          if (networkProfile.usesCleartextTraffic) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.NetworkCheck,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Netzwerkaktivität & Übertragung",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.semantics { heading() },
            )
          }
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color =
              when (networkProfile.telemetryStatus) {
                NetworkTelemetryStatus.OBSERVED -> MaterialTheme.colorScheme.primaryContainer
                NetworkTelemetryStatus.OS_RESTRICTED -> MaterialTheme.colorScheme.secondaryContainer
                NetworkTelemetryStatus.NO_NETWORK_CAPABILITY ->
                  MaterialTheme.colorScheme.surfaceContainerHighest
              },
          ) {
            Text(
              text = networkProfile.telemetryStatus.label,
              style = MaterialTheme.typography.labelSmall,
              color =
                when (networkProfile.telemetryStatus) {
                  NetworkTelemetryStatus.OBSERVED -> MaterialTheme.colorScheme.onPrimaryContainer
                  NetworkTelemetryStatus.OS_RESTRICTED ->
                    MaterialTheme.colorScheme.onSecondaryContainer
                  NetworkTelemetryStatus.NO_NETWORK_CAPABILITY ->
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }

        Text(
          text = networkProfile.activityTierLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = networkProfile.formattedTransferSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          networkProfile.capabilityTags.forEach { tag ->
            val highlightWarn = tag.contains("HTTP", ignoreCase = true) || tag.contains("Autostart")
            Surface(
              shape = MaterialTheme.shapes.small,
              color =
                if (highlightWarn) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (highlightWarn) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }
    }

    // Toggleable Rethink-Compatible Firewall Rules Section
    Card(
      modifier = Modifier.fillMaxWidth().testTag("app_rethink_rules_section_${app.packageName}"),
      shape = MaterialTheme.shapes.medium,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp),
            )
            Column {
              Text(
                text = "Rethink-Firewall-Regeln",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { heading() },
              )
              Text(
                text = posture.firewallRecommendation.rethinkRuleHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
              )
            }
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
          ) {
            Text(
              text = "$activeRuleCount/${rules.size} aktiv",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onSurface,
              modifier =
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("app_rethink_active_count_${app.packageName}"),
            )
          }
        }

        Text(
          text =
            "Auf Basis der erkannten Berechtigungen und Netzwerkaktivität vorgeschlagen. Schalte Regeln passend zu deinem Bedarf ein oder aus:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          rules.forEach { rule ->
            val enabled = effectiveStates[rule.id] ?: rule.recommendedEnabled
            RethinkFirewallRuleToggleItem(
              rule = rule,
              enabled = enabled,
              onCheckedChange = { checked -> toggleRule(rule.id, checked) },
            )
          }
        }

        if (activeOverrides.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              onClick = ::resetRules,
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .testTag("app_rethink_reset_${app.packageName}"),
            ) {
              Icon(
                imageVector = Icons.Filled.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Empfehlungen zurücksetzen")
            }
          }
        }

        if (onCopyRethinkRules != null) {
          FilledTonalButton(
            onClick = {
              val formatted =
                RethinkFirewallRuleEngine.formatRethinkRuleSummary(app, rules, activeOverrides)
              onCopyRethinkRules(formatted)
            },
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("copy_rethink_rules_button_${app.packageName}"),
          ) {
            Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aktive Regeln für Rethink kopieren")
          }
        }

        FilledTonalButton(
          onClick = onOpenRethinkGuide,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("sheet_open_rethink_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Dns,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Alle Rethink DNS- & Netzregeln ansehen")
        }
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text =
          "Hinweis: ${app.appName} wurde lokal bewertet (kein Malware-Beweis; DeviceCare KI ändert keine fremden VPN-Regeln automatisch).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RethinkFirewallRuleToggleItem(
  rule: RethinkFirewallRule,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  val priorityBg =
    when (rule.priority) {
      RethinkRulePriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
      RethinkRulePriority.HIGH -> MaterialTheme.colorScheme.primaryContainer
      RethinkRulePriority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
      RethinkRulePriority.OPTIONAL -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
  val priorityFg =
    when (rule.priority) {
      RethinkRulePriority.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
      RethinkRulePriority.HIGH -> MaterialTheme.colorScheme.onPrimaryContainer
      RethinkRulePriority.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
      RethinkRulePriority.OPTIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .minimumInteractiveComponentSize()
        .clickable(role = Role.Switch) { onCheckedChange(!enabled) }
        .testTag("rethink_rule_item_${rule.id}")
        .semantics { stateDescription = if (enabled) "Aktiviert" else "Deaktiviert" },
    shape = MaterialTheme.shapes.medium,
    color =
      if (enabled) MaterialTheme.colorScheme.surface
      else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    border =
      BorderStroke(
        width = if (enabled) 1.3.dp else 1.dp,
        color =
          if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Surface(shape = MaterialTheme.shapes.extraSmall, color = priorityBg) {
              Text(
                text = rule.priority.label,
                style = MonoTokenTextStyle,
                color = priorityFg,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            if (rule.recommendedEnabled) {
              Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
              ) {
                Text(
                  text = "Empfohlen",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = rule.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Switch(
          checked = enabled,
          onCheckedChange = onCheckedChange,
          modifier = Modifier.testTag("rethink_rule_switch_${rule.id}"),
        )
      }

      Text(
        text = rule.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (rule.triggerSignals.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          rule.triggerSignals.forEach { signal ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Text(
                text = signal,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      Text(
        text = rule.rethinkSettingPath,
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}
