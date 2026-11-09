package com.listenstats.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listenstats.app.data.RankItem
import com.listenstats.app.data.StatRange

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun RangeSelector(
    selected: StatRange,
    onSelect: (StatRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (r in StatRange.values()) {
            FilterChip(
                selected = r == selected,
                onClick = { onSelect(r) },
                label = { Text(r.label, style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

/** Big rounded card with a title line and free content. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(modifier = Modifier.padding(Dim.cardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Highlight card used for top artist / song / album. */
@Composable
fun HighlightCard(
    label: String,
    value: String,
    sub: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = onContainer.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One ranked row: position, name, sub line, time + plays, and a share bar. */
@Composable
fun RankRow(position: Int, item: RankItem, maxMs: Long, showSub: Boolean) {
    val fraction = if (maxMs > 0L) (item.listenedMs.toFloat() / maxMs.toFloat()).coerceIn(0.02f, 1f) else 0.02f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showSub && item.sub.isNotBlank()) {
                        Text(
                            text = item.sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Fmt.duration(item.listenedMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Fmt.plays(item.plays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Bar chart drawn with Canvas only. [axisLabels] are laid out evenly under the bars
 * (first label at the left edge, last at the right edge).
 */
@Composable
fun BarChart(
    values: List<Long>,
    axisLabels: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1
) {
    val barColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dim.chartHeight)
        ) {
            val n = values.size
            if (n > 0) {
                var maxV = 1L
                for (v in values) {
                    if (v > maxV) maxV = v
                }
                val gap = Dim.barGap.toPx()
                val barW = (size.width - gap * (n - 1)) / n
                val radius = minOf(barW / 2f, 10.dp.toPx())
                val minBar = 6.dp.toPx()
                for (i in 0 until n) {
                    val v = values[i]
                    val h = if (v > 0L) {
                        maxOf((v.toFloat() / maxV.toFloat()) * size.height, minBar)
                    } else {
                        minBar
                    }
                    val color = when {
                        v <= 0L -> emptyColor
                        i == highlightIndex -> highlightColor
                        else -> barColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * (barW + gap), size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
            }
        }
        if (axisLabels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            if (axisLabels.size == values.size) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (label in axisLabels) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (label in axisLabels) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
