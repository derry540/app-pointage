package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceLog
import com.example.data.model.AuditLogEntry
import com.example.data.model.DailyProcurement
import com.example.data.model.EmployeePayrollSummary
import com.example.data.model.SalaryAdvance
import com.example.data.model.StockItem
import com.example.data.model.SupplierRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExportAndErpManager {

    private const val CHANNEL_ID = "thermopoint_alerts_channel"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertes ThermoPoint Pro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes de pointage (>9h), réapprovisionnement stock et échéances fournisseurs"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendSystemAlertNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Generates a comprehensive multi-page PDF report for Équipements Thermiques:
     * 1. Payroll & Advances Summary (9h standard + >9h overtime bonus)
     * 2. Detailed Daily Attendance Logs & Multi-Company Shifts
     * 3. Stock & Reorder Alerts + Daily Procurement Needs
     * 4. Supplier Schedules & Access/Audit History
     */
    suspend fun generateFullPdfReport(
        context: Context,
        monthKey: String,
        summaries: List<EmployeePayrollSummary>,
        attendanceLogs: List<AttendanceLog>,
        advances: List<SalaryAdvance>,
        stockItems: List<StockItem>,
        procurements: List<DailyProcurement>,
        suppliers: List<SupplierRecord>,
        auditLogs: List<AuditLogEntry>
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "Rapport_Equipements_Thermiques_$monthKey.pdf")

        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(234, 88, 12)
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        // PAGE 1: Payroll, Advances & Attendance Logs
        val page1Info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(page1Info)
        val c1: Canvas = page1.canvas
        var y = 40f

        c1.drawText("ÉQUIPEMENTS THERMIQUES — RAPPORT MENSUEL ($monthKey)", 36f, y, titlePaint)
        y += 18f
        c1.drawText("Système Automatisé de Pointage (Norme 9h/j + Bonus >9h), Avances, Stock & Fournisseurs", 36f, y, bodyPaint)
        y += 12f
        c1.drawLine(36f, y, 559f, y, linePaint)
        y += 20f

        c1.drawText("1. BILAN PAIE HORAIRE, AVANCES & RESTE À PAYER ($monthKey)", 36f, y, headerPaint)
        y += 16f
        c1.drawText("Employé | Taux/h | Heures (Norm / >9h) | Brut + % | Avances | Reste à Payer | Mois Préc.", 36f, y, boldBodyPaint)
        y += 12f

        summaries.take(15).forEach { s ->
            val line = "${s.employee.fullName.take(16)} | ${"%.0f".format(s.employee.hourlyRate)} DH | " +
                "${"%.1f".format(s.regularHoursTotal)}h / +${"%.1f".format(s.overtimeHoursTotal)}h | " +
                "${"%.0f".format(s.grossEarnings)} DH | -${"%.0f".format(s.totalAdvances)} DH | " +
                "NET: ${"%.0f".format(s.remainingToPay)} DH | Préc: ${"%.0f".format(s.employee.lastMonthPaidTotal)} DH"
            c1.drawText(line, 36f, y, bodyPaint)
            y += 13f
        }

        y += 12f
        c1.drawLine(36f, y, 559f, y, linePaint)
        y += 18f
        c1.drawText("2. JOURNAL DE POINTAGE QUOTIDIEN & MULTI-SOCIÉTÉS", 36f, y, headerPaint)
        y += 15f
        attendanceLogs.filter { it.monthKey == monthKey }.take(18).forEach { log ->
            val line = "${log.date} | ${log.employeeName.take(14)} | ${log.companyName.take(20)} | " +
                "${"%.1f".format(log.totalHours)}h (Sup: ${"%.1f".format(log.overtimeHours)}h) | " +
                "${"%.0f".format(log.dailyPay)} DH | ★${log.performanceRating}/5"
            c1.drawText(line, 36f, y, bodyPaint)
            y += 12f
            if (log.shiftsSummary.isNotBlank() || log.dailyNote.isNotBlank()) {
                c1.drawText("   Créneaux: ${log.shiftsSummary.take(65)} | Note: ${log.dailyNote.take(35)}", 36f, y, bodyPaint)
                y += 12f
            }
            if (y > 790f) return@forEach
        }
        pdfDocument.finishPage(page1)

        // PAGE 2: Stock Alerts, Daily Procurements, Suppliers & Audit Logs
        val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val c2: Canvas = page2.canvas
        y = 40f

        c2.drawText("3. ÉTAT DES STOCKS THERMIQUES & ALERTES DE RÉAPPROVISIONNEMENT", 36f, y, headerPaint)
        y += 16f
        stockItems.take(12).forEach { item ->
            val statusTag = if (item.isLowStock) "[ALERTE STOCK BAS - Commander ${item.suggestedOrderQuantity}]" else "[OK]"
            c2.drawText(
                "${item.name.take(22)} (${item.category}) : ${item.currentQuantity} ${item.unit} (Seuil: ${item.reorderPoint}) $statusTag",
                36f,
                y,
                bodyPaint
            )
            y += 13f
        }

        y += 10f
        c2.drawLine(36f, y, 559f, y, linePaint)
        y += 18f
        c2.drawText("4. MARCHANDISES À APPROVISIONNER PAR SOCIÉTÉ & ÉCHÉANCIER FOURNISSEURS", 36f, y, headerPaint)
        y += 15f
        procurements.take(8).forEach { p ->
            c2.drawText(
                "Achat Jour ${p.date} | ${p.companyName.take(18)} | ${p.quantity}x ${p.itemName.take(20)} = ${"%.0f".format(p.totalCost)} DH [${p.status}]",
                36f,
                y,
                bodyPaint
            )
            y += 12f
        }
        y += 8f
        suppliers.take(8).forEach { sup ->
            c2.drawText(
                "Fournisseur: ${sup.name.take(18)} | Facture ${sup.invoiceReference} | Échéance: ${sup.dueDate} | Reste: ${"%.0f".format(sup.remainingBalance)} DH [${sup.status}]",
                36f,
                y,
                bodyPaint
            )
            y += 12f
        }

        y += 10f
        c2.drawLine(36f, y, 559f, y, linePaint)
        y += 18f
        c2.drawText("5. HISTORIQUE DES ACCÈS ET MODIFICATIONS (AUDIT LOG)", 36f, y, headerPaint)
        y += 15f
        auditLogs.take(12).forEach { audit ->
            c2.drawText(
                "[${audit.actionType}] ${audit.actorName} (${audit.module}) : ${audit.details.take(75)}",
                36f,
                y,
                bodyPaint
            )
            y += 12f
        }

        pdfDocument.finishPage(page2)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        pdfFile
    }

    /**
     * Generates an Excel-compatible CSV file containing Payroll, Advances, Attendance Logs, Suppliers & Daily Procurements.
     */
    suspend fun generateExcelCsv(
        context: Context,
        monthKey: String,
        summaries: List<EmployeePayrollSummary>,
        attendanceLogs: List<AttendanceLog>,
        advances: List<SalaryAdvance>,
        suppliers: List<SupplierRecord>,
        procurements: List<DailyProcurement>
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val csvFile = File(reportsDir, "Export_Equipements_Thermiques_Excel_$monthKey.csv")

        val sb = StringBuilder()
        // UTF-8 BOM for direct opening in Microsoft Excel with French accents
        sb.append("\uFEFF")
        sb.append("=== SECTION 1: RECAPITULATIF PAIE, AVANCES & MOIS PRECEDENT ($monthKey) ===\n")
        sb.append("Employe;Specialite;Role;TauxHoraire_DH;TauxSup9h_DH;HeuresNormales;HeuresSup_Plus9h;TotalHeures;Commission_Pct;TotalBrut_DH;AvancesDonnees_DH;ResteAPayer_DH;PayeMoisPrecedent_DH;NoteMoyenne\n")
        summaries.forEach { s ->
            sb.append("${s.employee.fullName};${s.employee.specialty};${s.employee.role};${s.employee.hourlyRate};${s.employee.overtimeBonusRate};${s.regularHoursTotal};${s.overtimeHoursTotal};${s.totalHours};${s.employee.commissionPercent};${"%.2f".format(s.grossEarnings)};${"%.2f".format(s.totalAdvances)};${"%.2f".format(s.remainingToPay)};${"%.2f".format(s.employee.lastMonthPaidTotal)};${"%.1f".format(s.averageRating)}\n")
        }

        sb.append("\n=== SECTION 2: POINTAGE JOURNALIER PAR SOCIETE & INTERVALLES ===\n")
        sb.append("Date;Employe;SocieteCliente;IntervallesHoraires;TacheEffectuee;NoteQuotidienne;PerformanceSur5;HeuresTotales;HeuresNormalesMax9h;HeuresSup;PaieJour_DH\n")
        attendanceLogs.filter { it.monthKey == monthKey }.forEach { log ->
            sb.append("${log.date};${log.employeeName};${log.companyName};\"${log.shiftsSummary}\";\"${log.taskDescription}\";\"${log.dailyNote}\";${log.performanceRating};${log.totalHours};${log.regularHours};${log.overtimeHours};${"%.2f".format(log.dailyPay)}\n")
        }

        sb.append("\n=== SECTION 3: AVANCES SUR SALAIRE VERSEES ===\n")
        sb.append("Date;Employe;Montant_DH;Motif;EnregistrePar\n")
        advances.filter { it.monthKey == monthKey }.forEach { adv ->
            sb.append("${adv.date};${adv.employeeName};${"%.2f".format(adv.amount)};\"${adv.note}\";${adv.recordedBy}\n")
        }

        sb.append("\n=== SECTION 4: ACHATS DU JOUR PAR CHANTIER ===\n")
        sb.append("Date;Societe;Article;Quantite;PrixUnitaire_DH;Total_DH;Statut\n")
        procurements.forEach { p ->
            sb.append("${p.date};${p.companyName};${p.itemName};${p.quantity};${p.unitCost};${p.totalCost};${p.status}\n")
        }

        sb.append("\n=== SECTION 5: ECHEANCIER FOURNISSEURS ===\n")
        sb.append("Fournisseur;RefFacture;Conditions;TotalFacture_DH;DejaPaye_DH;ResteAPayer_DH;DateEcheance;Statut\n")
        suppliers.forEach { sup ->
            sb.append("${sup.name};${sup.invoiceReference};${sup.paymentTerms};${"%.2f".format(sup.amountDue)};${"%.2f".format(sup.amountPaid)};${"%.2f".format(sup.remainingBalance)};${sup.dueDate};${sup.status}\n")
        }

        csvFile.writeText(sb.toString(), Charsets.UTF_8)
        csvFile
    }

    /**
     * Shares a generated PDF or CSV report file via Android Share Sheet or Email client.
     */
    fun shareReportFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String,
        bodyText: String,
        recipientEmails: Array<String> = emptyArray()
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            if (recipientEmails.isNotEmpty()) {
                putExtra(Intent.EXTRA_EMAIL, recipientEmails)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Exporter / Envoyer le rapport").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Shares an employee's live attendance, advances, and net balance update via WhatsApp (compatible with *6 Pass in Morocco)
     * or SMS/Share sheet.
     */
    fun shareEmployeeBalanceViaWhatsApp(
        context: Context,
        summary: EmployeePayrollSummary,
        monthKey: String
    ) {
        val message = buildString {
            appendLine("*ÉQUIPEMENTS THERMIQUES — Fiche Pointage & Solde ($monthKey)*")
            appendLine("👤 *Salarié :* ${summary.employee.fullName} (${summary.employee.specialty})")
            appendLine("⏱️ *Total Heures :* ${"%.1f".format(summary.totalHours)}h (${summary.daysWorked} j)")
            appendLine("   • Normales (≤9h/j à ${"%.0f".format(summary.employee.hourlyRate)} DH/h) : ${"%.1f".format(summary.regularHoursTotal)}h = ${"%.2f".format(summary.baseEarnings)} DH")
            appendLine("   • Heures Sup (>9h/j à ${"%.0f".format(summary.employee.overtimeBonusRate)} DH/h) : +${"%.1f".format(summary.overtimeHoursTotal)}h = ${"%.2f".format(summary.overtimeBonusEarnings)} DH")
            if (summary.employee.commissionPercent > 0) {
                appendLine("📈 *Bonus Commission (${"%.1f".format(summary.employee.commissionPercent)}%) :* +${"%.2f".format(summary.commissionAmount)} DH")
            }
            appendLine("💰 *Total Gagné ($monthKey) :* ${"%.2f".format(summary.grossEarnings)} DH")
            appendLine("💸 *Avances Reçues (Avances) :* -${"%.2f".format(summary.totalAdvances)} DH")
            appendLine("✅ *RESTE NET À PAYER (Ba9i l-khlas) : ${"%.2f".format(summary.remainingToPay)} DH*")
            appendLine("📊 *Payé le mois dernier :* ${"%.2f".format(summary.employee.lastMonthPaidTotal)} DH")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer Mise à Jour Salarié (WhatsApp *6 / SMS)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Generates a standalone iOS / iPhone Progressive Web App (HTML5 with apple-mobile-web-app-capable)
     * containing the live synchronized company database (employees, 9h+ attendance, advances, net pay, and stock alerts)
     * so iPhone users can open it in Safari and tap "Partager -> Sur l'écran d'accueil" (Add to Home Screen).
     */
    suspend fun generateIphoneWebPortalApp(
        context: Context,
        monthKey: String,
        summaries: List<EmployeePayrollSummary>,
        stockItems: List<StockItem>,
        suppliers: List<SupplierRecord>
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val htmlFile = File(reportsDir, "ThermoPoint_Pro_iPhone_iOS_$monthKey.html")

        val employeesRowsHtml = summaries.joinToString("\n") { s ->
            """
            <div class="card">
              <div class="row-between">
                <div>
                  <div class="emp-name">${s.employee.fullName}</div>
                  <div class="sub">${s.employee.specialty} • ${"%.0f".format(s.employee.hourlyRate)} DH/h (Sup &gt;9h: ${"%.0f".format(s.employee.overtimeBonusRate)} DH/h)</div>
                </div>
                <div class="badge">${"%.1f".format(s.totalHours)} h</div>
              </div>
              <hr/>
              <div class="row-between"><span>Heures Normales (≤9h/j) :</span><b>${"%.1f".format(s.regularHoursTotal)}h (${"%.0f".format(s.baseEarnings)} DH)</b></div>
              <div class="row-between"><span>Heures Sup (&gt;9h/j) :</span><b style="color:#F97316">+${"%.1f".format(s.overtimeHoursTotal)}h (${"%.0f".format(s.overtimeBonusEarnings)} DH)</b></div>
              <div class="row-between"><span>Total Brut (+${s.employee.commissionPercent}%) :</span><b>${"%.2f".format(s.grossEarnings)} DH</b></div>
              <div class="row-between"><span>Avances Reçues :</span><b style="color:#F59E0B">-${"%.2f".format(s.totalAdvances)} DH</b></div>
              <div class="net-box">
                <span>RESTE NET À PAYER :</span>
                <span class="net-val">${"%.2f".format(s.remainingToPay)} DH</span>
              </div>
              <div class="row-between" style="margin-top:6px;font-size:12px;color:#94A3B8">
                <span>Payé fin du mois dernier :</span><span>${"%.2f".format(s.employee.lastMonthPaidTotal)} DH</span>
              </div>
            </div>
            """.trimIndent()
        }

        val stockRowsHtml = stockItems.joinToString("\n") { item ->
            val badgeColor = if (item.isLowStock) "#EF4444" else "#10B981"
            val statusTxt = if (item.isLowStock) "ALERTE STOCK BAS (Commander +${item.suggestedOrderQuantity})" else "OK"
            """
            <div class="card">
              <div class="row-between">
                <b>${item.name} (${item.category})</b>
                <span style="color:$badgeColor;font-weight:bold;font-size:12px">$statusTxt</span>
              </div>
              <div class="sub">En stock : ${item.currentQuantity} ${item.unit} • Seuil min : ${item.reorderPoint} • ${"%.0f".format(item.unitPrice)} DH/u</div>
            </div>
            """.trimIndent()
        }

        val htmlContent = """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <meta name="apple-mobile-web-app-capable" content="yes" />
          <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
          <meta name="apple-mobile-web-app-title" content="ThermoPoint Pro" />
          <title>ThermoPoint Pro — iPhone &amp; Android ($monthKey)</title>
          <style>
            body { background:#0F172A; color:#F8FAFC; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin:0; padding:16px; }
            .header { background:linear-gradient(135deg,#1E293B,#090D16); border:1px solid #F97316; border-radius:18px; padding:16px; margin-bottom:16px; }
            .title { font-size:20px; font-weight:800; color:#F97316; }
            .sub { font-size:13px; color:#94A3B8; margin-top:4px; }
            .tabs { display:flex; gap:8px; margin-bottom:14px; }
            .tab-btn { flex:1; padding:10px; border-radius:10px; border:none; background:#1E293B; color:#CBD5E1; font-weight:700; cursor:pointer; }
            .tab-btn.active { background:#F97316; color:#FFFFFF; }
            .card { background:#1E293B; border-radius:14px; padding:14px; margin-bottom:12px; border:1px solid #334155; }
            .row-between { display:flex; justify-content:space-between; align-items:center; margin:4px 0; font-size:14px; }
            .emp-name { font-size:17px; font-weight:700; color:#FFFFFF; }
            .badge { background:#0284C7; color:#FFF; padding:4px 10px; border-radius:8px; font-weight:700; font-size:13px; }
            .net-box { background:rgba(16,185,129,0.16); border:1px solid #10B981; border-radius:10px; padding:10px; margin-top:8px; display:flex; justify-content:space-between; align-items:center; font-weight:800; }
            .net-val { color:#10B981; font-size:17px; }
            hr { border:0; border-top:1px solid #334155; margin:8px 0; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="title">🔥 ÉQUIPEMENTS THERMIQUES</div>
            <div class="sub">ThermoPoint Pro (Version iPhone iOS &amp; Android) • Période $monthKey</div>
            <div class="sub" style="color:#38BDF8;margin-top:6px">💡 Sur iPhone (Safari) : Touchez Partager ⬆️ puis "Sur l'écran d'accueil" pour l'installer comme application iOS.</div>
          </div>
          <div class="tabs">
            <button class="tab-btn active" onclick="showSection('paie', this)">Paie &amp; Salariés (${summaries.size})</button>
            <button class="tab-btn" onclick="showSection('stock', this)">Stock &amp; Alertes (${stockItems.size})</button>
          </div>
          <div id="sec-paie">$employeesRowsHtml</div>
          <div id="sec-stock" style="display:none">$stockRowsHtml</div>
          <script>
            function showSection(name, btn) {
              document.getElementById('sec-paie').style.display = (name === 'paie') ? 'block' : 'none';
              document.getElementById('sec-stock').style.display = (name === 'stock') ? 'block' : 'none';
              document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
              btn.classList.add('active');
            }
          </script>
        </body>
        </html>
        """.trimIndent()

        htmlFile.writeText(htmlContent, Charsets.UTF_8)
        htmlFile
    }
}
