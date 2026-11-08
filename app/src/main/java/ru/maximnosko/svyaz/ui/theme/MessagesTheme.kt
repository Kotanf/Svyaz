package ru.maximnosko.svyaz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Svyaz Official Brandbook Design System Tokens (https://maximnosko.gitverse.site/svyaz-stil/)
// ============================================================================
object SvyazBrandColors {
    val ElectricIndigo = Color(0xFF5B5BD6)
    val ElectricIndigoLight = Color(0xFF7B7BFF)
    val NeonCyan = Color(0xFF67E8F9)
    val NeonCyanGlow = Color(0x3367E8F9)
    val Fuchsia = Color(0xFFF0ABFC)
    val FuchsiaGlow = Color(0x26F0ABFC)
    val Indigo = Color(0xFFA5B4FC)
    val Amber = Color(0xFFFCD34D)
    val AmberGlow = Color(0x26FCD34D)
    val Teal = Color(0xFF5EEAD4)
    val TealGlow = Color(0x265EEAD4)
    val Sky = Color(0xFF7DD3FC)
    val SkyGlow = Color(0x267DD3FC)
    val Emerald = Color(0xFF6EE7B7)
    val EmeraldDelivered = Color(0xFF34D399)
    val Rose = Color(0xFFFDA4AF)
    val RoseDanger = Color(0xFFF43F5E)

    // Cosmic Glass Backgrounds & Surfaces
    val CosmicBgDark = Color(0xFF0B0C16)
    val CosmicSurfaceDark = Color(0xFF0E101D)
    val CosmicSurfaceContainerDark = Color(0xFF16182B)
    val CosmicSurfaceContainerHighDark = Color(0xFF1D2038)
    val CosmicGlassCardDark = Color(0x1AFFFFFF) // rgba(255, 255, 255, 0.10)
    val CosmicGlassBorder = Color(0x1FFFFFFF)   // rgba(255, 255, 255, 0.12)
    val CosmicBorderCyan = Color(0x4067E8F9)   // rgba(103, 232, 249, 0.25)
    val CosmicTextPrimary = Color(0xFFF0EFF4)
    val CosmicTextSecondary = Color(0xFFA5B4FC)
    val CosmicTextMuted = Color(0x80C7D2FE)
}

// ============================================================================
// 1. Theme Mode (Системная / Светлая / Тёмная)
// ============================================================================
enum class ThemeMode(val id: String, val title: String) {
    SYSTEM("system", "Системная"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Тёмная");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

// ============================================================================
// 2. Font Scale (Масштаб текста интерфейса)
// ============================================================================
enum class AppFontScale(val id: String, val scale: Float, val title: String) {
    SMALL("small", 0.88f, "Компактный"),
    NORMAL("normal", 1.0f, "Обычный"),
    LARGE("large", 1.15f, "Крупный"),
    EXTRA_LARGE("extra_large", 1.30f, "Очень крупный");

    companion object {
        fun fromId(id: String?): AppFontScale = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

// ============================================================================
// 3. Color Theme Presets (Фирменный Космос + Палитры Оформления)
// ============================================================================
enum class AppThemePreset(
    val id: String,
    val title: String,
    val primaryPreviewColor: Color,
    val secondaryPreviewColor: Color,
    val isOledBlack: Boolean = false
) {
    COSMIC(
        id = "cosmic",
        title = "Связь Космос",
        primaryPreviewColor = Color(0xFF5B5BD6),
        secondaryPreviewColor = Color(0xFF67E8F9)
    ),
    SAPPHIRE(
        id = "sapphire",
        title = "Сапфир",
        primaryPreviewColor = Color(0xFF0061A4),
        secondaryPreviewColor = Color(0xFF535F70)
    ),
    EMERALD(
        id = "emerald",
        title = "Изумруд",
        primaryPreviewColor = Color(0xFF006C4C),
        secondaryPreviewColor = Color(0xFF4D6356)
    ),
    AMETHYST(
        id = "amethyst",
        title = "Аметист",
        primaryPreviewColor = Color(0xFF704AB2),
        secondaryPreviewColor = Color(0xFF635B70)
    ),
    SUNSET(
        id = "sunset",
        title = "Закат",
        primaryPreviewColor = Color(0xFFB04300),
        secondaryPreviewColor = Color(0xFF775747)
    ),
    RUBY(
        id = "ruby",
        title = "Рубин",
        primaryPreviewColor = Color(0xFF984061),
        secondaryPreviewColor = Color(0xFF74565F)
    ),
    GRAPHITE(
        id = "graphite",
        title = "Графит (OLED)",
        primaryPreviewColor = Color(0xFF32363A),
        secondaryPreviewColor = Color(0xFF5C6065),
        isOledBlack = true
    );

    companion object {
        fun fromId(id: String?): AppThemePreset = entries.firstOrNull { it.id == id } ?: COSMIC
    }
}

// ============================================================================
// Hand-Crafted ColorSchemes (Light & Dark)
// ============================================================================

// 0. COSMIC (Фирменный брендбук «Связь» — Электрический Индиго & Неоновый Циан)
private val CosmicLightScheme = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2FF),
    onPrimaryContainer = Color(0xFF141466),
    secondary = Color(0xFF0891B2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF00363D),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF3B0764),
    background = Color(0xFFF7F7FD),
    onBackground = Color(0xFF151624),
    surface = Color(0xFFF7F7FD),
    onSurface = Color(0xFF151624),
    surfaceVariant = Color(0xFFE5E7F4),
    onSurfaceVariant = Color(0xFF454763),
    outline = Color(0xFF737699),
    outlineVariant = Color(0xFFC4C7E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F1FB),
    surfaceContainer = Color(0xFFEAECF7),
    surfaceContainerHigh = Color(0xFFE3E5F2),
    surfaceContainerHighest = Color(0xFFDCDEED)
)

private val CosmicDarkScheme = darkColorScheme(
    primary = Color(0xFF7B7BFF),
    onPrimary = Color(0xFF1A1A70),
    primaryContainer = Color(0xFF3E3EB5),
    onPrimaryContainer = Color(0xFFE4E4FF),
    secondary = Color(0xFF67E8F9),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFBCEBF2),
    tertiary = Color(0xFFF0ABFC),
    onTertiary = Color(0xFF521165),
    tertiaryContainer = Color(0xFF6E2882),
    onTertiaryContainer = Color(0xFFFDD8FF),
    background = Color(0xFF0B0C16), // Cosmic deep space
    onBackground = Color(0xFFF0EFF4),
    surface = Color(0xFF0E101D),    // Translucent dark glass canvas
    onSurface = Color(0xFFF0EFF4),
    surfaceVariant = Color(0xFF1B1D30),
    onSurfaceVariant = Color(0xFFA5B4FC),
    outline = Color(0xFF45486C),
    outlineVariant = Color(0xFF282B44),
    surfaceContainerLowest = Color(0xFF07080F),
    surfaceContainerLow = Color(0xFF121422),
    surfaceContainer = Color(0xFF16182B),
    surfaceContainerHigh = Color(0xFF1D2038),
    surfaceContainerHighest = Color(0xFF252946)
)

// 1. SAPPHIRE (Классический синий Telegram/Svyaz)
private val SapphireLightScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FD),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4FA),
    surfaceContainer = Color(0xFFECEFF6),
    surfaceContainerHigh = Color(0xFFE6E9F0),
    surfaceContainerHighest = Color(0xFFE0E3EA)
)

