package ru.maximnosko.svyaz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import ru.maximnosko.svyaz.ui.theme.AppThemePreset

/**
 * Authentic Telegram-style wallpaper with customizable gradients for all 6 themes
 * and crisp repeating vector doodles. Optimized for 120 FPS high-refresh rate displays.
 */
@Composable
fun TelegramWallpaper(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    themePreset: AppThemePreset = AppThemePreset.COSMIC
) {
    val gradientColors = remember(isDark, themePreset) {
        if (isDark) {
            when (themePreset) {
                AppThemePreset.COSMIC -> listOf(
                    Color(0xFF161830), // Cosmic Violet glow
                    Color(0xFF101224),
                    Color(0xFF0B0C16)  // Cosmic Deep Space
                )
                AppThemePreset.SAPPHIRE -> listOf(
                    Color(0xFF141F2B),
                    Color(0xFF0F1722),
                    Color(0xFF0A0F17)
                )
                AppThemePreset.EMERALD -> listOf(
                    Color(0xFF10221A),
                    Color(0xFF0B1913),
                    Color(0xFF07110C)
                )
                AppThemePreset.AMETHYST -> listOf(
                    Color(0xFF1E1528),
                    Color(0xFF170F20),
                    Color(0xFF0E0915)
                )
                AppThemePreset.SUNSET -> listOf(
                    Color(0xFF241510),
                    Color(0xFF1B0E09),
                    Color(0xFF110805)
                )
                AppThemePreset.RUBY -> listOf(
                    Color(0xFF241318),
                    Color(0xFF1C0D12),
                    Color(0xFF12070A)
                )
                AppThemePreset.GRAPHITE -> listOf(
                    Color(0xFF0E1012),
                    Color(0xFF050607),
                    Color(0xFF000000) // Deep AMOLED pitch black
                )
            }
        } else {
            when (themePreset) {
                AppThemePreset.COSMIC -> listOf(
                    Color(0xFFE8E9FC),
                    Color(0xFFF1F3FE),
                    Color(0xFFF8F8FD)
                )
                AppThemePreset.SAPPHIRE -> listOf(
                    Color(0xFFD7E7FA),
                    Color(0xFFE2E6FB),
                    Color(0xFFEDE5FA)
                )
                AppThemePreset.EMERALD -> listOf(
                    Color(0xFFD4F3E4),
                    Color(0xFFE0F6EC),
                    Color(0xFFEBF8F2)
                )
                AppThemePreset.AMETHYST -> listOf(
                    Color(0xFFEADDFB),
                    Color(0xFFF1E7FD),
                    Color(0xFFF7F0FE)
                )
                AppThemePreset.SUNSET -> listOf(
                    Color(0xFFFFE3D6),
                    Color(0xFFFFECE3),
                    Color(0xFFFFF4EE)
                )
                AppThemePreset.RUBY -> listOf(
                    Color(0xFFFFE0E8),
                    Color(0xFFFFEBF0),
                    Color(0xFFFFF4F7)
                )
                AppThemePreset.GRAPHITE -> listOf(
                    Color(0xFFE4E7EA),
                    Color(0xFFECEEF1),
                    Color(0xFFF4F6F8)
                )
            }
        }
    }

    val doodleColor = remember(isDark, themePreset) {
        if (isDark) {
            when (themePreset) {
                AppThemePreset.COSMIC -> Color(0xFF67E8F9).copy(alpha = 0.085f)
                AppThemePreset.GRAPHITE -> Color.White.copy(alpha = 0.085f)
                else -> Color.White.copy(alpha = 0.075f)
            }
        } else {
            when (themePreset) {
                AppThemePreset.COSMIC -> Color(0xFF5B5BD6).copy(alpha = 0.11f)
                AppThemePreset.SAPPHIRE -> Color(0xFF0061A4).copy(alpha = 0.11f)
                AppThemePreset.EMERALD -> Color(0xFF006C4C).copy(alpha = 0.11f)
                AppThemePreset.AMETHYST -> Color(0xFF704AB2).copy(alpha = 0.11f)
                AppThemePreset.SUNSET -> Color(0xFFB04300).copy(alpha = 0.11f)
                AppThemePreset.RUBY -> Color(0xFF984061).copy(alpha = 0.11f)
                AppThemePreset.GRAPHITE -> Color(0xFF2A2E33).copy(alpha = 0.11f)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        // 1. Draw smooth vertical gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = size.height
            ),
            size = size
        )

        // 2. Draw subtle ambient glow at the top center for 3D depth
        val glowColor = if (isDark) gradientColors.first().copy(alpha = 0.4f) else gradientColors.first().copy(alpha = 0.25f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.15f),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width / 2f, size.height * 0.15f)
        )

        // 3. Draw repeating large vector doodle icons in staggered grid
        val stepX = 120f
        val stepY = 120f
        val cols = (size.width / stepX).toInt() + 2
        val rows = (size.height / stepY).toInt() + 2

        for (row in 0..rows) {
            val offsetX = if (row % 2 == 1) stepX / 2 else 0f
            for (col in 0..cols) {
                val cx = col * stepX + offsetX
                val cy = row * stepY
                val iconType = (col * 3 + row * 7) % 8
                drawDoodle(iconType, cx, cy, doodleColor)
            }
        }
    }
}

