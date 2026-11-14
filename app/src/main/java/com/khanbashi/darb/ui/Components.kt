package com.khanbashi.darb.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(DarbColors.Panel)
            .border(1.dp, DarbColors.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun IconRes(@DrawableRes id: Int, tint: Color = DarbColors.Text, size: Dp = 22.dp, contentDescription: String? = null) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    background: Color = Color(0x1AFFFFFF),
    tint: Color = DarbColors.Text
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconRes(icon, tint = tint, size = size * 0.45f, contentDescription = contentDescription)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) DarbColors.Accent else Color(0x14FFFFFF))
            .border(1.dp, if (selected) DarbColors.Accent else DarbColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) DarbColors.OnAccent else DarbColors.Text,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun Tile(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) DarbColors.Accent.copy(alpha = 0.18f) else Color(0x12FFFFFF))
            .border(1.dp, if (selected) DarbColors.Accent else DarbColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = DarbColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        if (subtitle != null) Text(subtitle, color = DarbColors.Sub, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = DarbColors.Sub, fontSize = 14.sp)
    }
}
