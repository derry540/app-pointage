package com.example.data.repository

import android.content.Context
import com.example.data.model.AttendanceLog
import com.example.data.model.AuditLogEntry
import com.example.data.model.ClientCompany
import com.example.data.model.DailyProcurement
import com.example.data.model.EmployeeProfile
import com.example.data.model.SalaryAdvance
import com.example.data.model.StockItem
import com.example.data.model.SupplierRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent Offline-First Local Store that guarantees ThermoPoint Pro works 100% without internet ("Sans Connexion")
 * and immediately synchronizes Admin updates (payroll, advances, attendance, stock) with Worker (non-admin) sessions,
 * while also syncing with Firebase Firestore when connected via *3 / 4G / Wi-Fi.
 */
object LocalOfflineStore {

    private const val PREFS_NAME = "thermopoint_offline_sync_store"
    private const val KEY_EMPLOYEES = "employees_json"
    private const val KEY_ATTENDANCE = "attendance_json"
    private const val KEY_ADVANCES = "advances_json"
    private const val KEY_COMPANIES = "companies_json"
    private const val KEY_STOCK = "stock_json"
    private const val KEY_PROCUREMENTS = "procurements_json"
    private const val KEY_SUPPLIERS = "suppliers_json"
    private const val KEY_AUDIT = "audit_json"
    private const val KEY_ADMIN_PIN = "admin_pin_code"
    private const val KEY_WORKER_PIN = "worker_pin_code"

    private var appContext: Context? = null

    private val _employees = MutableStateFlow<List<EmployeeProfile>>(emptyList())
    val employees: StateFlow<List<EmployeeProfile>> = _employees.asStateFlow()

    private val _attendanceLogs = MutableStateFlow<List<AttendanceLog>>(emptyList())
    val attendanceLogs: StateFlow<List<AttendanceLog>> = _attendanceLogs.asStateFlow()

    private val _advances = MutableStateFlow<List<SalaryAdvance>>(emptyList())
    val advances: StateFlow<List<SalaryAdvance>> = _advances.asStateFlow()

    private val _companies = MutableStateFlow<List<ClientCompany>>(emptyList())
    val companies: StateFlow<List<ClientCompany>> = _companies.asStateFlow()

    private val _stockItems = MutableStateFlow<List<StockItem>>(emptyList())
    val stockItems: StateFlow<List<StockItem>> = _stockItems.asStateFlow()

    private val _procurements = MutableStateFlow<List<DailyProcurement>>(emptyList())
    val procurements: StateFlow<List<DailyProcurement>> = _procurements.asStateFlow()

