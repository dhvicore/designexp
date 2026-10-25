package com.nmims.songstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import com.nmims.songstudio.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF5F5EC)
val Ink = Color(0xFF202B24)
val Lime = Color(0xFFDCEB9E)
val Lavender = Color(0xFFE6DFF3)
val Accent = Color(0xFF28654E)
@OptIn(ExperimentalTextApi::class)
val StudioFont = FontFamily(
    Font(R.font.outfit, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.outfit, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Accent, onPrimary = Color.White,
            background = Paper, surface = Color.White, onSurface = Ink,
            onBackground = Ink, surfaceVariant = Color(0xFFE8EADD),
            outline = Color(0xFF96948D), outlineVariant = Color(0xFFDCDACE),
            secondary = Color(0xFF586843), secondaryContainer = Lime, onSecondaryContainer = Ink),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = StudioFont, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
            headlineMedium = TextStyle(fontFamily = StudioFont, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
        ).let { type -> type.copy(
            displayLarge = type.displayLarge.copy(fontFamily = StudioFont),
            displayMedium = type.displayMedium.copy(fontFamily = StudioFont),
            displaySmall = type.displaySmall.copy(fontFamily = StudioFont),
            headlineLarge = type.headlineLarge.copy(fontFamily = StudioFont),
            headlineMedium = type.headlineMedium.copy(fontFamily = StudioFont),
            headlineSmall = type.headlineSmall.copy(fontFamily = StudioFont),
            titleLarge = type.titleLarge.copy(fontFamily = StudioFont),
            titleMedium = type.titleMedium.copy(fontFamily = StudioFont),
            titleSmall = type.titleSmall.copy(fontFamily = StudioFont),
            bodyLarge = type.bodyLarge.copy(fontFamily = StudioFont),
            bodyMedium = type.bodyMedium.copy(fontFamily = StudioFont),
            bodySmall = type.bodySmall.copy(fontFamily = StudioFont),
            labelLarge = type.labelLarge.copy(fontFamily = StudioFont),
            labelMedium = type.labelMedium.copy(fontFamily = StudioFont),
            labelSmall = type.labelSmall.copy(fontFamily = StudioFont)
        ) }, content = content
    )
}

/** Soft atmospheric artwork, drawn locally so the app stays fully offline. */
@Composable
fun StudioBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF5F5EC), Color(0xFFFAFAF3), Color(0xFFF0F2E4))))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFE6DFF3).copy(alpha = .52f), Color.Transparent), center = Offset(size.width, size.height * .12f), radius = size.width * .9f), radius = size.width * .9f, center = Offset(size.width, size.height * .12f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFDCEB9E).copy(alpha = .55f), Color.Transparent), center = Offset(0f, size.height * .68f), radius = size.width), radius = size.width, center = Offset(0f, size.height * .68f))
            repeat(4) { n ->
                val wave = Path().apply {
                    moveTo(-size.width * .15f, size.height * (.22f + n * .025f))
                    cubicTo(size.width * .32f, size.height * .01f, size.width * .57f, size.height * .55f, size.width * 1.16f, size.height * .27f)
                }
                drawPath(wave, Color(0xFF8DAA7E).copy(alpha = .10f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = (1 + n).dp.toPx()))
            }
        }
        CompositionLocalProvider(LocalContentColor provides Ink) { content() }
    }
}

@Composable
fun StudioBrand() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(Brush.linearGradient(listOf(Accent, Color(0xFF657647))), RoundedCornerShape(13.dp)), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("♫", color = Color.White, fontSize = 24.sp)
        }
        Column {
            Text("Song Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("LEARN IN YOUR RHYTHM", style = MaterialTheme.typography.labelSmall, color = Accent)
        }
    }
}

@Composable
fun LearningSteps(page: Int) {
    val labels = listOf("Learn", "Sound", "Lyrics", "Listen", "Practice")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            val active = index + 1 == page
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(32.dp).background(if (active) Accent else if (index + 1 < page) Color(0xFFDCEB9E) else Color.White.copy(alpha = .75f), RoundedCornerShape(12.dp)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(if (index + 1 < page) "✓" else "${index + 1}", color = if (active) Color.White else Ink, style = MaterialTheme.typography.labelMedium)
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Accent else Ink.copy(alpha = .65f), letterSpacing = 0.sp)
            }
        }
    }
}


/** Original abstract sound artwork, rather than a downloaded stock background. */
@Composable
fun StudioHero(title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
        Brush.linearGradient(listOf(Color(0xFF102E27), Color(0xFF1C4938), Color(0xFF32664D))))) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF73B58B).copy(alpha = .28f), Color.Transparent), center = Offset(size.width * .9f, 0f), radius = size.width * .8f), radius = size.width * .8f, center = Offset(size.width * .9f, 0f))
            repeat(4) { i ->
                drawCircle(Color(0xFF9AD4A6).copy(alpha = .07f), radius = size.width * (.30f + i * .07f), center = Offset(size.width * .94f, size.height * .34f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
            val wave = Path().apply {
                moveTo(0f, size.height * .80f)
                cubicTo(size.width * .3f, size.height * .58f, size.width * .64f, size.height * 1.1f, size.width, size.height * .74f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(wave, Color(0xFF0C261E).copy(alpha = .3f))
            repeat(12) { i ->
                val x = size.width * .58f + i * size.width * .029f
                val half = (8 + (i * 17 % 35)).dp.toPx()
                drawLine(Color(0xFFAADCB7).copy(alpha = .10f), Offset(x, size.height * .5f - half), Offset(x, size.height * .5f + half), strokeWidth = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("♫", color = Lime, fontSize = 32.sp)
                Column {
                    Text("SONG STUDIO", color = Paper, style = MaterialTheme.typography.titleMedium, letterSpacing = 2.sp)
                    Text("LEARN IN YOUR RHYTHM", color = Color(0xFFB6CEBB), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Paper, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = Color(0xFFD2E1D3), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
