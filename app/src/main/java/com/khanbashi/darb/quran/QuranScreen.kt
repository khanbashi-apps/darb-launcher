package com.khanbashi.darb.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanbashi.darb.R
import com.khanbashi.darb.ui.Chip
import com.khanbashi.darb.ui.DarbColors
import com.khanbashi.darb.ui.RoundIconButton
import com.khanbashi.darb.ui.Tile
import kotlinx.coroutines.launch

private val TABS = listOf("القراء", "السور", "الإذاعات")

@Composable
fun QuranScreen() {
    val scope = rememberCoroutineScope()
    val reciters by QuranStore.reciters.collectAsState()
    val suwar by QuranStore.suwar.collectAsState()
    val radios by QuranStore.radios.collectAsState()
    val loading by QuranStore.loading.collectAsState()
    val error by QuranStore.error.collectAsState()

    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var reciterId by rememberSaveable { mutableStateOf<Int?>(null) }
    var moshafId by rememberSaveable { mutableStateOf<Int?>(null) }
    var surahId by rememberSaveable { mutableStateOf<Int?>(null) }
    var radioCat by rememberSaveable { mutableStateOf(RadioCategories.ALL) }

    LaunchedEffect(Unit) { QuranStore.loadIfNeeded() }

    val nested = (tab == 0 && reciterId != null) || (tab == 1 && surahId != null)
    BackHandler(enabled = nested) {
        if (tab == 0) {
            if (moshafId != null && (reciters.firstOrNull { it.id == reciterId }?.moshaf?.size ?: 0) > 1) moshafId = null
            else { reciterId = null; moshafId = null }
        } else surahId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TABS.forEachIndexed { i, t ->
                Chip(t, tab == i) {
                    tab = i
                    query = ""
                }
            }
            Box(Modifier.weight(1f))
            if (tab != 2) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (tab == 0) "ابحث عن قارئ" else "ابحث عن سورة") },
                    singleLine = true,
                    modifier = Modifier.width(280.dp)
                )
            }
            RoundIconButton(R.drawable.ic_refresh, "تحديث المكتبة", { scope.launch { QuranStore.loadIfNeeded(force = true) } })
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading && reciters.isEmpty() -> Centered { CircularProgressIndicator(color = DarbColors.Accent) }
                error != null && reciters.isEmpty() -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error ?: "", color = DarbColors.Text, fontSize = 16.sp)
                        Chip("إعادة المحاولة", true) { scope.launch { QuranStore.loadIfNeeded(force = true) } }
                    }
                }
                tab == 0 -> RecitersTab(
                    reciters = reciters,
                    query = query,
                    reciterId = reciterId,
                    moshafId = moshafId,
                    onReciter = { r ->
                        reciterId = r.id
                        moshafId = if (r.moshaf.size == 1) r.moshaf[0].id else null
                    },
                    onMoshaf = { moshafId = it },
                    onBack = { reciterId = null; moshafId = null }
                )
                tab == 1 -> SuwarTab(
                    suwar = suwar,
                    reciters = reciters,
                    query = query,
                    surahId = surahId,
                    onSurah = { surahId = it },
                    onBack = { surahId = null }
                )
                else -> RadiosTab(radios, radioCat) { radioCat = it }
            }
        }
        MiniPlayer()
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun Header(title: String, subtitle: String?, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundIconButton(R.drawable.ic_back, "رجوع", onBack)
        Column(Modifier.weight(1f)) {
            Text(title, color = DarbColors.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = DarbColors.Sub, fontSize = 13.sp)
        }
        action?.invoke()
    }
}

