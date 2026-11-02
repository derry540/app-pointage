package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CryoSkyBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ThermalCopper
import com.example.viewmodel.GeminiAiViewModel
import com.example.viewmodel.MediaAnalysisUiState
import com.example.viewmodel.StudioImageUiState
import com.example.viewmodel.StudioResolutionOption
import com.example.viewmodel.ThermoPointViewModel

@Composable
fun AiAssistantTabScreen(
    thermoViewModel: ThermoPointViewModel,
    aiViewModel: GeminiAiViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Chat & High Thinking") },
                modifier = Modifier.testTag("subtab_ai_chat")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Scan Photo & Vidéo") },
                modifier = Modifier.testTag("subtab_ai_vision")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Studio Image 1K-4K") },
                modifier = Modifier.testTag("subtab_ai_image_gen")
            )
        }

        when (selectedSubTab) {
            0 -> GeminiChatAndThinkingSubScreen(thermoViewModel = thermoViewModel, aiViewModel = aiViewModel)
            1 -> GeminiVisionAndVideoSubScreen(aiViewModel = aiViewModel)
            else -> GeminiStudioImageSubScreen(aiViewModel = aiViewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiChatAndThinkingSubScreen(
    thermoViewModel: ThermoPointViewModel,
    aiViewModel: GeminiAiViewModel
) {
    val messages by aiViewModel.chatMessages.collectAsStateWithLifecycle()
    val isHighThinking by aiViewModel.isHighThinkingEnabled.collectAsStateWithLifecycle()
    val isFastLite by aiViewModel.isFastLiteMode.collectAsStateWithLifecycle()
    val isLoading by aiViewModel.isChatLoading.collectAsStateWithLifecycle()
    val summaries by thermoViewModel.payrollSummaries.collectAsStateWithLifecycle()
    val stockItems by thermoViewModel.stockItems.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }

    val contextSummary = remember(summaries, stockItems) {
        buildString {
            append("Effectif: ${summaries.size} employés. ")
            summaries.forEach { s ->
                append("${s.employee.fullName}: ${s.totalHours}h (dont ${s.overtimeHoursTotal}h >9h), Brut=${s.grossEarnings} DH, Avances=${s.totalAdvances} DH, Reste=${s.remainingToPay} DH. ")
            }
            val lowStock = stockItems.filter { it.isLowStock }
            append("Articles en alerte stock: ${lowStock.joinToString { "${it.name} (${it.currentQuantity}/${it.reorderPoint})" }}.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Conseiller IA Gemini — Équipements Thermiques & Oxygène ERP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !isHighThinking && !isFastLite,
                        onClick = {
                            aiViewModel.setHighThinkingEnabled(false)
                            aiViewModel.setFastLiteMode(false)
                        },
                        label = { Text("Standard (gemini-3.8-flash)") }
                    )
                    FilterChip(
                        selected = isHighThinking,
                        onClick = { aiViewModel.setHighThinkingEnabled(!isHighThinking) },
                        leadingIcon = {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text("High Thinking (gemini-4-argon)") },
                        modifier = Modifier.testTag("toggle_high_thinking_chip")
                    )
                    FilterChip(
                        selected = isFastLite,
                        onClick = { aiViewModel.setFastLiteMode(!isFastLite) },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text("Ultra-Rapide (flash-lite)") }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = msg.modelBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isUser) ThermalCopper else CryoSkyBlue
                            )
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHighThinking) {
                                "Réflexion approfondie en cours (gemini-4-argon ThinkingLevel.HIGH)..."
                            } else {
                                "Génération de la réponse..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                label = { Text("Question paie 9h+, stock, chaudière ou Oxygène ERP...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                singleLine = true
            )
            Button(
                onClick = {
                    aiViewModel.sendChatMessage(inputPrompt, contextSummary)
                    inputPrompt = ""
                },
                enabled = !isLoading && inputPrompt.isNotBlank(),
                modifier = Modifier.testTag("ai_chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
            }
        }
    }
}

@Composable
private fun GeminiVisionAndVideoSubScreen(
    aiViewModel: GeminiAiViewModel
) {
    val context = LocalContext.current
    val analysisState by aiViewModel.mediaAnalysisState.collectAsStateWithLifecycle()
    var customInstruction by remember {
        mutableStateOf("Analyse ce document ou média de chantier (fiche de pointage, bon de livraison fournisseur ou installation thermique) et extrais les informations clés.")
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            aiViewModel.analyzeUploadedImage(context, uri, customInstruction)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            aiViewModel.analyzeUploadedVideo(context, uri, customInstruction)
        }
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
                        text = "Analyse Photo & Vidéo par Gemini 4 Argon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Importez une photo (fiche de pointage papier, facture fournisseur, plaque signalétique de chaudière) ou une vidéo d'inspection de chantier thermique pour une analyse complète via le modèle gemini-4-argon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customInstruction,
                        onValueChange = { customInstruction = it },
                        label = { Text("Instruction d'analyse personnalisée") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_image_analysis_button")
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyser Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_video_analysis_button")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyser Vidéo")
                        }
                    }
                }
            }
        }

        item {
            when (val state = analysisState) {
                is MediaAnalysisUiState.Idle -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sélectionnez une photo ou une vidéo pour lancer l'analyse multimodale (gemini-4-argon).",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is MediaAnalysisUiState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyse en cours avec gemini-4-argon (30-60s max)...")
                        }
                    }
                }
                is MediaAnalysisUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.mediaLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                            Text(
                                text = state.resultText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                is MediaAnalysisUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "Erreur: ${state.message}",
                            color = CrimsonAlert,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiStudioImageSubScreen(
    aiViewModel: GeminiAiViewModel
) {
    val selectedResolution by aiViewModel.selectedResolution.collectAsStateWithLifecycle()
    val studioState by aiViewModel.studioImageState.collectAsStateWithLifecycle()
    var promptText by remember {
        mutableStateOf("Schéma technique 3D d'une chaufferie industrielle haute pression avec brûleur modulant, collecteur cuivre et vannes motorisées")
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
                        text = "Studio de Génération d'Images Haute Qualité (gemini-3-pro-image)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Générez des visuels d'équipements thermiques, schémas d'installation ou signalétiques de sécurité chantier en résolution 1K, 2K ou 4K.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Choisir la taille de l'image (1K, 2K, 4K) :",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioResolutionOption.entries.forEach { option ->
                            FilterChip(
                                selected = selectedResolution == option,
                                onClick = { aiViewModel.setSelectedResolution(option) },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("resolution_chip_${option.name}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Description de l'image à générer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_image_prompt_input")
                    )

                    Button(
                        onClick = { aiViewModel.generateStudioThermalImage(promptText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_studio_image_button")
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Générer l'Image (${selectedResolution.label})")
                    }
                }
            }
        }

        item {
            when (val state = studioState) {
                is StudioImageUiState.Idle -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ThermalCopper)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Prêt à générer des visuels en 1K, 2K ou 4K.")
                        }
                    }
                }
                is StudioImageUiState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Synthèse de l'image (${selectedResolution.label}) en cours (30 à 60s)...")
                        }
                    }
                }
                is StudioImageUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Image Générée — Résolution ${state.resolutionLabel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                            Image(
                                bitmap = state.bitmap.asImageBitmap(),
                                contentDescription = "Visuel généré",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
                is StudioImageUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "Erreur: ${state.message}",
                            color = CrimsonAlert,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
