package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class ChatMessageItem(
    val id: String = System.currentTimeMillis().toString(),
    val isUser: Boolean,
    val text: String,
    val modelBadge: String
)

sealed interface MediaAnalysisUiState {
    data object Idle : MediaAnalysisUiState
    data object Loading : MediaAnalysisUiState
    data class Success(val resultText: String, val mediaLabel: String) : MediaAnalysisUiState
    data class Error(val message: String) : MediaAnalysisUiState
}

sealed interface StudioImageUiState {
    data object Idle : StudioImageUiState
    data object Loading : StudioImageUiState
    data class Success(val bitmap: Bitmap, val resolutionLabel: String) : StudioImageUiState
    data class Error(val message: String) : StudioImageUiState
}

enum class StudioResolutionOption(val label: String, val imageSize: ImageSize) {
    RES_1K("1K (1024px)", ImageSize.SIZE_1K),
    RES_2K("2K (2048px)", ImageSize.SIZE_2K),
    RES_4K("4K (4096px)", ImageSize.SIZE_4K)
}

class GeminiAiViewModel : ViewModel() {

    private val systemPrompt = content("system") {
        text(
            "Tu es l'assistant IA expert de la société 'Équipements Thermiques' au Maroc. " +
                "Tu maîtrises la gestion du pointage horaire (norme 9h/jour + heures supplémentaires et bonus), " +
                "la gestion des avances sur salaire, la maintenance thermique (chaudières, brûleurs, PAC, vapeur), " +
                "l'optimisation des stocks avec seuils d'alerte, et la comptabilité/paie sur Memesoft Oxygène ERP. " +
                "Réponds de façon claire, structurée et professionnelle en français (ou darija si demandé)."
        )
    }

