package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.max
import kotlin.math.min

@IgnoreExtraProperties
data class EmployeeProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val workerEmail: String = "",
    val role: String = "WORKER", // ADMIN, MANAGER, WORKER
    val specialty: String = "Technicien Thermique",
    val hourlyRate: Double = 45.0,
    val overtimeBonusRate: Double = 65.0,
    val commissionPercent: Double = 5.0,
    val lastMonthPaidTotal: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ShiftIntervalInput(
    val startTime: String = "08:00",
    val endTime: String = "13:00",
    val hours: Double = 5.0,
    val companyName: String = "",
    val taskNote: String = ""
)

@IgnoreExtraProperties
data class AttendanceLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val date: String = "", // YYYY-MM-DD
    val monthKey: String = "", // YYYY-MM
    val companyName: String = "",
    val shiftsSummary: String = "",
    val taskDescription: String = "",
    val dailyNote: String = "",
    val performanceRating: Long = 5L,
    val totalHours: Double = 0.0,
    val regularHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val dailyPay: Double = 0.0,
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalaryAdvance(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val monthKey: String = "",
    val note: String = "",
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClientCompany(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val city: String = "",
    val contactName: String = "",
    val phone: String = "",
    val contractType: String = "Installation & Maintenance",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StockItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Chaudières",
    val currentQuantity: Long = 0L,
    val reorderPoint: Long = 5L,
    val unit: String = "Pièce",
    val unitPrice: Double = 0.0,
    val supplierName: String = "",
    val dailyTaskNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean
        get() = currentQuantity <= reorderPoint

    val suggestedOrderQuantity: Long
        get() = if (currentQuantity <= reorderPoint) max(reorderPoint * 2 - currentQuantity, 5L) else 0L
}

@IgnoreExtraProperties
data class DailyProcurement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val itemName: String = "",
    val quantity: Long = 1L,
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val status: String = "NEEDED", // NEEDED, ORDERED, RECEIVED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupplierRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactInfo: String = "",
    val paymentTerms: String = "30 Jours FDM",
    val invoiceReference: String = "",
    val amountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val dueDate: String = "",
    val status: String = "PENDING", // PENDING, PARTIAL, PAID, OVERDUE
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val remainingBalance: Double
        get() = max(0.0, amountDue - amountPaid)
}

@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorName: String = "",
    val actorRole: String = "ADMIN",
    val actionType: String = "CREATE", // CREATE, UPDATE, DELETE, EXPORT, ERP_SYNC, ROLE_CHANGE
    val module: String = "Pointage",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class EmployeePayrollSummary(
    val employee: EmployeeProfile,
    val monthKey: String,
    val daysWorked: Int,
    val regularHoursTotal: Double,
    val overtimeHoursTotal: Double,
    val totalHours: Double,
    val baseEarnings: Double,
    val overtimeBonusEarnings: Double,
    val commissionAmount: Double,
    val grossEarnings: Double,
    val totalAdvances: Double,
    val remainingToPay: Double,
    val averageRating: Double
)

object PayrollCalculator {
    const val STANDARD_DAILY_HOURS = 9.0

    fun computeDailyBreakdown(
        totalHours: Double,
        hourlyRate: Double,
        overtimeBonusRate: Double
    ): Triple<Double, Double, Double> {
        val safeTotal = max(0.0, min(24.0, totalHours))
        val regular = min(STANDARD_DAILY_HOURS, safeTotal)
        val overtime = max(0.0, safeTotal - STANDARD_DAILY_HOURS)
        val effectiveOvertimeRate = if (overtimeBonusRate > 0.0) overtimeBonusRate else hourlyRate * 1.5
        val pay = (regular * hourlyRate) + (overtime * effectiveOvertimeRate)
        return Triple(regular, overtime, pay)
    }

    fun buildEmployeeSummary(
        employee: EmployeeProfile,
        monthKey: String,
        logs: List<AttendanceLog>,
        advances: List<SalaryAdvance>
    ): EmployeePayrollSummary {
        val empLogs = logs.filter { it.employeeId == employee.id && it.monthKey == monthKey }
        val empAdvances = advances.filter { it.employeeId == employee.id && it.monthKey == monthKey }

        val daysWorked = empLogs.map { it.date }.distinct().size
        val regularHours = empLogs.sumOf { it.regularHours }
        val overtimeHours = empLogs.sumOf { it.overtimeHours }
        val totalHours = empLogs.sumOf { it.totalHours }
        val baseEarnings = regularHours * employee.hourlyRate
        val overtimeEarnings = overtimeHours * employee.overtimeBonusRate
        val rawWorkPay = empLogs.sumOf { it.dailyPay }
        val commissionAmount = rawWorkPay * (employee.commissionPercent / 100.0)
        val grossEarnings = rawWorkPay + commissionAmount
        val totalAdvances = empAdvances.sumOf { it.amount }
        val remainingToPay = grossEarnings - totalAdvances
        val avgRating = if (empLogs.isNotEmpty()) empLogs.map { it.performanceRating }.average() else 5.0

        return EmployeePayrollSummary(
            employee = employee,
            monthKey = monthKey,
            daysWorked = daysWorked,
            regularHoursTotal = regularHours,
            overtimeHoursTotal = overtimeHours,
            totalHours = totalHours,
            baseEarnings = baseEarnings,
            overtimeBonusEarnings = overtimeEarnings,
            commissionAmount = commissionAmount,
            grossEarnings = grossEarnings,
            totalAdvances = totalAdvances,
            remainingToPay = remainingToPay,
            averageRating = avgRating
        )
    }
}