@Composable
private fun RecitersTab(
    reciters: List<Reciter>,
    query: String,
    reciterId: Int?,
    moshafId: Int?,
    onReciter: (Reciter) -> Unit,
    onMoshaf: (Int) -> Unit,
    onBack: () -> Unit
) {
    val reciter = reciters.firstOrNull { it.id == reciterId }
    if (reciter == null) {
        val shown = if (query.isBlank()) reciters else reciters.filter { it.name.contains(query.trim()) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(210.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(shown, key = { it.id }) { r ->
                Tile(
                    title = r.name,
                    subtitle = if (r.moshaf.size > 1) "${r.moshaf.size} روايات" else r.moshaf.first().name
                ) { onReciter(r) }
            }
        }
        return
    }
    val moshaf = reciter.moshaf.firstOrNull { it.id == moshafId }
    if (moshaf == null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header(reciter.name, "اختر الرواية", onBack)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reciter.moshaf, key = { it.id }) { m ->
                    Tile(m.name, "${m.surahs.size} سورة") { onMoshaf(m.id) }
                }
            }
        }
        return
    }
    val list = moshaf.surahs.sorted()
    val queue = list.map { QuranApi.surahUrl(moshaf, it) to ("سورة " + QuranStore.surahName(it)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header(reciter.name, moshaf.name, onBack) {
            Chip("تشغيل الكل", true) { QuranPlayer.playQueue(queue, 0, reciter.name) }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(list.size) { i ->
                val id = list[i]
                Tile("$id · ${QuranStore.surahName(id)}") { QuranPlayer.playQueue(queue, i, reciter.name) }
            }
        }
    }
}

@Composable
private fun SuwarTab(
    suwar: List<Surah>,
    reciters: List<Reciter>,
    query: String,
    surahId: Int?,
    onSurah: (Int) -> Unit,
    onBack: () -> Unit
) {
    if (surahId == null) {
        val shown = if (query.isBlank()) suwar else suwar.filter { it.name.contains(query.trim()) || it.id.toString() == query.trim() }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(shown, key = { it.id }) { s ->
                Tile("${s.id} · ${s.name}", if (s.makkia) "مكية" else "مدنية") { onSurah(s.id) }
            }
        }
        return
    }
    val name = QuranStore.surahName(surahId)
    val options = reciters.flatMap { r -> r.moshaf.filter { surahId in it.surahs }.map { r to it } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header("سورة $name", "اختر القارئ · ${options.size} تلاوة", onBack)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(options.size) { i ->
                val (r, m) = options[i]
                Tile(r.name, m.name) {
                    val list = m.surahs.sorted()
                    val queue = list.map { QuranApi.surahUrl(m, it) to ("سورة " + QuranStore.surahName(it)) }
                    QuranPlayer.playQueue(queue, list.indexOf(surahId).coerceAtLeast(0), r.name)
                }
            }
        }
    }
}

@Composable
private fun RadiosTab(radios: List<Radio>, cat: String, onCat: (String) -> Unit) {
    val available = RadioCategories.ordered.filter { c -> c == RadioCategories.ALL || radios.any { it.category == c } }
    val shown = if (cat == RadioCategories.ALL) radios else radios.filter { it.category == cat }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(available) { c -> Chip(c, c == cat) { onCat(c) } }
        }
        if (shown.isEmpty()) {
            Centered { Text("لا توجد إذاعات في هذا القسم حالياً", color = DarbColors.Sub) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(shown, key = { it.id }) { r ->
                    Tile(r.name, r.category) { QuranPlayer.playLive(r.url, r.name) }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer() {
    val title by QuranPlayer.title.collectAsState()
    val subtitle by QuranPlayer.subtitle.collectAsState()
    val playing by QuranPlayer.playing.collectAsState()
    val live by QuranPlayer.live.collectAsState()
    val err by QuranPlayer.error.collectAsState()
    if (title == null) return
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(bottom = 6.dp)
            .clip(shape)
            .background(Color(0x1FFFFFFF))
            .border(1.dp, DarbColors.Line, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoundIconButton(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play,
            if (playing) "إيقاف مؤقت" else "تشغيل",
            { QuranPlayer.toggle() },
            size = 52.dp,
            background = DarbColors.Accent,
            tint = DarbColors.OnAccent
        )
        if (!live) {
            RoundIconButton(R.drawable.ic_next, "التالي", { QuranPlayer.next() })
            RoundIconButton(R.drawable.ic_prev, "السابق", { QuranPlayer.previous() })
        }
        Column(Modifier.weight(1f)) {
            Text(title ?: "", color = DarbColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(err ?: (subtitle ?: ""), color = if (err != null) DarbColors.Live else DarbColors.Sub, fontSize = 13.sp, maxLines = 1)
        }
        RoundIconButton(R.drawable.ic_stop, "إيقاف", { QuranPlayer.stop() })
    }
}

@Suppress("unused")
private fun Modifier.tap(onClick: () -> Unit) = this.clickable(onClick = onClick)
