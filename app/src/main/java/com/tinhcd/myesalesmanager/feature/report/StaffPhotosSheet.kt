package com.tinhcd.myesalesmanager.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.SkeletonList
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime

/**
 * Ảnh trong ngày: what one rep photographed, in the order the day ran — the
 * punch that opened it, each outlet's shop front at check-in and at check-out,
 * the punch that closed it. Opened on one outlet, it shows only that visit.
 *
 * A photo the rep did not take is said to be missing rather than left out: a
 * supervisor checking a visit wants to know it has no picture, not wonder.
 */
@Composable
fun StaffPhotosSheet(sheet: PhotoSheet, day: String, onClose: () -> Unit) {
    var zoomed by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SubScreen(
            title = "Ảnh trong ngày",
            subtitle = "${sheet.name} · $day",
            onBack = onClose,
        ) { padding ->
            val data = sheet.data
            when {
                sheet.loading -> SkeletonList(Modifier.padding(padding), count = 3, lines = 3)
                sheet.error != null -> Box(Modifier.padding(padding)) { ErrorBox(sheet.error) }
                data != null -> {
                    val visits = data.visits.filter { sheet.customerCode == null || it.customerCode == sheet.customerCode }
                    val punches = if (sheet.customerCode == null) data.punches else emptyList()
                    val opening = punches.filter { it.type == "check_in" }
                    val closing = punches.filter { it.type != "check_in" }
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (visits.isEmpty() && punches.isEmpty()) {
                            item { Faded("Không có chấm công hay lượt ghé nào trong ngày") }
                        }
                        items(opening) { p ->
                            PunchCard(p.label, time(p.at), p.url) { zoomed = it }
                        }
                        items(visits) { v ->
                            PhotoCard(
                                "${v.customerCode} – ${v.customerName}",
                                listOfNotNull(time(v.checkInAt), time(v.checkOutAt)).joinToString(" – "),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PhotoTile("Check-in", v.checkInUrl, Modifier.weight(1f), onOpen = { zoomed = it })
                                    PhotoTile(
                                        "Check-out",
                                        v.checkOutUrl,
                                        Modifier.weight(1f),
                                        missing = if (v.checkOutAt == null) "Chưa check-out" else "Không có ảnh",
                                        onOpen = { zoomed = it },
                                    )
                                }
                            }
                        }
                        items(closing) { p ->
                            PunchCard(p.label, time(p.at), p.url) { zoomed = it }
                        }
                    }
                }
            }
        }
    }

    zoomed?.let { url -> ZoomedPhoto(url, onClose = { zoomed = null }) }
}

/** Half the width, as one of a visit's pair is: a punch photo needs no more room. */
@Composable
private fun PunchCard(title: String, at: String?, url: String?, onOpen: (String) -> Unit) {
    PhotoCard(title, at) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoTile(null, url, Modifier.weight(1f), onOpen = onOpen)
            Spacer(Modifier.weight(1f))
        }
    }
}

private fun time(iso: String?): String? = iso?.let { formatDateTime(it).takeLast(5) }

@Composable
private fun PhotoCard(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            subtitle?.takeIf { it.isNotBlank() }?.let { Faded(it) }
            content()
        }
    }
}

/** A 4:3 thumbnail with its caption; tapped, it opens full size. */
@Composable
private fun PhotoTile(
    caption: String?,
    url: String?,
    modifier: Modifier = Modifier,
    missing: String = "Không có ảnh",
    onOpen: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val shape = RoundedCornerShape(10.dp)
        if (url == null) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { Faded(missing) }
        } else {
            AsyncImage(
                model = url,
                contentDescription = caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(shape).clickable { onOpen(url) },
            )
        }
        caption?.let { Faded(it) }
    }
}

@Composable
private fun ZoomedPhoto(url: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onClose)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Faded(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
