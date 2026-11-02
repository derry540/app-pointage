package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.EmployeeProfile
import com.example.data.model.PayrollCalculator
import com.example.data.model.ShiftIntervalInput
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.ThermoPointViewModel

@Composable
fun PointageAndEmployeesTabScreen(
    viewModel: ThermoPointViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Pointage Journalier (9h+)") },
                modifier = Modifier.testTag("subtab_pointage_daily")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Catégories & Tarifs (Admin)") },
                modifier = Modifier.testTag("subtab_employee_categories")
            )
        }

        if (selectedSubTab == 0) {
            DailyPointageSubScreen(viewModel = viewModel)
        } else {
            EmployeeCategoriesAdminSubScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyPointageSubScreen(
    viewModel: ThermoPointViewModel
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val companies by viewModel.companies.collectAsStateWithLifecycle()
    val attendanceLogs by viewModel.attendanceLogs.collectAsStateWithLifecycle()

    var selectedEmployeeId by remember { mutableStateOf("") }
    val selectedEmployee = employees.find { it.id == selectedEmployeeId } ?: employees.firstOrNull()

    var workDate by remember { mutableStateOf(ThermoPointViewModel.todayDateString()) }
    var dailyNote by remember { mutableStateOf("") }
    var performanceRating by remember { mutableIntStateOf(5) }

    // Support multiple company shifts in the same day
    val shiftIntervals = remember {
        mutableStateListOf(
            ShiftIntervalInput(
                startTime = "08:00",
                endTime = "17:00",
                hours = 9.0,
                companyName = "",
                taskNote = ""
            )
        )
    }

    val totalShiftHours = shiftIntervals.sumOf { it.hours }
    val (regularHours, overtimeHours, estimatedPay) = if (selectedEmployee != null) {
        PayrollCalculator.computeDailyBreakdown(
            totalHours = totalShiftHours,
            hourlyRate = selectedEmployee.hourlyRate,
            overtimeBonusRate = selectedEmployee.overtimeBonusRate
        )
    } else {
        Triple(0.0, 0.0, 0.0)
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
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enregistrement Pointage Journalier & Multi-Sociétés",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sélectionnez l'employé, ajoutez 1 ou plusieurs créneaux horaires si l'employé a travaillé dans 2 sociétés le même jour. La tâche est facultative.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (employees.isEmpty()) {
                        Text(
                            text = "Aucun employé enregistré. Allez dans l'onglet 'Catégories & Tarifs' ou chargez les données démo depuis le Dashboard.",
                            color = ThermalAmber,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "1. Choisir l'employé :",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            employees.forEach { emp ->
                                val isSelected = selectedEmployee?.id == emp.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedEmployeeId = emp.id },
                                    label = {
                                        Text("${emp.fullName} (${"%.0f".format(emp.hourlyRate)} DH/h)")
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = workDate,
                            onValueChange = { workDate = it },
                            label = { Text("Date du pointage (YYYY-MM-DD)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pointage_date_input"),
                            singleLine = true
                        )

                        Text(
                            text = "2. Intervalles d'heures & Sociétés clientes (1 ou 2+ sociétés/jour) :",
                            style = MaterialTheme.typography.labelLarge
                        )

                        shiftIntervals.forEachIndexed { index, shift ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Créneau #${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ThermalCopper
                                        )
                                        if (shiftIntervals.size > 1) {
                                            IconButton(
                                                onClick = { shiftIntervals.removeAt(index) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Supprimer créneau"
                                                )
                                            }
                                        }
                                    }

                                    // Select company from pre-registered database list
                                    if (companies.isNotEmpty()) {
                                        Text(
                                            text = "Sélection rapide société (Base de données) :",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            companies.forEach { comp ->
                                                AssistChip(
                                                    onClick = {
                                                        shiftIntervals[index] = shift.copy(companyName = comp.name)
                                                    },
                                                    label = { Text(comp.name) },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = if (shift.companyName == comp.name) {
                                                            MaterialTheme.colorScheme.primaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.surface
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = shift.companyName,
                                        onValueChange = {
                                            shiftIntervals[index] = shift.copy(companyName = it)
                                        },
                                        label = { Text("Société / Chantier") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = shift.startTime,
                                            onValueChange = {
                                                shiftIntervals[index] = shift.copy(startTime = it)
                                            },
                                            label = { Text("Début") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = shift.endTime,
                                            onValueChange = {
                                                shiftIntervals[index] = shift.copy(endTime = it)
                                            },
                                            label = { Text("Fin") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = shift.hours.toString(),
                                            onValueChange = { v ->
                                                val parsed = v.toDoubleOrNull() ?: 0.0
                                                shiftIntervals[index] = shift.copy(hours = parsed)
                                            },
                                            label = { Text("Heures") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = shift.taskNote,
                                        onValueChange = {
                                            shiftIntervals[index] = shift.copy(taskNote = it)
                                        },
                                        label = { Text("Tâche effectuée (Optionnel - pas obligatoire)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                shiftIntervals.add(
                                    ShiftIntervalInput(
                                        startTime = "14:00",
                                        endTime = "19:30",
                                        hours = 5.5,
                                        companyName = companies.lastOrNull()?.name ?: "2ème Société",
                                        taskNote = ""
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_second_company_shift_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajouter un 2ème créneau / 2ème Société le même jour")
                        }

                        // Live Automatic Calculation Box (9h Regular Cap + >9h Overtime Bonus)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Calcul Automatique (Norme 9h/jour + Heures Sup Bonus) :",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ThermalCopper
                                )
                                Text(
                                    text = "Total Journée: ${"%.1f".format(totalShiftHours)}h = ${"%.1f".format(regularHours)}h Normales (à ${"%.0f".format(selectedEmployee?.hourlyRate ?: 0.0)} DH/h) + ${"%.1f".format(overtimeHours)}h Sup >9h (à ${"%.0f".format(selectedEmployee?.overtimeBonusRate ?: 0.0)} DH/h)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Total à payer pour cette journée : ${"%.2f".format(estimatedPay)} DH",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = dailyNote,
                            onValueChange = { dailyNote = it },
                            label = { Text("Note quotidienne sur l'employé (discipline, matériel, etc.)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pointage_daily_note_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Évaluation du jour :",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { performanceRating = star },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (star <= performanceRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$star étoiles",
                                            tint = ThermalAmber
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedEmployee != null) {
                                    viewModel.logMultiShiftAttendance(
                                        context = context,
                                        employee = selectedEmployee,
                                        date = workDate,
                                        shifts = shiftIntervals.toList(),
                                        dailyNote = dailyNote,
                                        performanceRating = performanceRating
                                    )
                                    dailyNote = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_pointage_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Valider & Enregistrer le Pointage Journalier")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Historique des Pointages Journaliers (${attendanceLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(attendanceLogs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${log.employeeName} • ${log.date}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Société(s) : ${log.companyName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CryoSkyBlue
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
                        text = "Heures : ${"%.1f".format(log.totalHours)}h (Normales <=9h: ${"%.1f".format(log.regularHours)}h | Heures Sup >9h: +${"%.1f".format(log.overtimeHours)}h) • Note: ★${log.performanceRating}/5",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Créneaux : ${log.shiftsSummary}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (log.taskDescription.isNotBlank()) {
                        Text(
                            text = "Tâche : ${log.taskDescription}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (log.dailyNote.isNotBlank()) {
                        Text(
                            text = "Observation : ${log.dailyNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThermalAmber
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saisi par : ${log.recordedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.deleteAttendance(log) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer pointage",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeCategoriesAdminSubScreen(
    viewModel: ThermoPointViewModel
) {
    val employees by viewModel.employees.collectAsStateWithLifecycle()

    var editingEmployee by remember { mutableStateOf<EmployeeProfile?>(null) }
    var fullName by remember { mutableStateOf("") }
    var workerEmail by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("Chauffagiste & Brûleurs") }
    var selectedRole by remember { mutableStateOf("WORKER") }
    var hourlyRateText by remember { mutableStateOf("50") }
    var overtimeRateText by remember { mutableStateOf("75") }
    var commissionText by remember { mutableStateOf("5") }
    var lastMonthPaidText by remember { mutableStateOf("9500") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        text = if (editingEmployee == null) {
                            "Ajouter un Employé & Définir son Tarif Horaire Variable (Admin)"
                        } else {
                            "Modifier le Profil de ${editingEmployee!!.fullName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nom complet de l'employé") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("employee_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = workerEmail,
                        onValueChange = { workerEmail = it },
                        label = { Text("Email (pour Portail Salarié & Rapport Mensuel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Catégorie / Spécialité Thermique") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Rôle & Permissions d'accès :",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "WORKER" to "Employé (Lecture Stats Seule)",
                            "MANAGER" to "Chef d'Équipe",
                            "ADMIN" to "Admin (Accès Total)"
                        ).forEach { (roleKey, roleLabel) ->
                            FilterChip(
                                selected = selectedRole == roleKey,
                                onClick = { selectedRole = roleKey },
                                label = { Text(roleLabel) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hourlyRateText,
                            onValueChange = { hourlyRateText = it },
                            label = { Text("Tarif 1h (DH/h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("employee_hourly_rate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = overtimeRateText,
                            onValueChange = { overtimeRateText = it },
                            label = { Text("Tarif >9h Bonus (DH/h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commissionText,
                            onValueChange = { commissionText = it },
                            label = { Text("Pourcentage (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lastMonthPaidText,
                            onValueChange = { lastMonthPaidText = it },
                            label = { Text("Payé Mois Dernier (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val rate = hourlyRateText.toDoubleOrNull() ?: 45.0
                                val overRate = overtimeRateText.toDoubleOrNull() ?: (rate * 1.5)
                                val comm = commissionText.toDoubleOrNull() ?: 0.0
                                val lastPaid = lastMonthPaidText.toDoubleOrNull() ?: 0.0

                                if (editingEmployee == null) {
                                    viewModel.addEmployee(
                                        fullName = fullName,
                                        workerEmail = workerEmail,
                                        role = selectedRole,
                                        specialty = specialty,
                                        hourlyRate = rate,
                                        overtimeBonusRate = overRate,
                                        commissionPercent = comm,
                                        lastMonthPaidTotal = lastPaid
                                    )
                                } else {
                                    viewModel.updateEmployeeProfile(
                                        editingEmployee!!.copy(
                                            fullName = fullName,
                                            workerEmail = workerEmail,
                                            role = selectedRole,
                                            specialty = specialty,
                                            hourlyRate = rate,
                                            overtimeBonusRate = overRate,
                                            commissionPercent = comm,
                                            lastMonthPaidTotal = lastPaid
                                        )
                                    )
                                    editingEmployee = null
                                }
                                fullName = ""
                                workerEmail = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_employee_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (editingEmployee == null) "Ajouter à la Base" else "Enregistrer Modifications")
                        }

                        if (editingEmployee != null) {
                            OutlinedButton(
                                onClick = {
                                    editingEmployee = null
                                    fullName = ""
                                    workerEmail = ""
                                }
                            ) {
                                Text("Annuler")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Effectif Équipements Thermiques (${employees.size} collaborateurs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(employees, key = { it.id }) { emp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${emp.fullName} [${emp.role}]",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${emp.specialty} • ${emp.workerEmail.ifBlank { "Sans email" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "1h Normale: ${"%.0f".format(emp.hourlyRate)} DH | >9h Bonus: ${"%.0f".format(emp.overtimeBonusRate)} DH | Commission: ${"%.1f".format(emp.commissionPercent)}% | Mois Préc: ${"%.0f".format(emp.lastMonthPaidTotal)} DH",
                            style = MaterialTheme.typography.labelLarge,
                            color = ThermalCopper
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                editingEmployee = emp
                                fullName = emp.fullName
                                workerEmail = emp.workerEmail
                                specialty = emp.specialty
                                selectedRole = emp.role
                                hourlyRateText = emp.hourlyRate.toString()
                                overtimeRateText = emp.overtimeBonusRate.toString()
                                commissionText = emp.commissionPercent.toString()
                                lastMonthPaidText = emp.lastMonthPaidTotal.toString()
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier employé")
                        }
                        IconButton(
                            onClick = { viewModel.deleteEmployeeProfile(emp) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer employé",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
