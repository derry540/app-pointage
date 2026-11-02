package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.util.NanoBananaBannerManager
import kotlinx.coroutines.launch

@Composable
fun ThermalHeroArtwork(
    modifier: Modifier = Modifier,
    height: Dp = 175.dp,
    showRegenerateButton: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiHeroBitmap by NanoBananaBannerManager.heroBitmap.collectAsStateWithLifecycle()
    val isGenerating by NanoBananaBannerManager.isGenerating.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        NanoBananaBannerManager.ensureOrGenerateHeroBanner(context, forceRegenerate = false)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
    ) {
        val currentBitmap = aiHeroBitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Bannière Nano Banana Pro Équipements Thermiques",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_thermal_hero),
                contentDescription = "Bannière HD Équipements Thermiques",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Subtle bottom-to-top cinematic vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x22090D16),
                            Color(0x55090D16),
                            Color(0xCC090D16)
                        )
                    )
                )
        )

        if (showRegenerateButton) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            NanoBananaBannerManager.ensureOrGenerateHeroBanner(
                                context = context,
                                forceRegenerate = true
                            )
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xCC0F172A),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.testTag("regenerate_nano_banana_banner_btn")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF97316)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nano Banana Pro...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Régénérer Nano Banana Pro",
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFF97316)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentBitmap != null) "Design IA (Nano Banana Pro)" else "Générer Design IA HD",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
