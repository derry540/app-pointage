package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.EmployeePayrollSummary
import com.example.ui.components.ThermalHeroArtwork
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.AppPortalMode
import com.example.viewmodel.ThermoPointViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardTabScreen(
    viewModel: ThermoPointViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val summaries by viewModel.payrollSummaries.collectAsStateWithLifecycle()
    val stockItems by viewModel.stockItems.collectAsStateWithLifecycle()
    val procurements by viewModel.dailyProcurements.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isSeeding by viewModel.isSeeding.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.toggleNotifications(granted)
    }

    val lowStockList = stockItems.filter { it.isLowStock }
    val overdueOrPendingSuppliers = suppliers.filter { it.remainingBalance > 0 }
    val totalMonthHours = summaries.sumOf { it.totalHours }
    val totalOvertimeHours = summaries.sumOf { it.overtimeHoursTotal }
    val totalGrossMAD = summaries.sumOf { it.grossEarnings }
    val totalAdvancesMAD = summaries.sumOf { it.totalAdvances }
    val totalNetRemainingMAD = summaries.sumOf { it.remainingToPay }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ThermalHeroArtwork(
                        height = 155.dp,
                        showRegenerateButton = true
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0xB30F172A))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ÉQUIPEMENTS THERMIQUES",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pilotage Pointage (9h/j + Bonus), Avances, Stock & Fournisseurs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.setPortalMode(AppPortalMode.WORKER_PORTAL)
                                },
                                modifier = Modifier.testTag("switch_worker_portal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Vue Employé",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vue Salarié")
                            }
                        }
                    }
                }
            }
        }

        // Empty State / Demo Data Loader
        if (summaries.isEmpty() && stockItems.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Démarrage Rapide — Base de Données Équipements Thermiques",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Chargez en un clic des employés (avec taux horaires variables et bonus >9h), des sociétés clientes, des pointages multi-chantiers, des avances, du stock thermique et des échéances fournisseurs.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.seedInitialCompanyData() },
                            enabled = !isSeeding,
                            modifier = Modifier.testTag("seed_demo_data_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSeeding) "Chargement en cours..." else "Charger les Données Équipements Thermiques")
                        }
                    }
                }
            }
        }

        // Month selector & Notification Alert Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Période Active : $selectedMonth",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Norme 9h/jour • Heures >9h majorées avec bonus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Statut Alertes",
                                tint = if (notificationsEnabled) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.toggleNotifications(enabled)
                                    }
                                },
                                modifier = Modifier.testTag("notifications_toggle_switch")
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("2026-11", "2026-10", "2026-09").forEach { month ->
                            AssistChip(
                                onClick = { viewModel.setSelectedMonth(month) },
                                label = { Text("Mois $month") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedMonth == month) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        // KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Heures Pointées",
                        value = "${"%.1f".format(totalMonthHours)} h",
                        subtitle = "Dont +${"%.1f".format(totalOvertimeHours)}h sup (>9h)",
                        accentColor = CryoSkyBlue
                    )
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Brut + Bonus (%)",
                        value = "${"%.0f".format(totalGrossMAD)} DH",
                        subtitle = "${summaries.size} employés actifs",
                        accentColor = ThermalCopper
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Avances Versées",
                        value = "-${"%.0f".format(totalAdvancesMAD)} DH",
                        subtitle = "Déduites automatiquement",
                        accentColor = ThermalAmber
                    )
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Reste à Payer Net",
                        value = "${"%.0f".format(totalNetRemainingMAD)} DH",
                        subtitle = "Solde dû fin de période",
                        accentColor = EmeraldSuccess
                    )
                }
            }
        }

        // Quick Module Navigation Cards (Employees, Stock Tracking, Suppliers, Payroll)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Navigation Modules Admin — Équipements Thermiques",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModuleQuickNavCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_nav_employees"),
                            icon = Icons.Default.Groups,
                            title = "Gestion Employés & Pointage",
                            badgeText = "${summaries.size} employés",
                            accentColor = ThermalCopper,
                            onClick = { onNavigateToTab(1) }
                        )
                        ModuleQuickNavCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_nav_stock"),
                            icon = Icons.Default.Inventory2,
                            title = "Suivi Stock & Alertes",
                            badgeText = "${lowStockList.size} alerte(s)",
                            accentColor = if (lowStockList.isNotEmpty()) CrimsonAlert else CryoSkyBlue,
                            onClick = { onNavigateToTab(3) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModuleQuickNavCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_nav_payroll"),
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Paie Active & Avances",
                            badgeText = "${"%.0f".format(totalNetRemainingMAD)} DH net",
                            accentColor = EmeraldSuccess,
                            onClick = { onNavigateToTab(2) }
                        )
                        ModuleQuickNavCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_nav_suppliers"),
                            icon = Icons.Default.LocalShipping,
                            title = "Fournisseurs & Échéances",
                            badgeText = "${overdueOrPendingSuppliers.size} échéance(s)",
                            accentColor = ThermalAmber,
                            onClick = { onNavigateToTab(4) }
                        )
                    }
                }
            }
        }

        // Active Payrolls Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_payrolls_summary_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Synthèse de la Paie Active ($selectedMonth)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Détails Paie →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTab(2) }
                        )
                    }
                    if (summaries.isEmpty()) {
                        Text(
                            text = "Aucun employé actif enregistré pour cette période.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        summaries.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { onNavigateToTab(2) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${s.employee.fullName} (${"%.0f".format(s.employee.hourlyRate)} DH/h)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Heures: ${"%.1f".format(s.totalHours)}h (Sup >9h: +${"%.1f".format(s.overtimeHoursTotal)}h) • Avances: -${"%.0f".format(s.totalAdvances)} DH",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${"%.0f".format(s.remainingToPay)} DH",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Reste à payer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Export, Email Automation & Oxygène ERP Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Exports Facturation (PDF / Excel) & Rapport Mensuel Email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportFullPdfReport(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ThermalCopper)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF", maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.exportExcelCsv(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_export_excel_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CryoSkyBlue)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel/CSV", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.sendAutomatedMonthlyEmailReport(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_email_report_button")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Envoyer le Rapport Mensuel par Email", maxLines = 1)
                    }
                }
            }
        }

        // Graphical Performance & Payroll Recap Chart
        if (summaries.isNotEmpty()) {
            item {
                EmployeePerformanceChartCard(summaries = summaries)
            }
        }

        // Real-Time Automatic Stock Restock Alerts Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lowStockList.isNotEmpty()) {
                        CrimsonAlert.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (lowStockList.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Alerte Stock",
                                tint = if (lowStockList.isNotEmpty()) CrimsonAlert else EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alertes Réapprovisionnement Stock (${lowStockList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Gérer Stock →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTab(3) }
                        )
                    }

                    if (lowStockList.isEmpty()) {
                        Text(
                            text = "Tous les équipements thermiques sont au-dessus du seuil critique de réapprovisionnement.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        lowStockList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.name} (${item.category})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "En stock: ${item.currentQuantity} ${item.unit} • Seuil min: ${item.reorderPoint} • Suggéré: +${item.suggestedOrderQuantity} (${"%.0f".format(item.unitPrice)} DH/u)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CrimsonAlert
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.addDailyProcurement(
                                            date = ThermoPointViewModel.todayDateString(),
                                            companyId = "stock_reappro",
                                            companyName = "Réapprovisionnement Stock Central",
                                            itemName = item.name,
                                            quantity = item.suggestedOrderQuantity,
                                            unitCost = item.unitPrice
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_procure_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = "Ajouter aux achats du jour",
                                        tint = ThermalCopper
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Procurement List (Sl3a li khasaha njibha pour cette journée)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marchandise à Apporter Aujourd'hui (${procurements.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ajouter +",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTab(3) }
                        )
                    }
                    if (procurements.isEmpty()) {
                        Text(
                            text = "Aucun achat journalier enregistré pour les chantiers aujourd'hui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        procurements.take(5).forEach { proc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { viewModel.cycleProcurementStatus(proc) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${proc.quantity}x ${proc.itemName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Chantier: ${proc.companyName} • ${proc.date} • ${"%.0f".format(proc.totalCost)} DH",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(text = proc.status)
                            }
                        }
                    }
                }
            }
        }

        // Supplier Payment Reminders
        if (overdueOrPendingSuppliers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rappels Échéanciers Fournisseurs (${overdueOrPendingSuppliers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Échéancier →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToTab(4) }
                            )
                        }
                        overdueOrPendingSuppliers.take(4).forEach { sup ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${sup.name} (${sup.invoiceReference})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Échéance: ${sup.dueDate} (${sup.paymentTerms}) • Reste: ${"%.0f".format(sup.remainingBalance)} DH",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (sup.status == "OVERDUE") CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(text = sup.status)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun KpiStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModuleQuickNavCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                StatusBadge(text = badgeText)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmployeePerformanceChartCard(
    summaries: List<EmployeePayrollSummary>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Graphique Récapitulatif : Performances, Brut, Avances & Mois Précédent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = ThermalCopper, label = "Brut Actuel")
                LegendDot(color = EmeraldSuccess, label = "Reste Net")
                LegendDot(color = CryoSkyBlue, label = "Mois Précédent")
            }

            val maxAmount = summaries.maxOfOrNull {
                maxOf(it.grossEarnings, it.employee.lastMonthPaidTotal, 1000.0)
            } ?: 1000.0

            summaries.forEach { s ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${s.employee.fullName} (★${"%.1f".format(s.averageRating)}/5 • ${"%.1f".format(s.totalHours)}h)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Net: ${"%.0f".format(s.remainingToPay)} DH / Préc: ${"%.0f".format(s.employee.lastMonthPaidTotal)} DH",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        val barHeight = size.height / 3f - 2f
                        val grossRatio = (s.grossEarnings / maxAmount).toFloat().coerceIn(0.02f, 1f)
                        val netRatio = (s.remainingToPay.coerceAtLeast(0.0) / maxAmount).toFloat().coerceIn(0.02f, 1f)
                        val prevRatio = (s.employee.lastMonthPaidTotal / maxAmount).toFloat().coerceIn(0.02f, 1f)

                        drawRoundRect(
                            color = ThermalCopper,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width * grossRatio, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = EmeraldSuccess,
                            topLeft = Offset(0f, barHeight + 3f),
                            size = Size(size.width * netRatio, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = CryoSkyBlue,
                            topLeft = Offset(0f, (barHeight + 3f) * 2f),
                            size = Size(size.width * prevRatio, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StatusBadge(text: String) {
    val color = when (text) {
        "PAID", "RECEIVED", "OK" -> EmeraldSuccess
        "PARTIAL", "ORDERED", "NEEDED", "PENDING" -> ThermalAmber
        "OVERDUE" -> CrimsonAlert
        else -> CryoSkyBlue
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
