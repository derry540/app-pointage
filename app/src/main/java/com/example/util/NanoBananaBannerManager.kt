package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object NanoBananaBannerManager {

    private const val TAG = "NanoBananaBanner"
    private const val CACHE_FILENAME = "nano_banana_pro_hero_v1.png"

    private val _heroBitmap = MutableStateFlow<Bitmap?>(null)
    val heroBitmap: StateFlow<Bitmap?> = _heroBitmap.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val mutex = Mutex()

    private val prompts = listOf(
        "Ultra-detailed 3D architectural industrial rendering for 'Équipements Thermiques' company: gleaming copper high-pressure steam boilers, glowing digital pressure manometers, warm amber thermal heat waves and cool cyan chilled water pipes in a sleek dark-slate modern mechanical engineering control room, cinematic rim lighting, 8k octane render, no text",
        "Photorealistic modern industrial HVAC and thermal boiler plant room with brushed stainless steel heat exchangers, glowing orange combustion chamber viewport, neon cyan telemetry displays, dramatic volumetric lighting, 8k studio photography, no text",
        "Futuristic high-precision thermal engineering command center with copper manifolds, digital flow meters, warm copper and cool sky-blue lighting accents, ultra-clean industrial design, no text"
    )
    private var promptIndex = 0

    suspend fun ensureOrGenerateHeroBanner(
        context: Context,
        forceRegenerate: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cacheFile = File(context.filesDir, CACHE_FILENAME)
            if (!forceRegenerate && _heroBitmap.value != null) {
                return@withContext _heroBitmap.value
            }
            if (!forceRegenerate && cacheFile.exists() && cacheFile.length() > 1024) {
                val cached = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (cached != null) {
                    _heroBitmap.value = cached
                    return@withContext cached
                }
            }

            _isGenerating.value = true
            try {
                val prompt = prompts[promptIndex % prompts.size]
                promptIndex++

                val config = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                        .setImageSize(ImageSize.SIZE_1K)
                        .build()
                }
                val model = Firebase.ai.generativeModel(
                    modelName = "gemini-3-pro-image",
                    generationConfig = config
                )
                val response = model.generateContent(prompt)
                val generatedBmp = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()?.image

                if (generatedBmp != null) {
                    FileOutputStream(cacheFile).use { out ->
                        generatedBmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    _heroBitmap.value = generatedBmp
                    return@withContext generatedBmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Nano Banana Pro (gemini-3-pro-image) runtime generation fallback: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
            return@withContext _heroBitmap.value
        }
    }
}