private val SapphireDarkScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1E2025),
    surfaceContainerHigh = Color(0xFF282B30),
    surfaceContainerHighest = Color(0xFF33363B)
)

// 2. EMERALD (Свежий изумрудный)
private val EmeraldLightScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8CF8C9),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D7),
    onSecondaryContainer = Color(0xFF0A1F15),
    tertiary = Color(0xFF3D6473),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E9FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FBF7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF3F4944),
    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F1),
    surfaceContainer = Color(0xFFEAF0EC),
    surfaceContainerHigh = Color(0xFFE4EBE6),
    surfaceContainerHighest = Color(0xFFDEE5E0)
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = Color(0xFF6FDBB0),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF8CF8C9),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF20352A),
    secondaryContainer = Color(0xFF364B3F),
    onSecondaryContainer = Color(0xFFD0E8D7),
    tertiary = Color(0xFFA5CDEC),
    onTertiary = Color(0xFF063544),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC1E9FB),
    background = Color(0xFF0E1512),
    onBackground = Color(0xFFE0E4DF),
    surface = Color(0xFF0E1512),
    onSurface = Color(0xFFE0E4DF),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4944),
    surfaceContainerLowest = Color(0xFF090F0D),
    surfaceContainerLow = Color(0xFF161E1A),
    surfaceContainer = Color(0xFF1B231F),
    surfaceContainerHigh = Color(0xFF252D29),
    surfaceContainerHighest = Color(0xFF303834)
)