    private val _suppliers = MutableStateFlow<List<SupplierRecord>>(emptyList())
    val suppliers: StateFlow<List<SupplierRecord>> = _suppliers.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadAllFromDisk()
        }
    }

    fun getAdminPin(context: Context): String {
        init(context)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADMIN_PIN, "1234") ?: "1234"
    }

    fun getWorkerPin(context: Context): String {
        init(context)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WORKER_PIN, "0000") ?: "0000"
    }

    fun upsertEmployee(employee: EmployeeProfile) {
        val updated = (_employees.value.filterNot { it.id == employee.id } + employee)
            .sortedBy { it.fullName }
        _employees.value = updated
        saveEmployeesToDisk(updated)
    }

    fun removeEmployee(employeeId: String) {
        val updated = _employees.value.filterNot { it.id == employeeId }
        _employees.value = updated
        saveEmployeesToDisk(updated)
    }

    fun mergeCloudEmployees(cloudList: List<EmployeeProfile>) {
        if (cloudList.isEmpty()) return
        val byId = (_employees.value + cloudList).associateBy { it.id }
        val merged = byId.values.sortedBy { it.fullName }
        _employees.value = merged
        saveEmployeesToDisk(merged)
    }

    fun upsertAttendance(log: AttendanceLog) {
        val updated = (listOf(log) + _attendanceLogs.value.filterNot { it.id == log.id })
        _attendanceLogs.value = updated
        saveAttendanceToDisk(updated)
    }

    fun removeAttendance(logId: String) {
        val updated = _attendanceLogs.value.filterNot { it.id == logId }
        _attendanceLogs.value = updated
        saveAttendanceToDisk(updated)
    }

    fun mergeCloudAttendance(cloudList: List<AttendanceLog>) {
        if (cloudList.isEmpty()) return
        val byId = (_attendanceLogs.value + cloudList).associateBy { it.id }
        val merged = byId.values.toList()
        _attendanceLogs.value = merged
        saveAttendanceToDisk(merged)
    }

    fun upsertAdvance(advance: SalaryAdvance) {
        val updated = (listOf(advance) + _advances.value.filterNot { it.id == advance.id })
        _advances.value = updated
        saveAdvancesToDisk(updated)
    }

    fun mergeCloudAdvances(cloudList: List<SalaryAdvance>) {
        if (cloudList.isEmpty()) return
        val byId = (_advances.value + cloudList).associateBy { it.id }
        val merged = byId.values.toList()
        _advances.value = merged
        saveAdvancesToDisk(merged)
    }

    fun upsertCompany(company: ClientCompany) {
        val updated = (_companies.value.filterNot { it.id == company.id } + company)
            .sortedBy { it.name }
        _companies.value = updated
        saveCompaniesToDisk(updated)
    }

    fun mergeCloudCompanies(cloudList: List<ClientCompany>) {
        if (cloudList.isEmpty()) return
        val byId = (_companies.value + cloudList).associateBy { it.id }
        val merged = byId.values.sortedBy { it.name }
        _companies.value = merged
        saveCompaniesToDisk(merged)
    }

    fun upsertStockItem(item: StockItem) {
        val updated = (_stockItems.value.filterNot { it.id == item.id } + item)
            .sortedBy { it.name }
        _stockItems.value = updated
        saveStockToDisk(updated)
    }

    fun mergeCloudStock(cloudList: List<StockItem>) {
        if (cloudList.isEmpty()) return
        val byId = (_stockItems.value + cloudList).associateBy { it.id }
        val merged = byId.values.sortedBy { it.name }
        _stockItems.value = merged
        saveStockToDisk(merged)
    }

    fun upsertProcurement(proc: DailyProcurement) {
        val updated = (listOf(proc) + _procurements.value.filterNot { it.id == proc.id })
        _procurements.value = updated
        saveProcurementsToDisk(updated)
    }

    fun mergeCloudProcurements(cloudList: List<DailyProcurement>) {
        if (cloudList.isEmpty()) return
        val byId = (_procurements.value + cloudList).associateBy { it.id }
        val merged = byId.values.toList()
        _procurements.value = merged
        saveProcurementsToDisk(merged)
    }

    fun upsertSupplier(supplier: SupplierRecord) {
        val updated = (_suppliers.value.filterNot { it.id == supplier.id } + supplier)
        _suppliers.value = updated
        saveSuppliersToDisk(updated)
    }

    fun mergeCloudSuppliers(cloudList: List<SupplierRecord>) {
        if (cloudList.isEmpty()) return
        val byId = (_suppliers.value + cloudList).associateBy { it.id }
        val merged = byId.values.toList()
        _suppliers.value = merged
        saveSuppliersToDisk(merged)
    }

    fun appendAuditLog(entry: AuditLogEntry) {
        val updated = (listOf(entry) + _auditLogs.value.filterNot { it.id == entry.id }).take(200)
        _auditLogs.value = updated
        saveAuditToDisk(updated)
    }

    fun mergeCloudAudit(cloudList: List<AuditLogEntry>) {
        if (cloudList.isEmpty()) return
        val byId = (_auditLogs.value + cloudList).associateBy { it.id }
        val merged = byId.values.toList().take(200)
        _auditLogs.value = merged
        saveAuditToDisk(merged)
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadAllFromDisk() {
        val p = prefs() ?: return
        _employees.value = parseEmployees(p.getString(KEY_EMPLOYEES, "[]") ?: "[]")
        _attendanceLogs.value = parseAttendance(p.getString(KEY_ATTENDANCE, "[]") ?: "[]")
        _advances.value = parseAdvances(p.getString(KEY_ADVANCES, "[]") ?: "[]")
        _companies.value = parseCompanies(p.getString(KEY_COMPANIES, "[]") ?: "[]")
        _stockItems.value = parseStock(p.getString(KEY_STOCK, "[]") ?: "[]")
        _procurements.value = parseProcurements(p.getString(KEY_PROCUREMENTS, "[]") ?: "[]")
        _suppliers.value = parseSuppliers(p.getString(KEY_SUPPLIERS, "[]") ?: "[]")
        _auditLogs.value = parseAudit(p.getString(KEY_AUDIT, "[]") ?: "[]")
    }

    private fun saveEmployeesToDisk(list: List<EmployeeProfile>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("userId", e.userId)
                put("fullName", e.fullName)
                put("workerEmail", e.workerEmail)
                put("role", e.role)
                put("specialty", e.specialty)
                put("hourlyRate", e.hourlyRate)
                put("overtimeBonusRate", e.overtimeBonusRate)
                put("commissionPercent", e.commissionPercent)
                put("lastMonthPaidTotal", e.lastMonthPaidTotal)
            })
        }
        prefs()?.edit()?.putString(KEY_EMPLOYEES, arr.toString())?.apply()
    }

    private fun parseEmployees(raw: String): List<EmployeeProfile> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EmployeeProfile(
                id = o.optString("id"),
                userId = o.optString("userId"),
                fullName = o.optString("fullName"),
                workerEmail = o.optString("workerEmail"),
                role = o.optString("role", "WORKER"),
                specialty = o.optString("specialty"),
                hourlyRate = o.optDouble("hourlyRate", 40.0),
                overtimeBonusRate = o.optDouble("overtimeBonusRate", 60.0),
                commissionPercent = o.optDouble("commissionPercent", 0.0),
                lastMonthPaidTotal = o.optDouble("lastMonthPaidTotal", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveAttendanceToDisk(list: List<AttendanceLog>) {
        val arr = JSONArray()
        list.forEach { l ->
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("userId", l.userId)
                put("employeeId", l.employeeId)
                put("employeeName", l.employeeName)
                put("date", l.date)
                put("monthKey", l.monthKey)
                put("companyName", l.companyName)
                put("shiftsSummary", l.shiftsSummary)
                put("taskDescription", l.taskDescription)
                put("dailyNote", l.dailyNote)
                put("performanceRating", l.performanceRating)
                put("totalHours", l.totalHours)
                put("regularHours", l.regularHours)
                put("overtimeHours", l.overtimeHours)
                put("dailyPay", l.dailyPay)
                put("recordedBy", l.recordedBy)
            })
        }
        prefs()?.edit()?.putString(KEY_ATTENDANCE, arr.toString())?.apply()
    }

    private fun parseAttendance(raw: String): List<AttendanceLog> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AttendanceLog(
                id = o.optString("id"),
                userId = o.optString("userId"),
                employeeId = o.optString("employeeId"),
                employeeName = o.optString("employeeName"),
                date = o.optString("date"),
                monthKey = o.optString("monthKey"),
                companyName = o.optString("companyName"),
                shiftsSummary = o.optString("shiftsSummary"),
                taskDescription = o.optString("taskDescription"),
                dailyNote = o.optString("dailyNote"),
                performanceRating = o.optLong("performanceRating", 5L),
                totalHours = o.optDouble("totalHours", 9.0),
                regularHours = o.optDouble("regularHours", 9.0),
                overtimeHours = o.optDouble("overtimeHours", 0.0),
                dailyPay = o.optDouble("dailyPay", 360.0),
                recordedBy = o.optString("recordedBy", "Admin")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveAdvancesToDisk(list: List<SalaryAdvance>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("userId", a.userId)
                put("employeeId", a.employeeId)
                put("employeeName", a.employeeName)
                put("amount", a.amount)
                put("date", a.date)
                put("monthKey", a.monthKey)
                put("note", a.note)
                put("recordedBy", a.recordedBy)
            })
        }
        prefs()?.edit()?.putString(KEY_ADVANCES, arr.toString())?.apply()
    }

    private fun parseAdvances(raw: String): List<SalaryAdvance> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SalaryAdvance(
                id = o.optString("id"),
                userId = o.optString("userId"),
                employeeId = o.optString("employeeId"),
                employeeName = o.optString("employeeName"),
                amount = o.optDouble("amount", 0.0),
                date = o.optString("date"),
                monthKey = o.optString("monthKey"),
                note = o.optString("note"),
                recordedBy = o.optString("recordedBy", "Admin")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveCompaniesToDisk(list: List<ClientCompany>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("userId", c.userId)
                put("name", c.name)
                put("city", c.city)
                put("contactName", c.contactName)
                put("phone", c.phone)
                put("contractType", c.contractType)
            })
        }
        prefs()?.edit()?.putString(KEY_COMPANIES, arr.toString())?.apply()
    }

    private fun parseCompanies(raw: String): List<ClientCompany> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ClientCompany(
                id = o.optString("id"),
                userId = o.optString("userId"),
                name = o.optString("name"),
                city = o.optString("city"),
                contactName = o.optString("contactName"),
                phone = o.optString("phone"),
                contractType = o.optString("contractType")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveStockToDisk(list: List<StockItem>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("userId", s.userId)
                put("name", s.name)
                put("category", s.category)
                put("currentQuantity", s.currentQuantity)
                put("reorderPoint", s.reorderPoint)
                put("unit", s.unit)
                put("unitPrice", s.unitPrice)
                put("supplierName", s.supplierName)
                put("dailyTaskNote", s.dailyTaskNote)
            })
        }
        prefs()?.edit()?.putString(KEY_STOCK, arr.toString())?.apply()
    }

    private fun parseStock(raw: String): List<StockItem> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            StockItem(
                id = o.optString("id"),
                userId = o.optString("userId"),
                name = o.optString("name"),
                category = o.optString("category"),
                currentQuantity = o.optLong("currentQuantity", 0L),
                reorderPoint = o.optLong("reorderPoint", 5L),
                unit = o.optString("unit", "Pièce"),
                unitPrice = o.optDouble("unitPrice", 0.0),
                supplierName = o.optString("supplierName"),
                dailyTaskNote = o.optString("dailyTaskNote")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveProcurementsToDisk(list: List<DailyProcurement>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("userId", p.userId)
                put("date", p.date)
                put("companyId", p.companyId)
                put("companyName", p.companyName)
                put("itemName", p.itemName)
                put("quantity", p.quantity)
                put("unitCost", p.unitCost)
                put("totalCost", p.totalCost)
                put("status", p.status)
            })
        }
        prefs()?.edit()?.putString(KEY_PROCUREMENTS, arr.toString())?.apply()
    }

    private fun parseProcurements(raw: String): List<DailyProcurement> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DailyProcurement(
                id = o.optString("id"),
                userId = o.optString("userId"),
                date = o.optString("date"),
                companyId = o.optString("companyId"),
                companyName = o.optString("companyName"),
                itemName = o.optString("itemName"),
                quantity = o.optLong("quantity", 1L),
                unitCost = o.optDouble("unitCost", 0.0),
                totalCost = o.optDouble("totalCost", 0.0),
                status = o.optString("status", "NEEDED")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveSuppliersToDisk(list: List<SupplierRecord>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("userId", s.userId)
                put("name", s.name)
                put("contactInfo", s.contactInfo)
                put("paymentTerms", s.paymentTerms)
                put("invoiceReference", s.invoiceReference)
                put("amountDue", s.amountDue)
                put("amountPaid", s.amountPaid)
                put("dueDate", s.dueDate)
                put("status", s.status)
                put("reminderEnabled", s.reminderEnabled)
            })
        }
        prefs()?.edit()?.putString(KEY_SUPPLIERS, arr.toString())?.apply()
    }

    private fun parseSuppliers(raw: String): List<SupplierRecord> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SupplierRecord(
                id = o.optString("id"),
                userId = o.optString("userId"),
                name = o.optString("name"),
                contactInfo = o.optString("contactInfo"),
                paymentTerms = o.optString("paymentTerms"),
                invoiceReference = o.optString("invoiceReference"),
                amountDue = o.optDouble("amountDue", 0.0),
                amountPaid = o.optDouble("amountPaid", 0.0),
                dueDate = o.optString("dueDate"),
                status = o.optString("status", "PENDING"),
                reminderEnabled = o.optBoolean("reminderEnabled", true)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveAuditToDisk(list: List<AuditLogEntry>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("userId", a.userId)
                put("actorName", a.actorName)
                put("actorRole", a.actorRole)
                put("actionType", a.actionType)
                put("module", a.module)
                put("details", a.details)
            })
        }
        prefs()?.edit()?.putString(KEY_AUDIT, arr.toString())?.apply()
    }

    private fun parseAudit(raw: String): List<AuditLogEntry> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AuditLogEntry(
                id = o.optString("id"),
                userId = o.optString("userId"),
                actorName = o.optString("actorName"),
                actorRole = o.optString("actorRole"),
                actionType = o.optString("actionType"),
                module = o.optString("module"),
                details = o.optString("details")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
