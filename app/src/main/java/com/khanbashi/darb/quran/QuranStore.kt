package com.khanbashi.darb.quran

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Loads the catalogue once and keeps it in memory while the launcher runs. */
object QuranStore {
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters
    private val _suwar = MutableStateFlow<List<Surah>>(emptyList())
    val suwar: StateFlow<List<Surah>> = _suwar
    private val _radios = MutableStateFlow<List<Radio>>(emptyList())
    val radios: StateFlow<List<Radio>> = _radios
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadIfNeeded(force: Boolean = false) {
        if (_loading.value) return
        if (!force && _reciters.value.isNotEmpty() && _suwar.value.isNotEmpty()) return
        _loading.value = true
        _error.value = null
        try {
            _suwar.value = QuranApi.suwar()
            _reciters.value = QuranApi.reciters()
            _radios.value = QuranApi.radios()
        } catch (e: Exception) {
            _error.value = "تعذر تحميل المكتبة. تحقق من اتصال الإنترنت ثم أعد المحاولة."
        } finally {
            _loading.value = false
        }
    }

    fun surahName(id: Int): String =
        _suwar.value.firstOrNull { it.id == id }?.name ?: "سورة $id"
}