    // 1. General-purpose model (gemini-3.8-flash)
    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = systemPrompt
        )
    }

    // 2. Fast lightweight model (gemini-3.5-flash-lite)
    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = systemPrompt
        )
    }

    // 3. Complex reasoning & High-Thinking model (gemini-4-argon with ThinkingLevel.HIGH, no maxOutputTokens)
    private val argonHighThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = systemPrompt
        )
    }

    // 4. Multimodal Image & Video understanding model (gemini-4-argon)
    private val argonVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            systemInstruction = systemPrompt
        )
    }

    private var activeFlashChat: Chat? = null
    private var activeThinkingChat: Chat? = null

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessageItem(
                id = "welcome",
                isUser = false,
                text = "Marhba ! Je suis votre conseiller IA Équipements Thermiques & Memesoft Oxygène ERP. Posez-moi vos questions sur le calcul des heures (>9h), les avances, les écritures comptables Oxygène ou le diagnostic thermique.",
                modelBadge = "gemini-3.8-flash"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessageItem>> = _chatMessages.asStateFlow()

    private val _isHighThinkingEnabled = MutableStateFlow(false)
    val isHighThinkingEnabled: StateFlow<Boolean> = _isHighThinkingEnabled.asStateFlow()

    private val _isFastLiteMode = MutableStateFlow(false)
    val isFastLiteMode: StateFlow<Boolean> = _isFastLiteMode.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _mediaAnalysisState = MutableStateFlow<MediaAnalysisUiState>(MediaAnalysisUiState.Idle)
    val mediaAnalysisState: StateFlow<MediaAnalysisUiState> = _mediaAnalysisState.asStateFlow()

    private val _selectedResolution = MutableStateFlow(StudioResolutionOption.RES_1K)
    val selectedResolution: StateFlow<StudioResolutionOption> = _selectedResolution.asStateFlow()

    private val _studioImageState = MutableStateFlow<StudioImageUiState>(StudioImageUiState.Idle)
    val studioImageState: StateFlow<StudioImageUiState> = _studioImageState.asStateFlow()

    fun setHighThinkingEnabled(enabled: Boolean) {
        _isHighThinkingEnabled.value = enabled
        if (enabled) _isFastLiteMode.value = false
    }

    fun setFastLiteMode(enabled: Boolean) {
        _isFastLiteMode.value = enabled
        if (enabled) _isHighThinkingEnabled.value = false
    }

    fun setSelectedResolution(option: StudioResolutionOption) {
        _selectedResolution.value = option
    }

    fun sendChatMessage(userPrompt: String, contextSnapshot: String = "") {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() || _isChatLoading.value) return

        val highThinking = _isHighThinkingEnabled.value
        val fastLite = _isFastLiteMode.value
        val badge = when {
            highThinking -> "gemini-4-argon (Thinking HIGH)"
            fastLite -> "gemini-3.5-flash-lite (Rapide)"
            else -> "gemini-3.8-flash"
        }

        _chatMessages.value = _chatMessages.value + ChatMessageItem(
            isUser = true,
            text = trimmed,
            modelBadge = "Admin"
        )
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val fullPrompt = if (contextSnapshot.isNotBlank()) {
                    "Contexte actuel de l'entreprise Équipements Thermiques:\n$contextSnapshot\n\nQuestion de l'utilisateur:\n$trimmed"
                } else {
                    trimmed
                }

                val responseText = withTimeout(60_000L) {
                    if (highThinking) {
                        val chat = activeThinkingChat ?: argonHighThinkingModel.startChat().also {
                            activeThinkingChat = it
                        }
                        chat.sendMessage(fullPrompt).text
                    } else if (fastLite) {
                        flashLiteModel.generateContent(fullPrompt).text
                    } else {
                        val chat = activeFlashChat ?: flashModel.startChat().also {
                            activeFlashChat = it
                        }
                        chat.sendMessage(fullPrompt).text
                    }
                }

                val finalReply = responseText?.takeIf { it.isNotBlank() }
                    ?: "Le modèle a retourné une réponse vide."
                _chatMessages.value = _chatMessages.value + ChatMessageItem(
                    isUser = false,
                    text = finalReply,
                    modelBadge = badge
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _chatMessages.value = _chatMessages.value + ChatMessageItem(
                    isUser = false,
                    text = "Délai d'attente dépassé (60s). Veuillez réessayer.",
                    modelBadge = "Erreur"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat inference failed", e)
                _chatMessages.value = _chatMessages.value + ChatMessageItem(
                    isUser = false,
                    text = "Erreur IA: ${e.localizedMessage ?: "Échec de génération."}",
                    modelBadge = "Erreur"
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    /**
     * Image understanding using gemini-4-argon: analyzes uploaded photo of paper timesheet,
     * supplier invoice, or thermal equipment installation.
     */
    fun analyzeUploadedImage(context: Context, imageUri: Uri, customInstruction: String) {
        viewModelScope.launch {
            _mediaAnalysisState.value = MediaAnalysisUiState.Loading
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } ?: throw IllegalArgumentException("Impossible de lire l'image sélectionnée.")

                val prompt = customInstruction.ifBlank {
                    "Analyse cette image pour Équipements Thermiques. Si c'est une fiche de pointage ou facture fournisseur, extrais les noms, heures, tarifs ou articles de stock. Si c'est un équipement thermique, donne un diagnostic technique détaillé."
                }

                val response = withTimeout(60_000L) {
                    argonVisionModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _mediaAnalysisState.value = MediaAnalysisUiState.Success(
                        resultText = text,
                        mediaLabel = "Analyse Photo (gemini-4-argon)"
                    )
                } else {
                    _mediaAnalysisState.value = MediaAnalysisUiState.Error("Réponse vide du modèle gemini-4-argon.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image analysis timed out", e)
                _mediaAnalysisState.value = MediaAnalysisUiState.Error("Analyse d'image expirée après 60 secondes.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image analysis failed", e)
                _mediaAnalysisState.value = MediaAnalysisUiState.Error(e.localizedMessage ?: "Échec de l'analyse d'image.")
            }
        }
    }

    /**
     * Video understanding using gemini-4-argon: analyzes uploaded job-site inspection video.
     */
    fun analyzeUploadedVideo(context: Context, videoUri: Uri, customInstruction: String) {
        viewModelScope.launch {
            _mediaAnalysisState.value = MediaAnalysisUiState.Loading
            try {
                val videoBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(videoUri)?.use { input ->
                        input.readBytes()
                    }
                } ?: throw IllegalArgumentException("Impossible de lire la vidéo sélectionnée.")

                if (videoBytes.size > 19 * 1024 * 1024) {
                    _mediaAnalysisState.value = MediaAnalysisUiState.Error("La vidéo dépasse la limite de 20 Mo pour l'analyse en ligne.")
                    return@launch
                }

                val mimeType = context.contentResolver.getType(videoUri) ?: "video/mp4"
                val prompt = customInstruction.ifBlank {
                    "Analyse cette vidéo d'inspection de chantier thermique. Identifie les équipements visibles, l'avancement des travaux, les anomalies éventuelles et les besoins en pièces de rechange."
                }

                val response = withTimeout(60_000L) {
                    argonVisionModel.generateContent(
                        content {
                            inlineData(videoBytes, mimeType)
                            text(prompt)
                        }
                    )
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _mediaAnalysisState.value = MediaAnalysisUiState.Success(
                        resultText = text,
                        mediaLabel = "Analyse Vidéo Chantier (gemini-4-argon)"
                    )
                } else {
                    _mediaAnalysisState.value = MediaAnalysisUiState.Error("Réponse vide lors de l'analyse vidéo.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Video analysis timed out", e)
                _mediaAnalysisState.value = MediaAnalysisUiState.Error("Analyse vidéo expirée après 60 secondes.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video analysis failed", e)
                _mediaAnalysisState.value = MediaAnalysisUiState.Error(e.localizedMessage ?: "Échec de l'analyse vidéo.")
            }
        }
    }

    /**
     * High-quality image generation using gemini-3-pro-image with 1K, 2K, and 4K size selector.
     */
    fun generateStudioThermalImage(promptText: String) {
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isEmpty()) {
            _studioImageState.value = StudioImageUiState.Error("Veuillez décrire le visuel ou schéma thermique à générer.")
            return
        }
        val resolution = _selectedResolution.value

        viewModelScope.launch {
            _studioImageState.value = StudioImageUiState.Loading
            try {
                val config = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                        .setImageSize(resolution.imageSize)
                        .build()
                }
                val imageModel = Firebase.ai.generativeModel(
                    modelName = "gemini-3-pro-image",
                    generationConfig = config
                )

                val bitmap = withTimeout(60_000L) {
                    val response = imageModel.generateContent(cleanPrompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (bitmap != null) {
                    _studioImageState.value = StudioImageUiState.Success(
                        bitmap = bitmap,
                        resolutionLabel = resolution.label
                    )
                } else {
                    _studioImageState.value = StudioImageUiState.Error("Aucune image n'a été retournée par le modèle.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Studio image generation timed out", e)
                _studioImageState.value = StudioImageUiState.Error("Génération d'image expirée après 60 secondes.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Studio image generation failed", e)
                _studioImageState.value = StudioImageUiState.Error(
                    e.localizedMessage ?: "Échec de la génération d'image."
                )
            }
        }
    }

    private companion object {
        const val TAG = "GeminiAiVM"
    }
}
