package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceLog
import com.example.data.model.AuditLogEntry
import com.example.data.model.ClientCompany
import com.example.data.model.DailyProcurement
import com.example.data.model.EmployeeProfile
import com.example.data.model.PayrollCalculator
import com.example.data.model.SalaryAdvance
import com.example.data.model.StockItem
import com.example.data.model.SupplierRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ThermoPointRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentActorName(): String {
        return auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.takeIf { it.isNotBlank() }
            ?: "Admin Équipements Thermiques"
    }

    // --- REALTIME OBSERVATION FLOWS (Scoped to userId) ---

    fun observeEmployees(): Flow<List<EmployeeProfile>> = flow {
        val uid = requireUserId()
        val path = "employees"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EmployeeProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.fullName }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAttendanceLogs(): Flow<List<AttendanceLog>> = flow {
        val uid = requireUserId()
        val path = "attendance_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AttendanceLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAdvances(): Flow<List<SalaryAdvance>> = flow {
        val uid = requireUserId()
        val path = "advances"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SalaryAdvance::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCompanies(): Flow<List<ClientCompany>> = flow {
        val uid = requireUserId()
        val path = "companies"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClientCompany::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeStockItems(): Flow<List<StockItem>> = flow {
        val uid = requireUserId()
        val path = "stock_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StockItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeDailyProcurements(): Flow<List<DailyProcurement>> = flow {
        val uid = requireUserId()
        val path = "daily_procurements"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyProcurement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSuppliers(): Flow<List<SupplierRecord>> = flow {
        val uid = requireUserId()
        val path = "suppliers"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupplierRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.dueDate }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> = flow {
        val uid = requireUserId()
        val path = "audit_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AuditLogEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- CRUD OPERATIONS WITH AUTOMATIC AUDIT LOGGING ---

    suspend fun logAuditEvent(
        actionType: String,
        module: String,
        details: String,
        actorRole: String = "ADMIN"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("audit_logs").document()
        val payload = mapOf(
            "userId" to uid,
            "actorName" to currentActorName().take(100),
            "actorRole" to actorRole.take(40),
            "actionType" to actionType,
            "module" to module.take(60),
            "details" to details.take(400),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createEmployee(
        fullName: String,
        workerEmail: String,
        role: String,
        specialty: String,
        hourlyRate: Double,
        overtimeBonusRate: Double,
        commissionPercent: Double,
        lastMonthPaidTotal: Double
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("employees").document()
        val payload = mapOf(
            "userId" to uid,
            "fullName" to fullName.trim().take(100),
            "workerEmail" to workerEmail.trim().take(150),
            "role" to role,
            "specialty" to specialty.trim().ifBlank { "Technicien Thermique" }.take(80),
            "hourlyRate" to hourlyRate,
            "overtimeBonusRate" to overtimeBonusRate,
            "commissionPercent" to commissionPercent,
            "lastMonthPaidTotal" to lastMonthPaidTotal,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Employés & Tarifs",
                details = "Ajout de $fullName ($specialty) — Tarif: ${hourlyRate} DH/h, Bonus >9h: ${overtimeBonusRate} DH/h, Rôle: $role"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateEmployee(employee: EmployeeProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(employee.id.isNotBlank()) { "Employee ID must not be blank" }
        requireNotNull(employee.createdAt) { "Employee createdAt must not be null for update" }
        val docRef = db.collection("employees").document(employee.id)
        try {
            docRef.set(employee.copy(userId = uid, updatedAt = null)).await()
            logAuditEvent(
                actionType = "UPDATE",
                module = "Employés & Tarifs",
                details = "Mise à jour de ${employee.fullName} — Tarif: ${employee.hourlyRate} DH/h, Rôle: ${employee.role}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteEmployee(employeeId: String, employeeName: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("employees").document(employeeId)
        try {
            docRef.delete().await()
            logAuditEvent(
                actionType = "DELETE",
                module = "Employés & Tarifs",
                details = "Suppression du profil employé: $employeeName"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getEmployeeById(employeeId: String): Result<EmployeeProfile?> = runCatching {
        requireUserId()
        val docRef = db.collection("employees").document(employeeId)
        try {
            docRef.get().await().toObject(
                EmployeeProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun recordAttendance(
        employee: EmployeeProfile,
        date: String,
        companyName: String,
        shiftsSummary: String,
        taskDescription: String,
        dailyNote: String,
        performanceRating: Int,
        totalHours: Double
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("attendance_logs").document()
        val monthKey = if (date.length >= 7) date.substring(0, 7) else "2026-11"
        val (regularHours, overtimeHours, dailyPay) = PayrollCalculator.computeDailyBreakdown(
            totalHours = totalHours,
            hourlyRate = employee.hourlyRate,
            overtimeBonusRate = employee.overtimeBonusRate
        )
        val ratingLong = performanceRating.coerceIn(1, 5).toLong()
        val payload = mapOf(
            "userId" to uid,
            "employeeId" to employee.id,
            "employeeName" to employee.fullName.take(100),
            "date" to date.take(20),
            "monthKey" to monthKey.take(10),
            "companyName" to companyName.ifBlank { "Chantier Principal" }.take(120),
            "shiftsSummary" to shiftsSummary.ifBlank { "08:00-17:00 ($totalHours h)" }.take(500),
            "taskDescription" to taskDescription.take(300),
            "dailyNote" to dailyNote.take(300),
            "performanceRating" to ratingLong,
            "totalHours" to totalHours,
            "regularHours" to regularHours,
            "overtimeHours" to overtimeHours,
            "dailyPay" to dailyPay,
            "recordedBy" to currentActorName().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Pointage Journalier",
                details = "Pointage ${employee.fullName} ($date): ${totalHours}h (Norm: ${regularHours}h, Sup >9h: ${overtimeHours}h) chez $companyName = ${"%.2f".format(dailyPay)} DH"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAttendanceLog(logId: String, summary: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("attendance_logs").document(logId)
        try {
            docRef.delete().await()
            logAuditEvent(
                actionType = "DELETE",
                module = "Pointage Journalier",
                details = "Suppression pointage: $summary"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun recordSalaryAdvance(
        employee: EmployeeProfile,
        amount: Double,
        date: String,
        note: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("advances").document()
        val monthKey = if (date.length >= 7) date.substring(0, 7) else "2026-11"
        val payload = mapOf(
            "userId" to uid,
            "employeeId" to employee.id,
            "employeeName" to employee.fullName.take(100),
            "amount" to amount,
            "date" to date.take(20),
            "monthKey" to monthKey.take(10),
            "note" to note.take(250),
            "recordedBy" to currentActorName().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Avances & Paie",
                details = "Avance de ${"%.2f".format(amount)} DH accordée à ${employee.fullName} le $date ($note)"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createClientCompany(
        name: String,
        city: String,
        contactName: String,
        phone: String,
        contractType: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("companies").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(120),
            "city" to city.trim().ifBlank { "Casablanca" }.take(80),
            "contactName" to contactName.trim().take(100),
            "phone" to phone.trim().take(40),
            "contractType" to contractType.trim().ifBlank { "Installation & Maintenance" }.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Sociétés Clientes",
                details = "Ajout société cliente: $name ($city) — Contrat: $contractType"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createStockItem(
        name: String,
        category: String,
        currentQuantity: Long,
        reorderPoint: Long,
        unit: String,
        unitPrice: Double,
        supplierName: String,
        dailyTaskNote: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("stock_items").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(120),
            "category" to category.trim().ifBlank { "Chaudières" }.take(60),
            "currentQuantity" to currentQuantity.coerceAtLeast(0L),
            "reorderPoint" to reorderPoint.coerceAtLeast(0L),
            "unit" to unit.trim().ifBlank { "Pièce" }.take(30),
            "unitPrice" to unitPrice.coerceAtLeast(0.0),
            "supplierName" to supplierName.trim().take(120),
            "dailyTaskNote" to dailyTaskNote.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Stock & Inventaire",
                details = "Ajout article stock: $name ($currentQuantity $unit, seuil alerte: $reorderPoint)"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateStockQuantityAndTask(
        item: StockItem,
        newQuantity: Long,
        newDailyTaskNote: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "StockItem ID must not be blank" }
        requireNotNull(item.createdAt) { "StockItem createdAt must not be null" }
        val docRef = db.collection("stock_items").document(item.id)
        val updated = item.copy(
            userId = uid,
            currentQuantity = newQuantity.coerceAtLeast(0L),
            dailyTaskNote = newDailyTaskNote.take(300),
            updatedAt = null
        )
        try {
            docRef.set(updated).await()
            logAuditEvent(
                actionType = "UPDATE",
                module = "Stock & Inventaire",
                details = "Mouvement stock ${item.name}: quantité ${item.currentQuantity} -> $newQuantity ${item.unit}. Note: $newDailyTaskNote"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun createDailyProcurement(
        date: String,
        companyId: String,
        companyName: String,
        itemName: String,
        quantity: Long,
        unitCost: Double,
        status: String = "NEEDED"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("daily_procurements").document()
        val safeQty = quantity.coerceAtLeast(1L)
        val totalCost = safeQty * unitCost
        val payload = mapOf(
            "userId" to uid,
            "date" to date.take(20),
            "companyId" to companyId.ifBlank { "general" }.take(128),
            "companyName" to companyName.ifBlank { "Stock Central" }.take(120),
            "itemName" to itemName.trim().take(120),
            "quantity" to safeQty,
            "unitCost" to unitCost,
            "totalCost" to totalCost,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Achats du Jour",
                details = "Besoin marchandise ($date) pour $companyName: $safeQty x $itemName (${"%.2f".format(totalCost)} DH)"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProcurementStatus(
        procurement: DailyProcurement,
        newStatus: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(procurement.id.isNotBlank()) { "Procurement ID must not be blank" }
        requireNotNull(procurement.createdAt) { "Procurement createdAt must not be null" }
        val docRef = db.collection("daily_procurements").document(procurement.id)
        try {
            docRef.set(procurement.copy(userId = uid, status = newStatus, updatedAt = null)).await()
            logAuditEvent(
                actionType = "UPDATE",
                module = "Achats du Jour",
                details = "Statut achat ${procurement.itemName} (${procurement.companyName}) -> $newStatus"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun createSupplierRecord(
        name: String,
        contactInfo: String,
        paymentTerms: String,
        invoiceReference: String,
        amountDue: Double,
        amountPaid: Double,
        dueDate: String,
        status: String,
        reminderEnabled: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("suppliers").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(120),
            "contactInfo" to contactInfo.trim().take(150),
            "paymentTerms" to paymentTerms.trim().ifBlank { "30 Jours FDM" }.take(80),
            "invoiceReference" to invoiceReference.trim().ifBlank { "FAC-2026" }.take(80),
            "amountDue" to amountDue.coerceAtLeast(0.0),
            "amountPaid" to amountPaid.coerceAtLeast(0.0),
            "dueDate" to dueDate.take(20),
            "status" to status,
            "reminderEnabled" to reminderEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAuditEvent(
                actionType = "CREATE",
                module = "Fournisseurs & Échéances",
                details = "Ajout fournisseur $name (Facture $invoiceReference, Échéance: $dueDate, Montant: ${"%.2f".format(amountDue)} DH)"
            )
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordSupplierPayment(
        supplier: SupplierRecord,
        additionalPayment: Double
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(supplier.id.isNotBlank()) { "Supplier ID must not be blank" }
        requireNotNull(supplier.createdAt) { "Supplier createdAt must not be null" }
        val newPaid = (supplier.amountPaid + additionalPayment).coerceAtMost(supplier.amountDue)
        val newStatus = when {
            newPaid >= supplier.amountDue -> "PAID"
            newPaid > 0.0 -> "PARTIAL"
            else -> supplier.status
        }
        val docRef = db.collection("suppliers").document(supplier.id)
        try {
            docRef.set(
                supplier.copy(
                    userId = uid,
                    amountPaid = newPaid,
                    status = newStatus,
                    updatedAt = null
                )
            ).await()
            logAuditEvent(
                actionType = "UPDATE",
                module = "Fournisseurs & Échéances",
                details = "Règlement fournisseur ${supplier.name} (${supplier.invoiceReference}): +${"%.2f".format(additionalPayment)} DH -> Statut $newStatus"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
