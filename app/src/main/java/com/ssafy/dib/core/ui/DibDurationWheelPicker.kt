package com.ssafy.dib.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlin.math.abs

private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 60L * SecondsPerMinute
private const val SecondsPerDay = 24L * SecondsPerHour

@Composable
fun DibDurationWheelPicker(
    durationSeconds: Long,
    onDurationChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val parts = remember(durationSeconds) { durationParts(durationSeconds) }
    var days by remember { mutableIntStateOf(parts.days) }
    var hours by remember { mutableIntStateOf(parts.hours) }
    var minutes by remember { mutableIntStateOf(parts.minutes) }

    LaunchedEffect(days, hours, minutes) {
        val updated = durationSeconds(days, hours, minutes)
        if (updated != durationSeconds) onDurationChange(updated)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Colors.Background, RoundedCornerShape(18.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DurationWheel("일", 0..30, days, { days = it }, Modifier.weight(1f))
            DurationWheel("시간", 0..23, hours, { hours = it }, Modifier.weight(1f))
            DurationWheel("분", 0..59, minutes, { minutes = it }, Modifier.weight(1f))
        }
        Text(
            text = "총 ${durationSummary(days, hours, minutes)}",
            modifier = Modifier.fillMaxWidth(),
            color = Colors.Navy,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DurationWheel(
    unit: String,
    values: IntRange,
    selectedValue: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = (selectedValue - values.first).coerceIn(0, values.count() - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                val centered = listState.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs(item.offset + item.size / 2 - viewportCenter)
                }
                centered?.index?.let { index -> onSelected(values.first + index) }
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(unit, color = Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .padding(top = 6.dp)
                .width(88.dp)
                .height(132.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Colors.NavySoft, RoundedCornerShape(10.dp))
                    .border(1.dp, Colors.Navy.copy(alpha = .12f), RoundedCornerShape(10.dp))
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxWidth().height(132.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 44.dp)
            ) {
                items(values.count()) { index ->
                    val value = values.first + index
                    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = value.toString().padStart(2, '0'),
                            color = if (value == selectedValue) Colors.Navy else Colors.Muted.copy(alpha = .55f),
                            fontSize = if (value == selectedValue) 21.sp else 15.sp,
                            fontWeight = if (value == selectedValue) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

internal data class DurationParts(val days: Int, val hours: Int, val minutes: Int)

internal fun durationParts(seconds: Long): DurationParts {
    val safeSeconds = seconds.coerceAtLeast(0)
    return DurationParts(
        days = (safeSeconds / SecondsPerDay).toInt().coerceAtMost(30),
        hours = ((safeSeconds % SecondsPerDay) / SecondsPerHour).toInt(),
        minutes = ((safeSeconds % SecondsPerHour) / SecondsPerMinute).toInt()
    )
}

internal fun durationSeconds(days: Int, hours: Int, minutes: Int): Long =
    days.coerceIn(0, 30) * SecondsPerDay +
        hours.coerceIn(0, 23) * SecondsPerHour +
        minutes.coerceIn(0, 59) * SecondsPerMinute

private fun durationSummary(days: Int, hours: Int, minutes: Int): String = buildList {
    if (days > 0) add("${days}일")
    if (hours > 0) add("${hours}시간")
    if (minutes > 0 || isEmpty()) add("${minutes}분")
}.joinToString(" ")
