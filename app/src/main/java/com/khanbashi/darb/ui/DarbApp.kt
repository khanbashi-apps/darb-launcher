package com.khanbashi.darb.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanbashi.darb.BuildConfigInfo
import com.khanbashi.darb.R
import com.khanbashi.darb.apps.AppsScreen
import com.khanbashi.darb.home.HomeScreen
import com.khanbashi.darb.quran.QuranScreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime

enum class Screen { Home, Quran, Apps }

private val DAYS = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
private val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

@Composable
fun DarbApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = DarbColors.Accent,
                background = DarbColors.Bg,
                surface = DarbColors.PanelSolid,
                onSurface = DarbColors.Text
            )
        ) {
            var screen by rememberSaveable { mutableStateOf(Screen.Home) }
            BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(DarbColors.BgTop, DarbColors.Bg)))
            ) {
                Column(Modifier.fillMaxSize()) {
                    StatusBar()
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (screen) {
                            Screen.Home -> HomeScreen(onOpenQuran = { screen = Screen.Quran })
                            Screen.Quran -> QuranScreen()
                            Screen.Apps -> AppsScreen()
                        }
                    }
                    Dock(current = screen, onSelect = { screen = it })
                }
            }
        }
    }
}

@Composable
private fun StatusBar() {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000)
        }
    }
    val h12 = if (now.hour % 12 == 0) 12 else now.hour % 12
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$h12:${now.minute.toString().padStart(2, '0')}", color = DarbColors.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                (if (now.hour < 12) "ص" else "م") + " · " + DAYS[now.dayOfWeek.value - 1] + " " + now.dayOfMonth + " " + MONTHS[now.monthValue - 1],
                color = DarbColors.Sub,
                fontSize = 14.sp
            )
        }
        Text("درب · " + BuildConfigInfo.VERSION, color = DarbColors.Sub, fontSize = 13.sp)
    }
}

@Composable
private fun Dock(current: Screen, onSelect: (Screen) -> Unit) {
    val ctx = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(androidx.compose.ui.graphics.Color(0x0DFFFFFF)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem(R.drawable.ic_home, "الرئيسية", current == Screen.Home) { onSelect(Screen.Home) }
        DockItem(R.drawable.ic_book, "القرآن", current == Screen.Quran) { onSelect(Screen.Quran) }
        DockItem(R.drawable.ic_map, "الخرائط", false) { openMap(ctx, "") }
        DockItem(R.drawable.ic_apps, "التطبيقات", current == Screen.Apps) { onSelect(Screen.Apps) }
        DockItem(R.drawable.ic_settings, "الإعدادات", false) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun DockItem(@DrawableRes icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(76.dp)
            .height(62.dp)
            .clip(shape)
            .background(if (selected) DarbColors.Accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClickLabel = label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconRes(icon, tint = if (selected) DarbColors.Accent else DarbColors.Sub, size = 24.dp)
        Text(label, color = if (selected) DarbColors.Accent else DarbColors.Sub, fontSize = 12.sp)
    }
}
