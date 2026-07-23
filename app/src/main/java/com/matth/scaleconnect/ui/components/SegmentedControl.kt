package com.matth.scaleconnect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.ui.theme.ScaleTheme

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
) {
    val extended = ScaleTheme.extendedColors
    Row(
        modifier = modifier
            .background(extended.surface2, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                modifier = (if (fillWidth) Modifier.weight(1f) else Modifier.wrapContentWidth())
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(opt),
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (active) MaterialTheme.colorScheme.onSurface else extended.muted,
                )
            }
        }
    }
}