// 3. AMETHYST (Королевский аметист / фиолетовый)
private val AmethystLightScheme = lightColorScheme(
    primary = Color(0xFF704AB2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDDFF),
    onPrimaryContainer = Color(0xFF270057),
    secondary = Color(0xFF635B70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1F182B),
    tertiary = Color(0xFF7E5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31101D),
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF1D1B1E),
    surface = Color(0xFFFBF8FD),
    onSurface = Color(0xFF1D1B1E),
    surfaceVariant = Color(0xFFE6E0EB),
    onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCBC4CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2F8),
    surfaceContainer = Color(0xFFEFEBF2),
    surfaceContainerHigh = Color(0xFFE9E5EC),
    surfaceContainerHighest = Color(0xFFE3DFE7)
)

private val AmethystDarkScheme = darkColorScheme(
    primary = Color(0xFFD3BAFF),
    onPrimary = Color(0xFF401582),
    primaryContainer = Color(0xFF573199),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFCDC2DB),
    onSecondary = Color(0xFF342D40),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF633B49),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFCBC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF48454E),
    surfaceContainerLowest = Color(0xFF0F0E13),
    surfaceContainerLow = Color(0xFF1C1A20),
    surfaceContainer = Color(0xFF211F25),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)

// 4. SUNSET (Теплый коралловый закат / амбра)
private val SunsetLightScheme = lightColorScheme(
    primary = Color(0xFFB04300),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF3A1000),
    secondary = Color(0xFF775747),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160A),
    tertiary = Color(0xFF655F30),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECE4A7),
    onTertiaryContainer = Color(0xFF1F1C00),
    background = Color(0xFFFCF8F6),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFCF8F6),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433C),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD8C2BA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2EF),
    surfaceContainer = Color(0xFFF1ECE9),
    surfaceContainerHigh = Color(0xFFECE7E3),
    surfaceContainerHighest = Color(0xFFE6E1DD)
)

private val SunsetDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB598),
    onPrimary = Color(0xFF5F1E00),
    primaryContainer = Color(0xFF872E00),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFE7BEAC),
    onSecondary = Color(0xFF442A1D),
    secondaryContainer = Color(0xFF5D4031),
    onSecondaryContainer = Color(0xFFFFDBCE),
    tertiary = Color(0xFFCFC88D),
    onTertiary = Color(0xFF363107),
    tertiaryContainer = Color(0xFF4D481B),
    onTertiaryContainer = Color(0xFFECE4A7),
    background = Color(0xFF18120F),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF18120F),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF53433C),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFFA08C84),
    outlineVariant = Color(0xFF53433C),
    surfaceContainerLowest = Color(0xFF120D0A),
    surfaceContainerLow = Color(0xFF201A17),
    surfaceContainer = Color(0xFF251E1B),
    surfaceContainerHigh = Color(0xFF302825),
    surfaceContainerHighest = Color(0xFF3B3330)
)

// 5. RUBY (Благородный рубин / вино)
private val RubyLightScheme = lightColorScheme(
    primary = Color(0xFF984061),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2D1600),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF847377),
    outlineVariant = Color(0xFFD6C2C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2F3),
    surfaceContainer = Color(0xFFF1ECED),
    surfaceContainerHigh = Color(0xFFEBE7E7),
    surfaceContainerHighest = Color(0xFFE5E1E2)
)

