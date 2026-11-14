package com.khanbashi.darb.quran

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Moshaf(val id: Int, val name: String, val server: String, val surahs: List<Int>)
data class Reciter(val id: Int, val name: String, val letter: String, val moshaf: List<Moshaf>)
data class Surah(val id: Int, val name: String, val makkia: Boolean)
data class Radio(val id: Int, val name: String, val url: String, val category: String)

/**
 * Public API of mp3quran.net (v3): reciters, surahs and radio streams.
 * Docs: https://mp3quran.net/ar/api
 */
object QuranApi {
    private const val BASE = "https://www.mp3quran.net/api/v3"

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL("$BASE/$path").openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 25_000
        conn.setRequestProperty("Accept", "application/json")
        try {
            conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun reciters(): List<Reciter> {
        val arr = get("reciters?language=ar").getJSONArray("reciters")
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            val ms = r.optJSONArray("moshaf")
            val moshaf = if (ms == null) emptyList() else (0 until ms.length()).map { j ->
                val m = ms.getJSONObject(j)
                Moshaf(
                    id = m.optInt("id"),
                    name = m.optString("name"),
                    server = m.optString("server"),
                    surahs = m.optString("surah_list")
                        .split(',')
                        .mapNotNull { it.trim().toIntOrNull() }
                )
            }
            Reciter(r.optInt("id"), r.optString("name").trim(), r.optString("letter"), moshaf)
        }.filter { it.moshaf.isNotEmpty() }.sortedBy { it.name }
    }

    suspend fun suwar(): List<Surah> {
        val arr = get("suwar?language=ar").getJSONArray("suwar")
        return (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            Surah(s.optInt("id"), s.optString("name").trim(), s.optInt("makkia") == 1)
        }.sortedBy { it.id }
    }

    suspend fun radios(): List<Radio> {
        val arr = get("radios?language=ar").getJSONArray("radios")
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            val name = r.optString("name").trim()
            Radio(r.optInt("id"), name, r.optString("url"), RadioCategories.of(name))
        }.filter { it.url.isNotBlank() }
    }

    fun surahUrl(m: Moshaf, surah: Int): String =
        m.server.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
}

/** Groups radio stations into the sections the user asked for (same order as Atheer radio library). */
object RadioCategories {
    const val ALL = "الكل"
    const val RECITERS = "القراء"

    private val rules: List<Pair<String, List<String>>> = listOf(
        "إذاعة القرآن الكريم - السعودية" to listOf("السعودية", "المملكة العربية"),
        "التفسير وعلوم القرآن" to listOf("تفسير", "التفسير", "علوم القرآن"),
        "ترجمة معاني القرآن الكريم" to listOf("ترجمة", "معاني"),
        "السيرة والقصص" to listOf("سيرة", "السيرة", "قصص", "قصة"),
        "الرقية الشرعية" to listOf("رقية", "الرقية"),
        "الفتاوى" to listOf("فتاوى", "فتوى", "الفتاوى"),
        "الأدعية والأذكار" to listOf("أذكار", "الأذكار", "اذكار", "أدعية", "الأدعية", "دعاء"),
        "القراءات العشر" to listOf("القراءات", "قراءة", "رواية"),
        "مواسم الخير" to listOf("رمضان", "الحج", "ذي الحجة", "عرفة", "التراويح", "مواسم"),
        "السنة النبوية" to listOf("السنة", "حديث", "الحديث", "نبوية", "صحيح"),
        "تلاوات متميزة" to listOf("تلاوات", "خاشعة", "مختارة", "متميزة", "تلاوة")
    )

    val ordered: List<String> = listOf(ALL, RECITERS) + listOf(
        "التفسير وعلوم القرآن",
        "ترجمة معاني القرآن الكريم",
        "السيرة والقصص",
        "تلاوات متميزة",
        "الرقية الشرعية",
        "الفتاوى",
        "الأدعية والأذكار",
        "القراءات العشر",
        "مواسم الخير",
        "السنة النبوية",
        "إذاعة القرآن الكريم - السعودية"
    )

    fun of(name: String): String {
        for ((cat, words) in rules) if (words.any { name.contains(it) }) return cat
        return RECITERS
    }
}
