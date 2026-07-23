package com.matth.scaleconnect.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.ui.theme.ScaleTheme
import kotlin.math.roundToInt

data class TrendPoint(val dateLabel: String, val value: Double)

private fun formatAxisValue(value: Double): String =
    if (value >= 500.0) "%,.0f".format(value) else "%.1f".format(value)

@Composable
fun WeightTrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    selectedIndices: Set<Int> = emptySet(),
    onPointTap: (Int) -> Unit = {},
    onRangeSelect: (Int, Int) -> Unit = { _, _ -> },
) {
    val extended = ScaleTheme.extendedColors
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (points.isEmpty()) {
        Box(modifier = modifier.height(150.dp), contentAlignment = Alignment.Center) {
            Text("No weigh-ins yet", color = extended.faint, fontSize = 13.sp)
        }
        return
    }

    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }
    val range = (maxV - minV).takeIf { it > 0.01 } ?: 1.0

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(
                modifier = Modifier.width(38.dp).fillMaxHeight().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatAxisValue(maxV), fontSize = 10.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
                Text(formatAxisValue((maxV + minV) / 2), fontSize = 10.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
                Text(formatAxisValue(minV), fontSize = 10.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 6.dp)
                    .pointerInput(points) {
                        val padLeft = 2.dp.toPx()
                        val padRight = 10.dp.toPx()
                        fun nearestIndex(tapX: Float): Int {
                            if (points.size <= 1) return 0
                            val stepX = (size.width - padLeft - padRight) / (points.size - 1)
                            return ((tapX - padLeft) / stepX).roundToInt().coerceIn(0, points.size - 1)
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val id1 = down.id
                            var index1 = nearestIndex(down.position.x)
                            var id2: PointerId? = null
                            var index2: Int? = null
                            onPointTap(index1)

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }

                                pressed.find { it.id == id1 }?.let {
                                    index1 = nearestIndex(it.position.x)
                                    it.consume()
                                }
                                if (id2 == null && pressed.size >= 2) {
                                    id2 = pressed.first { it.id != id1 }.id
                                }
                                id2?.let { pid ->
                                    pressed.find { it.id == pid }?.let {
                                        index2 = nearestIndex(it.position.x)
                                        it.consume()
                                    }
                                }

                                val idx2 = index2
                                if (idx2 != null && idx2 != index1) {
                                    onRangeSelect(index1, idx2)
                                } else {
                                    onPointTap(index1)
                                }

                                if (pressed.isEmpty()) break
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val padLeft = 2.dp.toPx()
                val padRight = 10.dp.toPx()
                val topY = 8.dp.toPx()
                val bottomY = h - 8.dp.toPx()

                for (i in 0..2) {
                    val y = topY + (bottomY - topY) * i / 2
                    drawLine(line, Offset(padLeft, y), Offset(w - padRight, y), strokeWidth = 1.dp.toPx())
                }

                val stepX = if (points.size > 1) (w - padLeft - padRight) / (points.size - 1) else 0f
                val coords = points.mapIndexed { i, p ->
                    val x = padLeft + stepX * i
                    val normalized = ((p.value - minV) / range).toFloat()
                    val y = bottomY - normalized * (bottomY - topY)
                    Offset(x, y)
                }

                val fillPath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, bottomY)
                    lineTo(coords.first().x, bottomY)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0f))),
                )

                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    linePath,
                    color = accent,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                if (selectedIndices.isEmpty()) {
                    coords.lastOrNull()?.let { last ->
                        drawCircle(color = surfaceColor, radius = 4.5.dp.toPx(), center = last)
                        drawCircle(color = accent, radius = 4.5.dp.toPx(), center = last, style = Stroke(width = 2.5.dp.toPx()))
                    }
                }

                selectedIndices.forEach { idx ->
                    coords.getOrNull(idx)?.let { p ->
                        drawLine(
                            color = accent.copy(alpha = 0.35f),
                            start = Offset(p.x, topY),
                            end = Offset(p.x, bottomY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                        drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = p)
                        drawCircle(color = accent, radius = 6.dp.toPx(), center = p, style = Stroke(width = 3.dp.toPx()))
                    }
                }

                if (selectedIndices.size == 2) {
                    val (i1, i2) = selectedIndices.toList().let { it[0] to it[1] }
                    val p1 = coords.getOrNull(i1)
                    val p2 = coords.getOrNull(i2)
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = accent.copy(alpha = 0.55f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(points.first().dateLabel, fontSize = 11.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
            if (points.size > 2) {
                Text(points[points.size / 2].dateLabel, fontSize = 11.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
            }
            Text(points.last().dateLabel, fontSize = 11.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
        }
    }
}