private val RubyDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5B3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF472A0C),
    tertiaryContainer = Color(0xFF613F20),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF181113),
    onBackground = Color(0xFFECE0E1),
    surface = Color(0xFF181113),
    onSurface = Color(0xFFECE0E1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD6C2C6),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
    surfaceContainerLowest = Color(0xFF120C0E),
    surfaceContainerLow = Color(0xFF201A1C),
    surfaceContainer = Color(0xFF251E20),
    surfaceContainerHigh = Color(0xFF30282A),
    surfaceContainerHighest = Color(0xFF3B3335)
)

// 6. GRAPHITE (Минималистичный монохром & Истинный OLED Черный)
private val GraphiteLightScheme = lightColorScheme(
    primary = Color(0xFF32363A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E3E6),
    onPrimaryContainer = Color(0xFF121417),
    secondary = Color(0xFF5C6065),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E4E7),
    onSecondaryContainer = Color(0xFF1A1C1F),
    tertiary = Color(0xFF45616D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBE6F4),
    onTertiaryContainer = Color(0xFF001E27),
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF9FAFB),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2E6),
    onSurfaceVariant = Color(0xFF43474B),
    outline = Color(0xFF73777C),
    outlineVariant = Color(0xFFC3C7CC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4F6),
    surfaceContainer = Color(0xFFECEEF0),
    surfaceContainerHigh = Color(0xFFE6E8EA),
    surfaceContainerHighest = Color(0xFFDFE2E4)
)

// True OLED Black Dark Scheme for Graphite
private val GraphiteDarkScheme = darkColorScheme(
    primary = Color(0xFFE2E4E8),
    onPrimary = Color(0xFF16191C),
    primaryContainer = Color(0xFF2D3034),
    onPrimaryContainer = Color(0xFFE2E4E8),
    secondary = Color(0xFFC5C7CC),
    onSecondary = Color(0xFF2A2D31),
    secondaryContainer = Color(0xFF414449),
    onSecondaryContainer = Color(0xFFE2E4E8),
    tertiary = Color(0xFFAFCBD7),
    onTertiary = Color(0xFF15333E),
    tertiaryContainer = Color(0xFF2D4955),
    onTertiaryContainer = Color(0xFFCBE6F4),
    background = Color(0xFF000000),      // True OLED Pure Black
    onBackground = Color(0xFFE4E5E8),
    surface = Color(0xFF000000),         // Pure Black for AMOLED
    onSurface = Color(0xFFE4E5E8),
    surfaceVariant = Color(0xFF25282C),
    onSurfaceVariant = Color(0xFFC3C7CC),
    outline = Color(0xFF8D9197),
    outlineVariant = Color(0xFF383C40),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0F11),
    surfaceContainer = Color(0xFF14171A),
    surfaceContainerHigh = Color(0xFF1D2024),
    surfaceContainerHighest = Color(0xFF272B2F)
)

fun AppThemePreset.getColorScheme(isDark: Boolean): ColorScheme {
    return when (this) {
        AppThemePreset.COSMIC -> if (isDark) CosmicDarkScheme else CosmicLightScheme
        AppThemePreset.SAPPHIRE -> if (isDark) SapphireDarkScheme else SapphireLightScheme
        AppThemePreset.EMERALD -> if (isDark) EmeraldDarkScheme else EmeraldLightScheme
        AppThemePreset.AMETHYST -> if (isDark) AmethystDarkScheme else AmethystLightScheme
        AppThemePreset.SUNSET -> if (isDark) SunsetDarkScheme else SunsetLightScheme
        AppThemePreset.RUBY -> if (isDark) RubyDarkScheme else RubyLightScheme
        AppThemePreset.GRAPHITE -> if (isDark) GraphiteDarkScheme else GraphiteLightScheme
    }
}

// ============================================================================
// Material 3 Expressive Typography
// ============================================================================
val MessagesTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================================================
// Material 3 Expressive Shapes
// ============================================================================
val MessagesExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MessagesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePreset: AppThemePreset = AppThemePreset.COSMIC,
    fontScale: AppFontScale = AppFontScale.NORMAL,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> themePreset.getColorScheme(isDark)
    }

    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale.scale
    )

    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MessagesTypography,
            shapes = MessagesExpressiveShapes,
            content = content
        )
    }
}
