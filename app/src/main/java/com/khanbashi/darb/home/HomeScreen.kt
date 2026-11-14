package com.khanbashi.darb.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanbashi.darb.R
import com.khanbashi.darb.location.SpeedTracker
import com.khanbashi.darb.quran.QuranPlayer
import com.khanbashi.darb.ui.DarbColors
import com.khanbashi.darb.ui.IconRes
import com.khanbashi.darb.ui.Panel
import com.khanbashi.darb.ui.RoundIconButton
import com.khanbashi.darb.ui.openMap
import kotlin.math.min

@Composable
fun HomeScreen(onOpenQuran: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .width(270.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuranCard(Modifier.weight(1f).fillMaxWidth(), onOpenQuran)
            TripCard(Modifier.fillMaxWidth())
        }
        SpeedGauge(Modifier.weight(1f).fillMaxHeight())
        Column(
            modifier = Modifier
                .width(270.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShortcutsCard(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun QuranCard(modifier: Modifier, onOpenQuran: () -> Unit) {
    val title by QuranPlayer.title.collectAsState()
    val subtitle by QuranPlayer.subtitle.collectAsState()
    val playing by QuranPlayer.playing.collectAsState()
    val live by QuranPlayer.live.collectAsState()
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconRes(R.drawable.ic_book, tint = DarbColors.Accent, size = 20.dp)
            Text("القرآن الكريم", color = DarbColors.Sub, fontSize = 14.sp)
        }
        Text(
            title ?: "اختر قارئاً أو سورة أو إذاعة",
            color = DarbColors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(subtitle ?: "من مكتبة mp3quran", color = DarbColors.Sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(1f))
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!live) RoundIconButton(R.drawable.ic_next, "التالي", { QuranPlayer.next() })
                Box(Modifier.width(10.dp))
                RoundIconButton(
                    if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    if (playing) "إيقاف مؤقت" else "تشغيل",
                    { QuranPlayer.toggle() },
                    size = 56.dp,
                    background = DarbColors.Accent,
                    tint = DarbColors.OnAccent
                )
                Box(Modifier.width(10.dp))
                if (!live) RoundIconButton(R.drawable.ic_prev, "السابق", { QuranPlayer.previous() })
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1AFFFFFF))
                .clickable(onClick = onOpenQuran),
            contentAlignment = Alignment.Center
        ) {
            Text("فتح المكتبة", color = DarbColors.Text, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TripCard(modifier: Modifier) {
    val trip by SpeedTracker.tripKm.collectAsState()
    val fix by SpeedTracker.hasFix.collectAsState()
    Panel(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("الرحلة", color = DarbColors.Sub, fontSize = 13.sp)
                Text(String.format("%.1f كم", trip), color = DarbColors.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (fix) "GPS متصل" else "بانتظار GPS", color = if (fix) DarbColors.Ok else DarbColors.Sub, fontSize = 13.sp)
                Text(
                    "تصفير",
                    color = DarbColors.Accent,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { SpeedTracker.resetTrip() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ShortcutsCard(modifier: Modifier) {
    val ctx = LocalContext.current
    Panel(modifier) {
        Text("اختصارات الطريق", color = DarbColors.Sub, fontSize = 14.sp)
        Shortcut(R.drawable.ic_map, "فتح الخريطة") { openMap(ctx, "") }
        Shortcut(R.drawable.ic_fuel, "أقرب محطة وقود") { openMap(ctx, "محطة وقود") }
        Shortcut(R.drawable.ic_mosque, "أقرب مسجد") { openMap(ctx, "مسجد") }
        Shortcut(R.drawable.ic_home, "البيت") { openMap(ctx, "البيت") }
    }
}

@Composable
private fun Shortcut(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x12FFFFFF))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconRes(icon, tint = DarbColors.Accent, size = 22.dp)
        Text(label, color = DarbColors.Text, fontSize = 15.sp)
    }
}

@Composable
private fun SpeedGauge(modifier: Modifier) {
    val speed by SpeedTracker.speedKmh.collectAsState()
    val frac = (speed / 180f).coerceIn(0f, 1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .aspectRatio(1f)
        ) {
            val stroke = 18.dp.toPx()
            val d = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = DarbColors.Accent,
                startAngle = 135f,
                sweepAngle = 270f * frac,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(speed.toInt().toString(), color = DarbColors.Text, fontSize = 96.sp, fontWeight = FontWeight.SemiBold)
            Text("كم/س", color = DarbColors.Sub, fontSize = 16.sp)
        }
    }
}
