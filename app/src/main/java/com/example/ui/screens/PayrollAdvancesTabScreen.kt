package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.AppPortalMode
import com.example.viewmodel.ThermoPointViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayrollAdvancesTabScreen(
    viewModel: ThermoPointViewModel
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val summaries by viewModel.payrollSummaries.collectAsStateWithLifecycle()
    val advances by viewModel.advances.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    var selectedEmployeeId by remember { mutableStateOf("") }
    val selectedEmployee = employees.find { it.id == selectedEmployeeId } ?: employees.firstOrNull()
    var advanceAmountText by remember { mutableStateOf("300") }
    var advanceDate by remember { mutableStateOf(ThermoPointViewModel.todayDateString()) }
    var advanceNote by remember { mutableStateOf("Avance espèces chantier") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Record Advance Card
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
                        text = "Enregistrer une Avance sur Salaire (Système d'Avances)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chaque avance versée est automatiquement déduite du total gagné par l'employé sur la période ($selectedMonth) pour afficher le reste exact à lui payer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (employees.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            employees.forEach { emp ->
                                FilterChip(
                                    selected = selectedEmployee?.id == emp.id,
                                    onClick = { selectedEmployeeId = emp.id },
                                    label = { Text(emp.fullName) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = advanceAmountText,
                                onValueChange = { advanceAmountText = it },
                                label = { Text("Montant Avance (DH)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("advance_amount_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = advanceDate,
                                onValueChange = { advanceDate = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = advanceNote,
                            onValueChange = { advanceNote = it },
                            label = { Text("Motif / Mode de versement (Espèces, Virement...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val amt = advanceAmountText.toDoubleOrNull() ?: 0.0
                                if (selectedEmployee != null && amt > 0.0) {
                                    viewModel.giveSalaryAdvance(
                                        context = context,
                                        employee = selectedEmployee,
                                        amount = amt,
                                        date = advanceDate,
                                        note = advanceNote
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_advance_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Valider l'Avance & Mettre à Jour le Solde")
                        }
                    }
                }
            }
        }

        // Graphical Performance Recap
        if (summaries.isNotEmpty()) {
            item {
                EmployeePerformanceChartCard(summaries = summaries)
            }
        }

        item {
            Text(
                text = "Bilan Automatique par Employé — Période $selectedMonth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Detailed Employee Payroll Breakdown Cards
        items(summaries, key = { it.employee.id }) { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.employee.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.employee.specialty} • ${summary.daysWorked} jour(s) pointé(s) • ★${"%.1f".format(summary.averageRating)}/5",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.setPortalMode(
                                    mode = AppPortalMode.WORKER_PORTAL,
                                    workerId = summary.employee.id
                                )
                            }
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compte Salarié")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Heures Normales (<=9h/j à ${"%.0f".format(summary.employee.hourlyRate)} DH/h) :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${"%.1f".format(summary.regularHoursTotal)}h = ${"%.2f".format(summary.baseEarnings)} DH",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Heures Sup + Bonus (>9h/j à ${"%.0f".format(summary.employee.overtimeBonusRate)} DH/h) :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "+${"%.1f".format(summary.overtimeHoursTotal)}h = ${"%.2f".format(summary.overtimeBonusEarnings)} DH",
                            style = MaterialTheme.typography.labelLarge,
                            color = ThermalCopper
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pourcentage Commission (${"%.1f".format(summary.employee.commissionPercent)}%) :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "+${"%.2f".format(summary.commissionAmount)} DH",
                            style = MaterialTheme.typography.labelLarge,
                            color = CryoSkyBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Gagné sur la Période ($selectedMonth) :",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${"%.2f".format(summary.grossEarnings)} DH",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Avances déjà versées (Avance li 3titha) :",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThermalAmber
                        )
                        Text(
                            text = "-${"%.2f".format(summary.totalAdvances)} DH",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThermalAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (summary.remainingToPay >= 0) {
                                    EmeraldSuccess.copy(alpha = 0.16f)
                                } else {
                                    CrimsonAlert.copy(alpha = 0.16f)
                                }
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RESTE À LUI PAYER (Chno katsalli ba9i) :",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${"%.2f".format(summary.remainingToPay)} DH",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (summary.remainingToPay >= 0) EmeraldSuccess else CrimsonAlert,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Payé fin du mois précédent (Chhar li fat) :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${"%.2f".format(summary.employee.lastMonthPaidTotal)} DH",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CryoSkyBlue
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.shareWorkerBalanceOnWhatsApp(context, summary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_whatsapp_${summary.employee.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Envoyer Mise à Jour au Salarié sur WhatsApp (*6)")
                    }
                }
            }
        }

        // Advances Log History
        if (advances.isNotEmpty()) {
            item {
                Text(
                    text = "Journal des Avances Versées (${advances.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(advances, key = { it.id }) { adv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${adv.employeeName} • ${adv.date}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${adv.note} (Par ${adv.recordedBy})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "-${"%.0f".format(adv.amount)} DH",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThermalAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
