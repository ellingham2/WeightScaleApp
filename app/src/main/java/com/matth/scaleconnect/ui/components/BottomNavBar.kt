package com.matth.scaleconnect.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.ui.theme.ScaleTheme

enum class NavDestination(val route: String, val label: String) {
    Dashboard("dashboard", "Dashboard"),
    History("history", "History"),
    Profile("profile", "Profile"),
}

@Composable
fun BottomNavBar(current: NavDestination, onSelect: (NavDestination) -> Unit) {
    val extended = ScaleTheme.extendedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavDestination.entries.forEach { dest ->
            val active = dest == current
            val color = if (active) MaterialTheme.colorScheme.primary else extended.muted
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(dest) },
            ) {
                NavIcon(dest, color)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dest.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun NavIcon(dest: NavDestination, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        when (dest) {
            NavDestination.Dashboard -> {
                val s = 7.dp.toPx()
                val corner = CornerRadius(2.dp.toPx())
                val positions = listOf(3.dp.toPx() to 3.dp.toPx(), 12.dp.toPx() to 3.dp.toPx(),
                    3.dp.toPx() to 12.dp.toPx(), 12.dp.toPx() to 12.dp.toPx())
                positions.forEach { (x, y) ->
                    drawRoundRect(color, topLeft = Offset(x, y), size = Size(s, s), cornerRadius = corner)
                }
            }
            NavDestination.History -> {
                val barW = 3.4.dp.toPx()
                val corner = CornerRadius(1.2.dp.toPx())
                drawRoundRect(color, topLeft = Offset(3.dp.toPx(), 12.dp.toPx()), size = Size(barW, 7.dp.toPx()), cornerRadius = corner)
                drawRoundRect(color, topLeft = Offset(9.3.dp.toPx(), 8.dp.toPx()), size = Size(barW, 11.dp.toPx()), cornerRadius = corner)
                drawRoundRect(color, topLeft = Offset(15.6.dp.toPx(), 4.dp.toPx()), size = Size(barW, 15.dp.toPx()), cornerRadius = corner)
            }
            NavDestination.Profile -> {
                drawCircle(color, radius = 3.4.dp.toPx(), center = Offset(11.dp.toPx(), 7.dp.toPx()))
                val path = Path().apply {
                    moveTo(4.5.dp.toPx(), 18.5.dp.toPx())
                    cubicTo(4.5.dp.toPx(), 14.9.dp.toPx(), 7.4.dp.toPx(), 12.5.dp.toPx(), 11.dp.toPx(), 12.5.dp.toPx())
                    cubicTo(14.6.dp.toPx(), 12.5.dp.toPx(), 17.5.dp.toPx(), 14.9.dp.toPx(), 17.5.dp.toPx(), 18.5.dp.toPx())
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}
