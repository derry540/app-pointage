package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.AppPortalMode
import com.example.viewmodel.ThermoPointViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerSelfPortalScreen(
    viewModel: ThermoPointViewModel
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val summaries by viewModel.payrollSummaries.collectAsStateWithLifecycle()
    val attendanceLogs by viewModel.attendanceLogs.collectAsStateWithLifecycle()
    val advances by viewModel.advances.collectAsStateWithLifecycle()
    val selectedWorkerId by viewModel.selectedWorkerIdForPortal.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    if (!viewModel.isWorkerLockedSession) {
        BackHandler {
            viewModel.setPortalMode(AppPortalMode.ADMIN_PORTAL)
        }
    }

    val activeSummary = summaries.find { it.employee.id == selectedWorkerId } ?: summaries.firstOrNull()
    val workerLogs = attendanceLogs.filter {
        it.employeeId == activeSummary?.employee?.id && it.monthKey == selectedMonth
    }
    val workerAdvances = advances.filter {
        it.employeeId == activeSummary?.employee?.id && it.monthKey == selectedMonth
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CryoSkyBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Espace Salarié (Lecture Seule — Sans Permissions Admin)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!viewModel.isWorkerLockedSession) {
                            OutlinedButton(
                                onClick = { viewModel.setPortalMode(AppPortalMode.ADMIN_PORTAL) },
                                modifier = Modifier.testTag("return_admin_portal_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Admin")
                            }
                        }
                    }

                    Text(
                        text = "Ce compte affiche uniquement les statistiques d'heures travaillées, les avances reçues et les soldes du mois en cours ($selectedMonth) et du mois précédent, sans possibilité de modifier la base de données.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (employees.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            employees.forEach { emp ->
                                FilterChip(
                                    selected = activeSummary?.employee?.id == emp.id,
                                    onClick = { viewModel.selectWorkerForPortal(emp.id) },
                                    label = { Text(emp.fullName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeSummary == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aucun profil employé disponible.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${activeSummary.employee.fullName} — ${activeSummary.employee.specialty}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tarif Horaire: ${"%.0f".format(activeSummary.employee.hourlyRate)} DH/h • Bonus >9h/j: ${"%.0f".format(activeSummary.employee.overtimeBonusRate)} DH/h • Commission: ${"%.1f".format(activeSummary.employee.commissionPercent)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = ThermalCopper
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Heures Mois ($selectedMonth)",
                                value = "${"%.1f".format(activeSummary.totalHours)} h",
                                subtitle = "Norm: ${"%.1f".format(activeSummary.regularHoursTotal)}h | >9h: +${"%.1f".format(activeSummary.overtimeHoursTotal)}h",
                                accentColor = CryoSkyBlue
                            )
                            KpiStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Gagné Brut ($selectedMonth)",
                                value = "${"%.0f".format(activeSummary.grossEarnings)} DH",
                                subtitle = "${activeSummary.daysWorked} jours pointés",
                                accentColor = ThermalCopper
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Avances Reçues",
                                value = "-${"%.0f".format(activeSummary.totalAdvances)} DH",
                                subtitle = "${workerAdvances.size} avance(s) ce mois",
                                accentColor = ThermalAmber
                            )
                            KpiStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Reste à Percevoir",
                                value = "${"%.0f".format(activeSummary.remainingToPay)} DH",
                                subtitle = "Solde net disponible",
                                accentColor = EmeraldSuccess
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CryoSkyBlue.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Payé le Mois Précédent (Chhar li fat) :",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${"%.2f".format(activeSummary.employee.lastMonthPaidTotal)} DH",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CryoSkyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.shareWorkerBalanceOnWhatsApp(context, activeSummary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("worker_share_whatsapp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Envoyer / Recevoir Fiche sur WhatsApp (*6)")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Mes Pointages Journaliers Validés (${workerLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(workerLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = CryoSkyBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${log.date} • ${log.companyName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${"%.2f".format(log.dailyPay)} DH",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Total: ${"%.1f".format(log.totalHours)}h (Normales: ${"%.1f".format(log.regularHours)}h | Sup >9h: +${"%.1f".format(log.overtimeHours)}h)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = log.shiftsSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
