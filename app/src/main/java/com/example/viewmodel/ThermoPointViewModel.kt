package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.AttendanceLog
import com.example.data.model.AuditLogEntry
import com.example.data.model.ClientCompany
import com.example.data.model.DailyProcurement
import com.example.data.model.EmployeePayrollSummary
import com.example.data.model.EmployeeProfile
import com.example.data.model.PayrollCalculator
import com.example.data.model.SalaryAdvance
import com.example.data.model.ShiftIntervalInput
import com.example.data.model.StockItem
import com.example.data.model.SupplierRecord
import com.example.data.repository.LocalOfflineStore
import com.example.data.repository.ThermoPointRepository
import com.example.util.ExportAndErpManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppPortalMode {
    ADMIN_PORTAL,
    WORKER_PORTAL
}

class ThermoPointViewModel(
    private val repository: ThermoPointRepository,
    val currentUserId: String,
    initialPortalMode: AppPortalMode = AppPortalMode.ADMIN_PORTAL,
    initialWorkerId: String? = null,
    val isWorkerLockedSession: Boolean = false
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentMonthKey())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _portalMode = MutableStateFlow(initialPortalMode)
    val portalMode: StateFlow<AppPortalMode> = _portalMode.asStateFlow()

    private val _selectedWorkerIdForPortal = MutableStateFlow(initialWorkerId)
    val selectedWorkerIdForPortal: StateFlow<String?> = _selectedWorkerIdForPortal.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val isCloudSignedIn: Boolean
        get() = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }

    private val cloudEmployeesFlow = repository.observeEmployees()
        .onEach { LocalOfflineStore.mergeCloudEmployees(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for employees (${e.message})")
            emit(emptyList())
        }

    val employees: StateFlow<List<EmployeeProfile>> = combine(
        LocalOfflineStore.employees,
        cloudEmployeesFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedBy { it.fullName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.employees.value)

    private val cloudAttendanceFlow = repository.observeAttendanceLogs()
        .onEach { LocalOfflineStore.mergeCloudAttendance(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for attendance_logs (${e.message})")
            emit(emptyList())
        }

    val attendanceLogs: StateFlow<List<AttendanceLog>> = combine(
        LocalOfflineStore.attendanceLogs,
        cloudAttendanceFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.attendanceLogs.value)

    private val cloudAdvancesFlow = repository.observeAdvances()
        .onEach { LocalOfflineStore.mergeCloudAdvances(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for advances (${e.message})")
            emit(emptyList())
        }

    val advances: StateFlow<List<SalaryAdvance>> = combine(
        LocalOfflineStore.advances,
        cloudAdvancesFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.advances.value)

    private val cloudCompaniesFlow = repository.observeCompanies()
        .onEach { LocalOfflineStore.mergeCloudCompanies(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for companies (${e.message})")
            emit(emptyList())
        }

    val companies: StateFlow<List<ClientCompany>> = combine(
        LocalOfflineStore.companies,
        cloudCompaniesFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.companies.value)

    private val cloudStockFlow = repository.observeStockItems()
        .onEach { LocalOfflineStore.mergeCloudStock(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for stock_items (${e.message})")
            emit(emptyList())
        }

    val stockItems: StateFlow<List<StockItem>> = combine(
        LocalOfflineStore.stockItems,
        cloudStockFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.stockItems.value)

    private val cloudProcurementsFlow = repository.observeDailyProcurements()
        .onEach { LocalOfflineStore.mergeCloudProcurements(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for daily_procurements (${e.message})")
            emit(emptyList())
        }

    val dailyProcurements: StateFlow<List<DailyProcurement>> = combine(
        LocalOfflineStore.procurements,
        cloudProcurementsFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.procurements.value)

    private val cloudSuppliersFlow = repository.observeSuppliers()
        .onEach { LocalOfflineStore.mergeCloudSuppliers(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for suppliers (${e.message})")
            emit(emptyList())
        }

    val suppliers: StateFlow<List<SupplierRecord>> = combine(
        LocalOfflineStore.suppliers,
        cloudSuppliersFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.suppliers.value)

    private val cloudAuditFlow = repository.observeAuditLogs()
        .onEach { LocalOfflineStore.mergeCloudAudit(it) }
        .catch { e ->
            Log.w(TAG, "Offline mode active for audit_logs (${e.message})")
            emit(emptyList())
        }

    val auditLogs: StateFlow<List<AuditLogEntry>> = combine(
        LocalOfflineStore.auditLogs,
        cloudAuditFlow
    ) { local, cloud ->
        (local + cloud).associateBy { it.id }.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalOfflineStore.auditLogs.value)

    val payrollSummaries: StateFlow<List<EmployeePayrollSummary>> = combine(
        employees,
        attendanceLogs,
        advances,
        selectedMonth
    ) { emps, logs, advs, month ->
        emps.map { emp ->
            PayrollCalculator.buildEmployeeSummary(
                employee = emp,
                monthKey = month,
                logs = logs,
                advances = advs
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setSelectedMonth(monthKey: String) {
        _selectedMonth.value = monthKey
    }

    fun setPortalMode(mode: AppPortalMode, workerId: String? = null) {
        if (isWorkerLockedSession && mode == AppPortalMode.ADMIN_PORTAL) {
            _statusBannerMessage.value = "Accès restreint : ce compte Salarié n'a pas les permissions Administrateur."
            return
        }
        _portalMode.value = mode
        if (workerId != null) {
            _selectedWorkerIdForPortal.value = workerId
        } else if (_selectedWorkerIdForPortal.value == null) {
            _selectedWorkerIdForPortal.value = employees.value.firstOrNull()?.id
        }
    }

    fun selectWorkerForPortal(workerId: String) {
        _selectedWorkerIdForPortal.value = workerId
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        _statusBannerMessage.value = if (enabled) {
            "Système de notifications d'alertes en temps réel activé."
        } else {
            "Notifications d'alertes désactivées."
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun shareWorkerBalanceOnWhatsApp(context: Context, summary: EmployeePayrollSummary) {
        ExportAndErpManager.shareEmployeeBalanceViaWhatsApp(
            context = context,
            summary = summary,
            monthKey = _selectedMonth.value
        )
    }

    // --- ACTIONS (Offline-First + Automatic Cloud Firestore Sync) ---

    private fun recordLocalAudit(actionType: String, module: String, details: String) {
        LocalOfflineStore.appendAuditLog(
            AuditLogEntry(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                actorName = "Admin Équipements Thermiques",
                actorRole = "ADMIN",
                actionType = actionType,
                module = module,
                details = details
            )
        )
    }

    fun addEmployee(
        fullName: String,
        workerEmail: String,
        role: String,
        specialty: String,
        hourlyRate: Double,
        overtimeBonusRate: Double,
        commissionPercent: Double,
        lastMonthPaidTotal: Double
    ) {
        if (fullName.isBlank()) {
            _statusBannerMessage.value = "Veuillez saisir le nom complet de l'employé."
            return
        }
        viewModelScope.launch {
            val localId = UUID.randomUUID().toString()
            val localProfile = EmployeeProfile(
                id = localId,
                userId = currentUserId,
                fullName = fullName.trim(),
                workerEmail = workerEmail.trim(),
                role = role,
                specialty = specialty.trim(),
                hourlyRate = hourlyRate,
                overtimeBonusRate = overtimeBonusRate,
                commissionPercent = commissionPercent,
                lastMonthPaidTotal = lastMonthPaidTotal
            )
            LocalOfflineStore.upsertEmployee(localProfile)
            recordLocalAudit(
                "CREATE",
                "Employés",
                "Ajout employé $fullName (${hourlyRate} DH/h, Bonus >9h: ${overtimeBonusRate} DH/h)"
            )
            _statusBannerMessage.value = "Profil de $fullName enregistré et synchronisé (${"%.0f".format(hourlyRate)} DH/h, Bonus >9h: ${"%.0f".format(overtimeBonusRate)} DH/h)."

            if (isCloudSignedIn) {
                repository.createEmployee(
                    fullName = fullName,
                    workerEmail = workerEmail,
                    role = role,
                    specialty = specialty,
                    hourlyRate = hourlyRate,
                    overtimeBonusRate = overtimeBonusRate,
                    commissionPercent = commissionPercent,
                    lastMonthPaidTotal = lastMonthPaidTotal
                ).onSuccess { cloudId ->
                    if (cloudId != localId) {
                        LocalOfflineStore.removeEmployee(localId)
                        LocalOfflineStore.upsertEmployee(localProfile.copy(id = cloudId))
                    }
                }
            }
        }
    }

    fun updateEmployeeProfile(employee: EmployeeProfile) {
        viewModelScope.launch {
            LocalOfflineStore.upsertEmployee(employee)
            recordLocalAudit("UPDATE", "Employés", "Mise à jour profil ${employee.fullName}")
            _statusBannerMessage.value = "Profil de ${employee.fullName} mis à jour et synchronisé."
            if (isCloudSignedIn) {
                repository.updateEmployee(employee)
            }
        }
    }

    fun deleteEmployeeProfile(employee: EmployeeProfile) {
        viewModelScope.launch {
            LocalOfflineStore.removeEmployee(employee.id)
            recordLocalAudit("DELETE", "Employés", "Suppression employé ${employee.fullName}")
            _statusBannerMessage.value = "Profil de ${employee.fullName} supprimé."
            if (isCloudSignedIn) {
                repository.deleteEmployee(employee.id, employee.fullName)
            }
        }
    }

    fun logMultiShiftAttendance(
        context: Context,
        employee: EmployeeProfile,
        date: String,
        shifts: List<ShiftIntervalInput>,
        dailyNote: String,
        performanceRating: Int
    ) {
        if (shifts.isEmpty()) {
            _statusBannerMessage.value = "Veuillez ajouter au moins un créneau horaire."
            return
        }
        val totalHours = shifts.sumOf { it.hours }.coerceIn(0.25, 24.0)
        val combinedCompanies = shifts.map { it.companyName.ifBlank { "Chantier Thermique" } }
            .distinct()
            .joinToString(" | ")
        val shiftsSummary = shifts.joinToString(" | ") { s ->
            val taskPart = if (s.taskNote.isNotBlank()) " (${s.taskNote})" else ""
            "${s.startTime}-${s.endTime} [${"%.1f".format(s.hours)}h] @ ${s.companyName.ifBlank { "Chantier" }}$taskPart"
        }
        val combinedTasks = shifts.mapNotNull { it.taskNote.takeIf { t -> t.isNotBlank() } }
            .joinToString(" ; ")

        val (reg, over, pay) = PayrollCalculator.computeDailyBreakdown(
            totalHours = totalHours,
            hourlyRate = employee.hourlyRate,
            overtimeBonusRate = employee.overtimeBonusRate
        )
        val monthKey = if (date.length >= 7) date.substring(0, 7) else currentMonthKey()

        viewModelScope.launch {
            val localId = UUID.randomUUID().toString()
            val localLog = AttendanceLog(
                id = localId,
                userId = currentUserId,
                employeeId = employee.id,
                employeeName = employee.fullName,
                date = date,
                monthKey = monthKey,
                companyName = combinedCompanies,
                shiftsSummary = shiftsSummary,
                taskDescription = combinedTasks,
                dailyNote = dailyNote,
                performanceRating = performanceRating.toLong(),
                totalHours = totalHours,
                regularHours = reg,
                overtimeHours = over,
                dailyPay = pay,
                recordedBy = "Admin Équipements Thermiques"
            )
            LocalOfflineStore.upsertAttendance(localLog)
            recordLocalAudit(
                "CREATE",
                "Pointage",
                "Pointage ${employee.fullName} ($date): ${totalHours}h = ${"%.2f".format(pay)} DH"
            )

            val msg = "Pointage synchronisé: ${employee.fullName} (${"%.1f".format(totalHours)}h -> Norm: ${"%.1f".format(reg)}h, Sup >9h: ${"%.1f".format(over)}h) = ${"%.2f".format(pay)} DH"
            _statusBannerMessage.value = msg
            if (_notificationsEnabled.value) {
                val alertTitle = if (over > 0) "Alerte Heures Sup (>9h) — ${employee.fullName}" else "Pointage Validé — ${employee.fullName}"
                ExportAndErpManager.sendSystemAlertNotification(context, alertTitle, msg)
            }

            if (isCloudSignedIn) {
                repository.recordAttendance(
                    employee = employee,
                    date = date,
                    companyName = combinedCompanies,
                    shiftsSummary = shiftsSummary,
                    taskDescription = combinedTasks,
                    dailyNote = dailyNote,
                    performanceRating = performanceRating,
                    totalHours = totalHours
                ).onSuccess { cloudId ->
                    if (cloudId != localId) {
                        LocalOfflineStore.removeAttendance(localId)
                        LocalOfflineStore.upsertAttendance(localLog.copy(id = cloudId))
                    }
                }
            }
        }
    }

    fun deleteAttendance(log: AttendanceLog) {
        viewModelScope.launch {
            LocalOfflineStore.removeAttendance(log.id)
            recordLocalAudit("DELETE", "Pointage", "Suppression pointage ${log.employeeName} (${log.date})")
            _statusBannerMessage.value = "Pointage du ${log.date} supprimé."
            if (isCloudSignedIn) {
                repository.deleteAttendanceLog(log.id, "${log.employeeName} (${log.date})")
            }
        }
    }

    fun giveSalaryAdvance(
        context: Context,
        employee: EmployeeProfile,
        amount: Double,
        date: String,
        note: String
    ) {
        if (amount <= 0.0) {
            _statusBannerMessage.value = "Le montant de l'avance doit être supérieur à 0 DH."
            return
        }
        val monthKey = if (date.length >= 7) date.substring(0, 7) else currentMonthKey()
        viewModelScope.launch {
            val localAdv = SalaryAdvance(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                employeeId = employee.id,
                employeeName = employee.fullName,
                amount = amount,
                date = date,
                monthKey = monthKey,
                note = note,
                recordedBy = "Admin Équipements Thermiques"
            )
            LocalOfflineStore.upsertAdvance(localAdv)
            recordLocalAudit("CREATE", "Avances", "Avance ${"%.0f".format(amount)} DH pour ${employee.fullName}")

            val msg = "Avance de ${"%.0f".format(amount)} DH synchronisée pour ${employee.fullName} (visible sur son Compte Salarié)."
            _statusBannerMessage.value = msg
            if (_notificationsEnabled.value) {
                ExportAndErpManager.sendSystemAlertNotification(context, "Avance Versée & Synchronisée", msg)
            }

            if (isCloudSignedIn) {
                repository.recordSalaryAdvance(employee, amount, date, note)
            }
        }
    }

    fun addCompany(
        name: String,
        city: String,
        contactName: String,
        phone: String,
        contractType: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val localComp = ClientCompany(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                name = name.trim(),
                city = city.trim(),
                contactName = contactName.trim(),
                phone = phone.trim(),
                contractType = contractType.trim()
            )
            LocalOfflineStore.upsertCompany(localComp)
            recordLocalAudit("CREATE", "Sociétés", "Ajout société $name ($city)")
            _statusBannerMessage.value = "Société '$name' ajoutée à la base de données."
            if (isCloudSignedIn) {
                repository.createClientCompany(name, city, contactName, phone, contractType)
            }
        }
    }

    fun addStockItem(
        context: Context,
        name: String,
        category: String,
        currentQuantity: Long,
        reorderPoint: Long,
        unit: String,
        unitPrice: Double,
        supplierName: String,
        dailyTaskNote: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val localItem = StockItem(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                name = name.trim(),
                category = category.trim(),
                currentQuantity = currentQuantity,
                reorderPoint = reorderPoint,
                unit = unit.trim(),
                unitPrice = unitPrice,
                supplierName = supplierName.trim(),
                dailyTaskNote = dailyTaskNote.trim()
            )
            LocalOfflineStore.upsertStockItem(localItem)
            recordLocalAudit("CREATE", "Stock", "Ajout article $name (Qté: $currentQuantity, Seuil: $reorderPoint)")
            _statusBannerMessage.value = "Article '$name' ajouté au stock."
            if (_notificationsEnabled.value && currentQuantity <= reorderPoint) {
                ExportAndErpManager.sendSystemAlertNotification(
                    context,
                    "Alerte Réapprovisionnement Stock",
                    "L'article $name ($currentQuantity $unit) est sous le seuil critique ($reorderPoint $unit)."
                )
            }
            if (isCloudSignedIn) {
                repository.createStockItem(
                    name = name,
                    category = category,
                    currentQuantity = currentQuantity,
                    reorderPoint = reorderPoint,
                    unit = unit,
                    unitPrice = unitPrice,
                    supplierName = supplierName,
                    dailyTaskNote = dailyTaskNote
                )
            }
        }
    }

    fun adjustStockQuantity(
        context: Context,
        item: StockItem,
        newQuantity: Long,
        taskNote: String
    ) {
        viewModelScope.launch {
            val updated = item.copy(currentQuantity = newQuantity, dailyTaskNote = taskNote)
            LocalOfflineStore.upsertStockItem(updated)
            recordLocalAudit("UPDATE", "Stock", "Mise à jour stock ${item.name}: $newQuantity ${item.unit}")
            if (newQuantity <= item.reorderPoint) {
                val alertMsg = "Stock bas pour ${item.name} ($newQuantity ${item.unit} <= seuil ${item.reorderPoint}). Réapprovisionnement suggéré!"
                _statusBannerMessage.value = alertMsg
                if (_notificationsEnabled.value) {
                    ExportAndErpManager.sendSystemAlertNotification(
                        context,
                        "Alerte Réapprovisionnement Temps Réel",
                        alertMsg
                    )
                }
            } else {
                _statusBannerMessage.value = "Stock ${item.name} mis à jour: $newQuantity ${item.unit}."
            }
            if (isCloudSignedIn) {
                repository.updateStockQuantityAndTask(item, newQuantity, taskNote)
            }
        }
    }

    fun addDailyProcurement(
        date: String,
        companyId: String,
        companyName: String,
        itemName: String,
        quantity: Long,
        unitCost: Double
    ) {
        if (itemName.isBlank()) return
        viewModelScope.launch {
            val proc = DailyProcurement(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                date = date,
                companyId = companyId,
                companyName = companyName,
                itemName = itemName.trim(),
                quantity = quantity,
                unitCost = unitCost,
                totalCost = quantity * unitCost,
                status = "NEEDED"
            )
            LocalOfflineStore.upsertProcurement(proc)
            recordLocalAudit("CREATE", "Achats Jour", "Achat ${quantity}x $itemName pour $companyName")
            _statusBannerMessage.value = "Besoin du jour ajouté: ${quantity}x $itemName pour $companyName."
            if (isCloudSignedIn) {
                repository.createDailyProcurement(
                    date = date,
                    companyId = companyId,
                    companyName = companyName,
                    itemName = itemName,
                    quantity = quantity,
                    unitCost = unitCost
                )
            }
        }
    }

    fun cycleProcurementStatus(procurement: DailyProcurement) {
        val next = when (procurement.status) {
            "NEEDED" -> "ORDERED"
            "ORDERED" -> "RECEIVED"
            else -> "NEEDED"
        }
        viewModelScope.launch {
            LocalOfflineStore.upsertProcurement(procurement.copy(status = next))
            _statusBannerMessage.value = "Statut approvisionnement ${procurement.itemName} -> $next"
            if (isCloudSignedIn) {
                repository.updateProcurementStatus(procurement, next)
            }
        }
    }

    fun addSupplier(
        name: String,
        contactInfo: String,
        paymentTerms: String,
        invoiceReference: String,
        amountDue: Double,
        amountPaid: Double,
        dueDate: String,
        status: String,
        reminderEnabled: Boolean
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val sup = SupplierRecord(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                name = name.trim(),
                contactInfo = contactInfo.trim(),
                paymentTerms = paymentTerms.trim(),
                invoiceReference = invoiceReference.trim(),
                amountDue = amountDue,
                amountPaid = amountPaid,
                dueDate = dueDate.trim(),
                status = status,
                reminderEnabled = reminderEnabled
            )
            LocalOfflineStore.upsertSupplier(sup)
            recordLocalAudit("CREATE", "Fournisseurs", "Ajout fournisseur $name (Échéance $dueDate)")
            _statusBannerMessage.value = "Fournisseur $name enregistré (Échéance: $dueDate)."
            if (isCloudSignedIn) {
                repository.createSupplierRecord(
                    name = name,
                    contactInfo = contactInfo,
                    paymentTerms = paymentTerms,
                    invoiceReference = invoiceReference,
                    amountDue = amountDue,
                    amountPaid = amountPaid,
                    dueDate = dueDate,
                    status = status,
                    reminderEnabled = reminderEnabled
                )
            }
        }
    }

    fun paySupplierInstallment(supplier: SupplierRecord, paymentAmount: Double) {
        viewModelScope.launch {
            val newPaid = (supplier.amountPaid + paymentAmount).coerceAtMost(supplier.amountDue)
            val newStatus = if (newPaid >= supplier.amountDue) "PAID" else "PARTIAL"
            LocalOfflineStore.upsertSupplier(supplier.copy(amountPaid = newPaid, status = newStatus))
            recordLocalAudit("UPDATE", "Fournisseurs", "Paiement ${"%.0f".format(paymentAmount)} DH pour ${supplier.name}")
            _statusBannerMessage.value = "Paiement de ${"%.0f".format(paymentAmount)} DH enregistré pour ${supplier.name}."
            if (isCloudSignedIn) {
                repository.recordSupplierPayment(supplier, paymentAmount)
            }
        }
    }

    // --- EXPORTS & EMAIL AUTOMATION ---

    fun exportFullPdfReport(context: Context) {
        viewModelScope.launch {
            try {
                val month = _selectedMonth.value
                val pdf = ExportAndErpManager.generateFullPdfReport(
                    context = context,
                    monthKey = month,
                    summaries = payrollSummaries.value,
                    attendanceLogs = attendanceLogs.value,
                    advances = advances.value,
                    stockItems = stockItems.value,
                    procurements = dailyProcurements.value,
                    suppliers = suppliers.value,
                    auditLogs = auditLogs.value
                )
                recordLocalAudit("EXPORT", "Export PDF", "Génération rapport PDF ($month): ${pdf.name}")
                if (isCloudSignedIn) {
                    repository.logAuditEvent(
                        actionType = "EXPORT",
                        module = "Export PDF",
                        details = "Génération du rapport PDF complet ($month): ${pdf.name}"
                    )
                }
                _statusBannerMessage.value = "Rapport PDF généré: ${pdf.name}"
                ExportAndErpManager.shareReportFile(
                    context = context,
                    file = pdf,
                    mimeType = "application/pdf",
                    subject = "Rapport Mensuel Pointage, Paie & Stock — Équipements Thermiques ($month)",
                    bodyText = "Bonjour,\n\nVeuillez trouver ci-joint le rapport PDF complet (pointage 9h+, avances, soldes, stock et fournisseurs) de la société Équipements Thermiques pour la période $month."
                )
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erreur export PDF: ${e.localizedMessage}"
            }
        }
    }

    fun exportExcelCsv(context: Context) {
        viewModelScope.launch {
            try {
                val month = _selectedMonth.value
                val csv = ExportAndErpManager.generateExcelCsv(
                    context = context,
                    monthKey = month,
                    summaries = payrollSummaries.value,
                    attendanceLogs = attendanceLogs.value,
                    advances = advances.value,
                    suppliers = suppliers.value,
                    procurements = dailyProcurements.value
                )
                recordLocalAudit("EXPORT", "Export Excel (CSV)", "Export CSV Excel ($month): ${csv.name}")
                if (isCloudSignedIn) {
                    repository.logAuditEvent(
                        actionType = "EXPORT",
                        module = "Export Excel (CSV)",
                        details = "Export CSV Excel complet ($month): ${csv.name}"
                    )
                }
                _statusBannerMessage.value = "Fichier Excel/CSV généré: ${csv.name}"
                ExportAndErpManager.shareReportFile(
                    context = context,
                    file = csv,
                    mimeType = "text/csv",
                    subject = "Export Excel Pointage, Paie & Stock — Équipements Thermiques ($month)",
                    bodyText = "Veuillez trouver ci-joint le fichier CSV compatible Excel pour la période $month."
                )
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erreur export CSV: ${e.localizedMessage}"
            }
        }
    }

    fun sendAutomatedMonthlyEmailReport(context: Context) {
        viewModelScope.launch {
            try {
                val month = _selectedMonth.value
                val summaries = payrollSummaries.value
                val pdf = ExportAndErpManager.generateFullPdfReport(
                    context = context,
                    monthKey = month,
                    summaries = summaries,
                    attendanceLogs = attendanceLogs.value,
                    advances = advances.value,
                    stockItems = stockItems.value,
                    procurements = dailyProcurements.value,
                    suppliers = suppliers.value,
                    auditLogs = auditLogs.value
                )
                val recipients = summaries.mapNotNull { it.employee.workerEmail.takeIf { e -> e.contains("@") } }
                    .toTypedArray()
                val emailBody = buildString {
                    appendLine("ÉQUIPEMENTS THERMIQUES — RAPPORT AUTOMATISÉ DE PAIE & POINTAGE ($month)")
                    appendLine("==================================================================")
                    summaries.forEach { s ->
                        appendLine("• ${s.employee.fullName} (${s.employee.specialty}):")
                        appendLine("  - Heures travaillées: ${"%.1f".format(s.totalHours)}h (Normales <=9h: ${"%.1f".format(s.regularHoursTotal)}h | Sup >9h: ${"%.1f".format(s.overtimeHoursTotal)}h)")
                        appendLine("  - Total Brut (+${s.employee.commissionPercent}%): ${"%.2f".format(s.grossEarnings)} DH")
                        appendLine("  - Avances versées: -${"%.2f".format(s.totalAdvances)} DH")
                        appendLine("  - RESTE À PAYER: ${"%.2f".format(s.remainingToPay)} DH (Mois précédent: ${"%.2f".format(s.employee.lastMonthPaidTotal)} DH)")
                        appendLine()
                    }
                }
                recordLocalAudit("EXPORT", "Email Automation", "Envoi rapport mensuel par e-mail ($month)")
                if (isCloudSignedIn) {
                    repository.logAuditEvent(
                        actionType = "EXPORT",
                        module = "Email Automation",
                        details = "Envoi rapport mensuel par e-mail ($month) à ${recipients.size} destinataires"
                    )
                }
                ExportAndErpManager.shareReportFile(
                    context = context,
                    file = pdf,
                    mimeType = "application/pdf",
                    subject = "[Équipements Thermiques] Relevé Mensuel de Pointage & Paie ($month)",
                    bodyText = emailBody,
                    recipientEmails = recipients
                )
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erreur envoi e-mail: ${e.localizedMessage}"
            }
        }
    }

    fun exportIphoneWebApp(context: Context) {
        viewModelScope.launch {
            try {
                val month = _selectedMonth.value
                val htmlFile = ExportAndErpManager.generateIphoneWebPortalApp(
                    context = context,
                    monthKey = month,
                    summaries = payrollSummaries.value,
                    stockItems = stockItems.value,
                    suppliers = suppliers.value
                )
                recordLocalAudit("EXPORT", "Portail iPhone (iOS)", "Génération Web App iPhone/iOS ($month)")
                _statusBannerMessage.value = "Application Web iPhone (iOS PWA) générée: ${htmlFile.name}"
                ExportAndErpManager.shareReportFile(
                    context = context,
                    file = htmlFile,
                    mimeType = "text/html",
                    subject = "Application ThermoPoint Pro pour iPhone (iOS) — Équipements Thermiques ($month)",
                    bodyText = "Ouvrez ce fichier sur votre iPhone dans Safari, puis touchez 'Partager -> Sur l'écran d'accueil' pour installer ThermoPoint Pro sur iOS."
                )
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erreur génération iPhone App: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Populates realistic initial data for Équipements Thermiques in both LocalOfflineStore (works 100% offline)
     * and Cloud Firestore (when signed in).
     */
    fun seedInitialCompanyData() {
        if (_isSeeding.value) return
        viewModelScope.launch {
            _isSeeding.value = true
            try {
                val month = _selectedMonth.value
                val today = todayDateString()

                val comp1 = ClientCompany(
                    id = "comp_atlas",
                    userId = currentUserId,
                    name = "Atlas Agro-Thermique SA",
                    city = "Casablanca - Ain Sebaa",
                    contactName = "M. Bennani",
                    phone = "+212 661-234567",
                    contractType = "Installation Chaudières Vapeur"
                )
                val comp2 = ClientCompany(
                    id = "comp_clinique",
                    userId = currentUserId,
                    name = "Clinique Internationale du Sud",
                    city = "Marrakech",
                    contactName = "Dr. El Fassi",
                    phone = "+212 662-987654",
                    contractType = "Maintenance CVC & Eau Chaude Solaire"
                )
                val comp3 = ClientCompany(
                    id = "comp_mogador",
                    userId = currentUserId,
                    name = "Complexe Hôtelier Mogador",
                    city = "Essaouira",
                    contactName = "Mme. Alaoui",
                    phone = "+212 663-445566",
                    contractType = "Pompes à Chaleur & Brûleurs"
                )
                LocalOfflineStore.upsertCompany(comp1)
                LocalOfflineStore.upsertCompany(comp2)
                LocalOfflineStore.upsertCompany(comp3)

                val emp1 = EmployeeProfile(
                    id = "emp_youssef",
                    userId = currentUserId,
                    fullName = "Youssef El Mansouri",
                    workerEmail = "youssef.mansouri@equipements-thermiques.ma",
                    role = "WORKER",
                    specialty = "Chef Monteur Chaudières",
                    hourlyRate = 55.0,
                    overtimeBonusRate = 80.0,
                    commissionPercent = 5.0,
                    lastMonthPaidTotal = 11450.0
                )
                val emp2 = EmployeeProfile(
                    id = "emp_hamza",
                    userId = currentUserId,
                    fullName = "Hamza Berrada",
                    workerEmail = "hamza.berrada@equipements-thermiques.ma",
                    role = "WORKER",
                    specialty = "Technicien Brûleurs & Régulation",
                    hourlyRate = 45.0,
                    overtimeBonusRate = 65.0,
                    commissionPercent = 4.0,
                    lastMonthPaidTotal = 9200.0
                )
                val emp3 = EmployeeProfile(
                    id = "emp_rachid",
                    userId = currentUserId,
                    fullName = "Rachid Ait Oufkir",
                    workerEmail = "rachid.oufkir@equipements-thermiques.ma",
                    role = "MANAGER",
                    specialty = "Frigoriste & Tuyauteur Haute Pression",
                    hourlyRate = 60.0,
                    overtimeBonusRate = 90.0,
                    commissionPercent = 6.0,
                    lastMonthPaidTotal = 12800.0
                )
                LocalOfflineStore.upsertEmployee(emp1)
                LocalOfflineStore.upsertEmployee(emp2)
                LocalOfflineStore.upsertEmployee(emp3)

                LocalOfflineStore.upsertAttendance(
                    AttendanceLog(
                        id = "att_1",
                        userId = currentUserId,
                        employeeId = emp1.id,
                        employeeName = emp1.fullName,
                        date = "$month-01",
                        monthKey = month,
                        companyName = "Atlas Agro-Thermique SA | Clinique Internationale du Sud",
                        shiftsSummary = "08:00-13:00 [5.0h] @ Atlas Agro-Thermique SA (Soudure collecteur vapeur) | 14:00-20:00 [6.0h] @ Clinique Internationale du Sud (Dépannage brûleur gaz)",
                        taskDescription = "Soudure collecteur vapeur ; Dépannage brûleur gaz",
                        dailyNote = "A travaillé dans 2 sociétés le même jour (11h total = 9h norm + 2h sup bonus)",
                        performanceRating = 5L,
                        totalHours = 11.0,
                        regularHours = 9.0,
                        overtimeHours = 2.0,
                        dailyPay = 655.0,
                        recordedBy = "Admin"
                    )
                )
                LocalOfflineStore.upsertAttendance(
                    AttendanceLog(
                        id = "att_2",
                        userId = currentUserId,
                        employeeId = emp2.id,
                        employeeName = emp2.fullName,
                        date = "$month-01",
                        monthKey = month,
                        companyName = "Atlas Agro-Thermique SA",
                        shiftsSummary = "08:00-17:00 [9.0h] @ Atlas Agro-Thermique SA (Câblage armoire de régulation)",
                        taskDescription = "Câblage armoire de régulation",
                        dailyNote = "Journée complète standard 9h validée",
                        performanceRating = 4L,
                        totalHours = 9.0,
                        regularHours = 9.0,
                        overtimeHours = 0.0,
                        dailyPay = 405.0,
                        recordedBy = "Admin"
                    )
                )
                LocalOfflineStore.upsertAttendance(
                    AttendanceLog(
                        id = "att_3",
                        userId = currentUserId,
                        employeeId = emp3.id,
                        employeeName = emp3.fullName,
                        date = "$month-01",
                        monthKey = month,
                        companyName = "Complexe Hôtelier Mogador",
                        shiftsSummary = "07:30-18:00 [10.5h] @ Complexe Hôtelier Mogador (Mise en service PAC 120kW)",
                        taskDescription = "Mise en service PAC 120kW",
                        dailyNote = "1.5h supplémentaires validées avec client",
                        performanceRating = 5L,
                        totalHours = 10.5,
                        regularHours = 9.0,
                        overtimeHours = 1.5,
                        dailyPay = 675.0,
                        recordedBy = "Admin"
                    )
                )

                LocalOfflineStore.upsertAdvance(
                    SalaryAdvance(
                        id = "adv_1",
                        userId = currentUserId,
                        employeeId = emp1.id,
                        employeeName = emp1.fullName,
                        amount = 300.0,
                        date = "$month-01",
                        monthKey = month,
                        note = "Avance espèces chantier Casablanca",
                        recordedBy = "Admin Équipements Thermiques"
                    )
                )
                LocalOfflineStore.upsertAdvance(
                    SalaryAdvance(
                        id = "adv_2",
                        userId = currentUserId,
                        employeeId = emp2.id,
                        employeeName = emp2.fullName,
                        amount = 150.0,
                        date = "$month-01",
                        monthKey = month,
                        note = "Avance transport & déplacement",
                        recordedBy = "Admin Équipements Thermiques"
                    )
                )

                LocalOfflineStore.upsertStockItem(
                    StockItem(
                        id = "stk_1",
                        userId = currentUserId,
                        name = "Brûleur Gaz Modulant Riello RS50",
                        category = "Brûleurs",
                        currentQuantity = 2L,
                        reorderPoint = 4L,
                        unit = "Pièce",
                        unitPrice = 14500.0,
                        supplierName = "Maghreb Thermo-Distribution",
                        dailyTaskNote = "Sortie 1 unité pour Clinique Marrakech — Réapprovisionnement urgent requis"
                    )
                )
                LocalOfflineStore.upsertStockItem(
                    StockItem(
                        id = "stk_2",
                        userId = currentUserId,
                        name = "Vanne Motorisée 3 Voies DN65 Siemens",
                        category = "Tuyauterie & Vannes",
                        currentQuantity = 3L,
                        reorderPoint = 6L,
                        unit = "Pièce",
                        unitPrice = 2850.0,
                        supplierName = "Casa Hydro-Vannes",
                        dailyTaskNote = "Vérification étanchéité lot B"
                    )
                )
                LocalOfflineStore.upsertStockItem(
                    StockItem(
                        id = "stk_3",
                        userId = currentUserId,
                        name = "Sonde de Température PT1000 Immersion",
                        category = "Régulation & Sondes",
                        currentQuantity = 18L,
                        reorderPoint = 8L,
                        unit = "Pièce",
                        unitPrice = 320.0,
                        supplierName = "EuroTherm Maroc",
                        dailyTaskNote = "Stock suffisant — Inventaire contrôlé"
                    )
                )

                LocalOfflineStore.upsertProcurement(
                    DailyProcurement(
                        id = "proc_1",
                        userId = currentUserId,
                        date = today,
                        companyId = comp1.id,
                        companyName = comp1.name,
                        itemName = "Soupape de sécurité vapeur 10 bars DN40",
                        quantity = 3L,
                        unitCost = 1650.0,
                        totalCost = 4950.0,
                        status = "NEEDED"
                    )
                )
                LocalOfflineStore.upsertProcurement(
                    DailyProcurement(
                        id = "proc_2",
                        userId = currentUserId,
                        date = today,
                        companyId = comp2.id,
                        companyName = comp2.name,
                        itemName = "Circulateur haut rendement Grundfos Magna3",
                        quantity = 2L,
                        unitCost = 6400.0,
                        totalCost = 12800.0,
                        status = "ORDERED"
                    )
                )

                LocalOfflineStore.upsertSupplier(
                    SupplierRecord(
                        id = "sup_1",
                        userId = currentUserId,
                        name = "Maghreb Thermo-Distribution",
                        contactInfo = "+212 522-304050 | compta@maghrebthermo.ma",
                        paymentTerms = "30 Jours FDM",
                        invoiceReference = "FAC-MTD-2026-884",
                        amountDue = 48500.0,
                        amountPaid = 20000.0,
                        dueDate = "$month-15",
                        status = "PARTIAL",
                        reminderEnabled = true
                    )
                )
                LocalOfflineStore.upsertSupplier(
                    SupplierRecord(
                        id = "sup_2",
                        userId = currentUserId,
                        name = "Casa Hydro-Vannes SARL",
                        contactInfo = "+212 522-441122 | ventes@casahydro.ma",
                        paymentTerms = "60 Jours",
                        invoiceReference = "FAC-CHV-2026-319",
                        amountDue = 19200.0,
                        amountPaid = 0.0,
                        dueDate = "$month-10",
                        status = "OVERDUE",
                        reminderEnabled = true
                    )
                )

                recordLocalAudit(
                    "CREATE",
                    "Initialisation",
                    "Données Équipements Thermiques chargées (Hors-Ligne + Synchronisation Cloud)"
                )

                if (isCloudSignedIn) {
                    repository.createClientCompany(comp1.name, comp1.city, comp1.contactName, comp1.phone, comp1.contractType)
                    repository.createClientCompany(comp2.name, comp2.city, comp2.contactName, comp2.phone, comp2.contractType)
                    repository.createClientCompany(comp3.name, comp3.city, comp3.contactName, comp3.phone, comp3.contractType)
                }

                _statusBannerMessage.value = "Données Équipements Thermiques chargées et synchronisées (Hors-ligne & Cloud)."
            } catch (e: Exception) {
                _statusBannerMessage.value = "Erreur chargement données: ${e.localizedMessage}"
            } finally {
                _isSeeding.value = false
            }
        }
    }

    companion object {
        private const val TAG = "ThermoPointVM"

        fun currentMonthKey(): String {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        }

        fun todayDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        fun provideFactory(
            repository: ThermoPointRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ThermoPointViewModel(repository, currentUserId)
            }
        }
    }
}