private val reusablePath = ThreadLocal<Path>()

private fun DrawScope.drawDoodle(type: Int, x: Float, y: Float, color: Color) {
    val stroke = Stroke(width = 2.2f)
    var path = reusablePath.get()
    if (path == null) {
        path = Path()
        reusablePath.set(path)
    }
    path.reset()

    when (type) {
        // 0: Speech Bubble with tail (Large 36x28)
        0 -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(x - 18f, y - 14f),
                size = Size(36f, 26f),
                cornerRadius = CornerRadius(8f, 8f),
                style = stroke
            )
            path.moveTo(x - 6f, y + 12f)
            path.lineTo(x - 14f, y + 20f)
            path.lineTo(x, y + 12f)
            path.close()
            drawPath(path, color = color)
            // Speech dots
            drawCircle(color, radius = 2f, center = Offset(x - 8f, y - 1f))
            drawCircle(color, radius = 2f, center = Offset(x, y - 1f))
            drawCircle(color, radius = 2f, center = Offset(x + 8f, y - 1f))
        }
        // 1: Svyaz Rocket / Paper Airplane (Large 38x38)
        1 -> {
            path.moveTo(x - 16f, y - 14f)
            path.lineTo(x + 18f, y)
            path.lineTo(x - 16f, y + 14f)
            path.lineTo(x - 8f, y)
            path.close()
            drawPath(path, color = color, style = stroke)
            path.reset()
            path.moveTo(x - 8f, y)
            path.lineTo(x + 18f, y)
            drawPath(path, color = color, style = stroke)
        }
        // 2: Expressive Heart (Large 36x36)
        2 -> {
            path.moveTo(x, y + 16f)
            path.cubicTo(x - 22f, y, x - 22f, y - 16f, x - 8f, y - 16f)
            path.cubicTo(x, y - 16f, x, y - 8f, x, y - 8f)
            path.cubicTo(x, y - 8f, x, y - 16f, x + 8f, y - 16f)
            path.cubicTo(x + 22f, y - 16f, x + 22f, y, x, y + 16f)
            drawPath(path, color = color, style = stroke)
        }
        // 3: 5-Point Cosmic Star (Large 36x36)
        3 -> {
            path.moveTo(x, y - 18f)
            path.lineTo(x + 5f, y - 5f)
            path.lineTo(x + 18f, y - 5f)
            path.lineTo(x + 8f, y + 4f)
            path.lineTo(x + 12f, y + 16f)
            path.lineTo(x, y + 9f)
            path.lineTo(x - 12f, y + 16f)
            path.lineTo(x - 8f, y + 4f)
            path.lineTo(x - 18f, y - 5f)
            path.lineTo(x - 5f, y - 5f)
            path.close()
            drawPath(path, color = color, style = stroke)
        }
        // 4: Security Shield & Lock (Large 32x36)
        4 -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(x - 14f, y - 2f),
                size = Size(28f, 22f),
                cornerRadius = CornerRadius(6f, 6f),
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x - 8f, y - 14f),
                size = Size(16f, 16f),
                style = stroke
            )
            drawCircle(color, radius = 2.5f, center = Offset(x, y + 7f))
        }
        // 5: Wi-Fi / Mesh Waves Node (Large 36x32)
        5 -> {
            drawArc(
                color = color,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(x - 18f, y - 14f),
                size = Size(36f, 28f),
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(x - 11f, y - 8f),
                size = Size(22f, 18f),
                style = stroke
            )
            drawCircle(color = color, radius = 3f, center = Offset(x, y + 8f))
        }
        // 6: Magic Sparkle Starburst (Large 36x36)
        6 -> {
            path.moveTo(x, y - 16f)
            path.lineTo(x + 4f, y - 4f)
            path.lineTo(x + 16f, y)
            path.lineTo(x + 4f, y + 4f)
            path.lineTo(x, y + 16f)
            path.lineTo(x - 4f, y + 4f)
            path.lineTo(x - 16f, y)
            path.lineTo(x - 4f, y - 4f)
            path.close()
            drawPath(path, color = color, style = stroke)
        }
        // 7: Musical Note & Wave (Large 34x34)
        else -> {
            drawCircle(color = color, radius = 4f, center = Offset(x - 6f, y + 10f))
            drawCircle(color = color, radius = 4f, center = Offset(x + 10f, y + 6f))
            path.moveTo(x - 2f, y + 10f)
            path.lineTo(x - 2f, y - 10f)
            path.lineTo(x + 14f, y - 14f)
            path.lineTo(x + 14f, y + 6f)
            drawPath(path, color = color, style = stroke)
        }
    }
}
