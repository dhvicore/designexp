package com.nmims.songstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor

/** Original, static music artwork: no network, animation loop or image processing. */
@Composable
fun LoginBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        Color(0xFF102D29), Color(0xFF214B3E), Color(0xFF17392F)
    )))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Brush.radialGradient(listOf(Color(0xFFB4A0CB).copy(alpha = .28f), Color.Transparent),
                center = Offset(w * 1.05f, h * .15f), radius = w * .9f), w * .9f, Offset(w * 1.05f, h * .15f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFAFC87C).copy(alpha = .24f), Color.Transparent),
                center = Offset(w * -.15f, h * .78f), radius = w), w, Offset(w * -.15f, h * .78f))
            // Five curved staff lines make the background a flowing musical score.
            repeat(5) { i ->
                val staff = Path().apply {
                    moveTo(-w * .2f, h * (.22f + i * .016f))
                    cubicTo(w * .33f, h * (.03f + i * .016f), w * .62f, h * (.40f + i * .016f), w * 1.22f, h * (.15f + i * .016f))
                }
                drawPath(staff, Color(0xFFE5EBC6).copy(alpha = .10f), style = Stroke(1.dp.toPx()))
            }
            repeat(5) { i ->
                val staff = Path().apply {
                    moveTo(-w * .1f, h * (.78f + i * .014f))
                    cubicTo(w * .28f, h * (.62f + i * .014f), w * .77f, h * (1.03f + i * .014f), w * 1.15f, h * (.78f + i * .014f))
                }
                drawPath(staff, Color(0xFFDBCDEF).copy(alpha = .12f), style = Stroke(1.dp.toPx()))
            }
            // A vinyl-like halo and sparse sparkles add depth without obscuring fields.
            repeat(5) { i ->
                drawCircle(Color(0xFFDCEB9E).copy(alpha = .045f), w * (.22f + .045f * i),
                    Offset(w * .98f, h * .12f), style = Stroke(1.dp.toPx()))
            }
            repeat(24) { i ->
                val point = Offset(w * ((i * 37 % 97) / 97f), h * ((i * 29 % 101) / 101f))
                drawCircle(Color.White.copy(alpha = .12f), if (i % 3 == 0) 1.3.dp.toPx() else .7.dp.toPx(), point)
            }
            fun note(x: Float, y: Float, scale: Float, paired: Boolean, alpha: Float) {
                val color = if (paired) Lavender.copy(alpha = alpha) else Lime.copy(alpha = alpha)
                val rw = 7.dp.toPx() * scale
                val rh = 4.5.dp.toPx() * scale
                val stem = 27.dp.toPx() * scale
                fun head(cx: Float, cy: Float) {
                    rotate(-22f, Offset(cx, cy)) {
                        drawOval(color, Offset(cx - rw, cy - rh), Size(rw * 2, rh * 2))
                    }
                }
                head(x, y)
                drawLine(color, Offset(x + rw * .8f, y), Offset(x + rw * .8f, y - stem), 2.dp.toPx() * scale, StrokeCap.Round)
                if (paired) {
                    val x2 = x + 24.dp.toPx() * scale
                    val y2 = y - 6.dp.toPx() * scale
                    head(x2, y2)
                    drawLine(color, Offset(x2 + rw * .8f, y2), Offset(x2 + rw * .8f, y2 - stem), 2.dp.toPx() * scale, StrokeCap.Round)
                    drawLine(color, Offset(x + rw * .8f, y - stem), Offset(x2 + rw * .8f, y2 - stem), 5.dp.toPx() * scale)
                } else {
                    val flag = Path().apply {
                        moveTo(x + rw * .8f, y - stem)
                        quadraticTo(x + rw * 3, y - stem + rh * 2, x + rw * 1.7f, y - stem + rh * 4)
                    }
                    drawPath(flag, color, style = Stroke(2.dp.toPx() * scale, cap = StrokeCap.Round))
                }
            }
            note(w * .81f, h * .21f, 1.4f, true, .32f)
            note(w * .12f, h * .32f, 1.15f, false, .18f)
            note(w * .91f, h * .50f, .9f, false, .16f)
            note(w * .13f, h * .67f, 1.3f, true, .16f)
            note(w * .74f, h * .91f, 1.25f, true, .28f)
        }
        CompositionLocalProvider(LocalContentColor provides Paper) { content() }
    }
}

@Composable
fun LoginHeader(signup: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(45.dp).background(Brush.linearGradient(listOf(Lime.copy(alpha = .23f), Lavender.copy(alpha = .16f))), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text("♫", color = Lime, fontSize = 29.sp)
            }
            Column {
                Text("Syllabeat", color = Paper, fontSize = 29.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                Text("YOUR LEARNING, REMIXED", color = Color(0xFFB9CBB6), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp)
            }
        }
        Spacer(Modifier.height(3.dp))
        Column {
            Text(if (signup) "A fresh start." else "Your syllabus.", color = Paper,
                fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
            Text(if (signup) "A new rhythm." else "Your soundtrack.", color = Lime,
                fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
        }
        Text("Turn a topic into a song. Make every revision a little more you.",
            color = Color(0xFFD1DFCD), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(max = 310.dp))
    }
}

@Composable
fun LoginGlassPanel(content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides Ink) {
        Column(Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFFFFFEF4).copy(alpha = .91f), Color(0xFFF1F4E9).copy(alpha = .78f))), RoundedCornerShape(27.dp))
            .border(1.dp, Color.White.copy(alpha = .65f), RoundedCornerShape(27.dp)).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
fun LoginCollectionHint() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf("∫" to "98 topics", "♫" to "9 songs", "↻" to "Offline audio").forEach { (icon, label) ->
            Column(Modifier.weight(1f).background(Color.White.copy(alpha = .07f), RoundedCornerShape(15.dp))
                .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(15.dp)).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(icon, color = Lime, fontSize = 20.sp)
                Text(label, color = Color(0xFFD1DFCD), fontSize = 11.sp)
            }
        }
    }
}
