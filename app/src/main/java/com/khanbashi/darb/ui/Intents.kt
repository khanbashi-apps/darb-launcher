package com.khanbashi.darb.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens the user's map app (Google Maps, Waze, ...) with an optional search query. */
fun openMap(ctx: Context, query: String) {
    val uri = if (query.isBlank()) Uri.parse("geo:0,0") else Uri.parse("geo:0,0?q=" + Uri.encode(query))
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(ctx, "لا يوجد تطبيق خرائط على الجهاز", Toast.LENGTH_SHORT).show()
    }
}
