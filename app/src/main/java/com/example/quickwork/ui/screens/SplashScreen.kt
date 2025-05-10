package com.example.quickwork.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import com.example.quickwork.R
import kotlin.math.sin
import androidx.compose.ui.geometry.Size

import androidx.compose.ui.unit.Density


@Composable
fun SplashScreen() {
    val greenColor = Color(0xFF2ECC71)

    val waveAnim = rememberInfiniteTransition()
    val offset by waveAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(greenColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_jobsgo),
                contentDescription = "QuickWork Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        translationY = sin(offset / 100f) * 10
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "QuickWork",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your smart job partner",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Bottom wave animation overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.BottomCenter)
                .clip(WaveShape(offset))
                .background(Color.White)
        )
    }
}

// Custom shape for wave effect
class WaveShape(private val offset: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val waveHeight = 40f
        val path = Path().apply {
            moveTo(0f, waveHeight)
            for (i in 0..size.width.toInt() step 40) {
                quadraticBezierTo(
                    i + 20f, if (i % 80 == 0) 0f else waveHeight * 2,
                    i + 40f, waveHeight
                )
            }
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
