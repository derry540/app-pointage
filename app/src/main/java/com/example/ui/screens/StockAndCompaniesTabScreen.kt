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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.ThermoPointViewModel

@Composable
fun StockAndCompaniesTabScreen(
    viewModel: ThermoPointViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Stock & Alertes Auto") },
                modifier = Modifier.testTag("subtab_stock_inventory")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Sociétés & Achats du Jour") },
                modifier = Modifier.testTag("subtab_companies_procurements")
            )
        }

        if (selectedSubTab == 0) {
            StockInventorySubScreen(viewModel = viewModel)
        } else {
            CompaniesAndDailyProcurementSubScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockInventorySubScreen(
    viewModel: ThermoPointViewModel
) {
    val context = LocalContext.current
    val stockItems by viewModel.stockItems.collectAsStateWithLifecycle()

    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Chaudières") }
    var currentQtyText by remember { mutableStateOf("4") }
    var reorderPointText by remember { mutableStateOf("5") }
    var unitText by remember { mutableStateOf("Pièce") }
    var unitPriceText by remember { mutableStateOf("3500") }
    var supplierName by remember { mutableStateOf("Maghreb Thermo-Distribution") }
    var dailyTaskNote by remember { mutableStateOf("Contrôle réception et affectation chantier") }

    val categories = listOf(
        "Chaudières",
        "Brûleurs",
        "Pompes à Chaleur",
        "Tuyauterie & Vannes",
        "Régulation & Sondes"
    )

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
                        text = "Ajouter un Équipement au Stock & Définir le Seuil d'Alerte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Désignation de l'article thermique") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_item_name_input"),
                        singleLine = true
                    )

                    Text("Catégorie :", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentQtyText,
                            onValueChange = { currentQtyText = it },
                            label = { Text("Stock Actuel") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = reorderPointText,
                            onValueChange = { reorderPointText = it },
                            label = { Text("Seuil Alerte Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitText,
                            onValueChange = { unitText = it },
                            label = { Text("Unité") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            label = { Text("Prix Unitaire (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = { supplierName = it },
                            label = { Text("Fournisseur") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = dailyTaskNote,
                        onValueChange = { dailyTaskNote = it },
                        label = { Text("Tâche quotidienne liée au stock / mouvement") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.addStockItem(
                                context = context,
                                name = itemName,
                                category = selectedCategory,
                                currentQuantity = currentQtyText.toLongOrNull() ?: 0L,
                                reorderPoint = reorderPointText.toLongOrNull() ?: 5L,
                                unit = unitText,
                                unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0,
                                supplierName = supplierName,
                                dailyTaskNote = dailyTaskNote
                            )
                            itemName = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_stock_item_button")
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer dans l'Inventaire Automatisé")
                    }
                }
            }
        }

        item {
            Text(
                text = "Inventaire Thermique & Suivi Temps Réel (${stockItems.size} articles)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(stockItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isLowStock) {
                        CrimsonAlert.copy(alpha = 0.12f)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.category} • Fournisseur: ${item.supplierName} • ${"%.0f".format(item.unitPrice)} DH/${item.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.isLowStock) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonAlert.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerte Stock",
                                    tint = CrimsonAlert
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Commander +${item.suggestedOrderQuantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonAlert,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (item.dailyTaskNote.isNotBlank()) {
                        Text(
                            text = "Tâche stock du jour : ${item.dailyTaskNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CryoSkyBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "En stock: ${item.currentQuantity} ${item.unit} (Seuil min: ${item.reorderPoint})",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (item.isLowStock) CrimsonAlert else EmeraldSuccess
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.adjustStockQuantity(
                                        context = context,
                                        item = item,
                                        newQuantity = (item.currentQuantity - 1L).coerceAtLeast(0L),
                                        taskNote = "Sortie 1 ${item.unit} pour chantier"
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer stock")
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.adjustStockQuantity(
                                        context = context,
                                        item = item,
                                        newQuantity = item.currentQuantity + 1L,
                                        taskNote = "Entrée +1 ${item.unit} en stock"
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter stock")
                            }
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
private fun CompaniesAndDailyProcurementSubScreen(
    viewModel: ThermoPointViewModel
) {
    val companies by viewModel.companies.collectAsStateWithLifecycle()
    val procurements by viewModel.dailyProcurements.collectAsStateWithLifecycle()

    // New company form
    var companyName by remember { mutableStateOf("") }
    var companyCity by remember { mutableStateOf("Casablanca") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contractType by remember { mutableStateOf("Installation & Maintenance") }

    // Daily procurement form (Sl3a li khasaha njibha wbch7al)
    var selectedCompanyId by remember { mutableStateOf("") }
    val selectedCompany = companies.find { it.id == selectedCompanyId } ?: companies.firstOrNull()
    var procDate by remember { mutableStateOf(ThermoPointViewModel.todayDateString()) }
    var procItemName by remember { mutableStateOf("") }
    var procQtyText by remember { mutableStateOf("2") }
    var procUnitCostText by remember { mutableStateOf("1200") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Daily Procurement Form (Besoins du jour par société)
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
                        text = "Marchandises à Apporter Aujourd'hui par Société (Sl3a li khass njibha)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (companies.isNotEmpty()) {
                        Text(
                            text = "Choisir la société / chantier :",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            companies.forEach { comp ->
                                FilterChip(
                                    selected = selectedCompany?.id == comp.id,
                                    onClick = { selectedCompanyId = comp.id },
                                    label = { Text(comp.name) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = procItemName,
                        onValueChange = { procItemName = it },
                        label = { Text("Marchandise / Pièce thermique à acheter aujourd'hui") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("procurement_item_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = procQtyText,
                            onValueChange = { procQtyText = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = procUnitCostText,
                            onValueChange = { procUnitCostText = it },
                            label = { Text("Prix Unitaire (DH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = procDate,
                            onValueChange = { procDate = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    val totalPreview = (procQtyText.toLongOrNull() ?: 1L) * (procUnitCostText.toDoubleOrNull() ?: 0.0)
                    Text(
                        text = "Budget estimé pour cet achat : ${"%.2f".format(totalPreview)} DH",
                        style = MaterialTheme.typography.labelLarge,
                        color = ThermalCopper
                    )

                    Button(
                        onClick = {
                            viewModel.addDailyProcurement(
                                date = procDate,
                                companyId = selectedCompany?.id ?: "general",
                                companyName = selectedCompany?.name ?: "Chantier Principal",
                                itemName = procItemName,
                                quantity = procQtyText.toLongOrNull() ?: 1L,
                                unitCost = procUnitCostText.toDoubleOrNull() ?: 0.0
                            )
                            procItemName = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_procurement_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter à la Liste des Achats du Jour (Dashboard)")
                    }
                }
            }
        }

        // List of Daily Procurements
        if (procurements.isNotEmpty()) {
            item {
                Text(
                    text = "Suivi des Achats Journaliers par Société (${procurements.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(procurements, key = { it.id }) { proc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.cycleProcurementStatus(proc) },
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
                                text = "${proc.quantity}x ${proc.itemName} (${"%.0f".format(proc.totalCost)} DH)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Société : ${proc.companyName} • Date: ${proc.date} • (${"%.0f".format(proc.unitCost)} DH/u)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(text = proc.status)
                    }
                }
            }
        }

        // Add Client Company to Database Card
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
                        text = "Base de Données des Sociétés Clientes (Admin)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enregistrez vos sociétés clientes une seule fois pour les sélectionner directement lors du pointage et des achats du jour.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nom de la Société / Chantier") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_name_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = companyCity,
                            onValueChange = { companyCity = it },
                            label = { Text("Ville / Zone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contractType,
                            onValueChange = { contractType = it },
                            label = { Text("Spécialité / Contrat") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Responsable") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Téléphone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addCompany(
                                name = companyName,
                                city = companyCity,
                                contactName = contactName,
                                phone = contactPhone,
                                contractType = contractType
                            )
                            companyName = ""
                            contactName = ""
                            contactPhone = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_company_button")
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer la Société dans la Base de Données")
                    }
                }
            }
        }

        items(companies, key = { it.id }) { comp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${comp.name} (${comp.city})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Contrat : ${comp.contractType} • Contact: ${comp.contactName} (${comp.phone})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
