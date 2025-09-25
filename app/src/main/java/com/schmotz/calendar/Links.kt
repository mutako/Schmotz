package com.schmotz.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LinksScreen(
    repo: FirestoreRepository,
    profile: UserProfile
) {
    val links by repo.observeLinks(profile).collectAsState(initial = emptyList())

    if (links.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Shared links will appear here once someone posts one.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(links) { link ->
                LinkCard(link)
            }
        }
    }
}

@Composable
fun LinkCard(link: SharedLink) {
    val uriHandler = LocalUriHandler.current
    val targetUrl = remember(link.url) {
        val trimmed = link.url.trim()
        when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.isBlank() -> trimmed
            else -> "https://$trimmed"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (targetUrl.isNotBlank()) {
                runCatching { uriHandler.openUri(targetUrl) }
            }
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp)) {
            LinkThumbnail(link)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = link.title.ifBlank { link.url },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = link.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                link.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Shared by ${link.sharedByName.ifBlank { "Someone" }} on ${formatSharedAt(link.sharedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinkThumbnail(link: SharedLink) {
    val shape = RoundedCornerShape(12.dp)

    if (!link.imageUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(link.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = link.title,
            modifier = Modifier
                .size(64.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
            loading = { LinkThumbnailPlaceholder() },
            error = { LinkThumbnailPlaceholder() }
        )
    } else {
        LinkThumbnailPlaceholder()
    }
}

@Composable
private fun LinkThumbnailPlaceholder() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatSharedAt(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
