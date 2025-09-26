package com.schmotz.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinksScreen(
    repo: FirestoreRepository,
    profile: UserProfile
) {
    val links by repo.observeLinks(profile).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val categories = remember(links) {
        links.mapNotNull { link -> link.category.takeIf { it.isNotBlank() } }
            .distinct()
            .sorted()
    }

    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var categoryDialogFor by remember { mutableStateOf<SharedLink?>(null) }
    var commentDialogFor by remember { mutableStateOf<SharedLink?>(null) }

    val filteredLinks = remember(links, selectedCategory) {
        if (selectedCategory == "All") links
        else links.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        if (links.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                Text("Shared links will appear here once someone posts one.")
            }
        } else {
            CategorySelectorRow(
                categories = categories,
                selected = selectedCategory,
                onSelected = { selectedCategory = it }
            )

            if (filteredLinks.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                    Text("No links in this category yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLinks, key = { it.id }) { link ->
                        LinkCard(
                            link = link,
                            onEditCategory = { categoryDialogFor = it },
                            onOpenComments = { commentDialogFor = it }
                        )
                    }
                }
            }
        }
    }

    categoryDialogFor?.let { pending ->
        val latest = links.firstOrNull { it.id == pending.id } ?: pending
        LinkCategoryDialog(
            link = latest,
            categories = categories,
            onDismiss = { categoryDialogFor = null },
            onSave = { category ->
                scope.launch {
                    repo.updateLinkCategory(profile, latest.id, category)
                }
            }
        )
    }

    commentDialogFor?.let { pending ->
        val latest = links.firstOrNull { it.id == pending.id } ?: pending
        LinkCommentsDialog(
            link = latest,
            onDismiss = { commentDialogFor = null },
            onSubmit = { message ->
                scope.launch {
                    repo.addLinkComment(
                        profile = profile,
                        linkId = latest.id,
                        comment = LinkComment(
                            authorUid = profile.uid,
                            authorName = profile.displayName.ifBlank { "Someone" },
                            message = message
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun LinkCard(
    link: SharedLink,
    onEditCategory: ((SharedLink) -> Unit)? = null,
    onOpenComments: ((SharedLink) -> Unit)? = null
) {
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
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
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

            if (link.comments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Recent comments",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                link.comments.sortedBy { it.createdAt }
                    .takeLast(2)
                    .forEach { comment ->
                        CommentRow(comment)
                        Spacer(Modifier.height(8.dp))
                    }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    onEditCategory != null -> {
                        AssistChip(
                            onClick = { onEditCategory(link) },
                            label = {
                                Text(if (link.category.isBlank()) "Add category" else link.category)
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Label, contentDescription = null)
                            }
                        )
                    }
                    link.category.isNotBlank() -> {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(link.category) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Label, contentDescription = null)
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (onOpenComments != null) {
                    TextButton(onClick = { onOpenComments(link) }) {
                        Icon(imageVector = Icons.Filled.Comment, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Comments (${link.comments.size})")
                    }
                } else if (link.comments.isNotEmpty()) {
                    Text(
                        "${link.comments.size} comments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelectorRow(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val allOptions = listOf("All") + categories
            allOptions.forEach { option ->
                FilterChip(
                    selected = selected.equals(option, ignoreCase = true),
                    onClick = { onSelected(option) },
                    label = { Text(option) }
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
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

@Composable
private fun CommentRow(comment: LinkComment) {
    Column {
        Text(
            text = comment.authorName.ifBlank { "Someone" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = comment.message,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatSharedAt(comment.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkCategoryDialog(
    link: SharedLink,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(link.id) { mutableStateOf(link.category) }
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose category") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Quick pick", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            AssistChip(
                                onClick = { text = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(trimmed)
                    onDismiss()
                },
                enabled = trimmed != link.category
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LinkCommentsDialog(
    link: SharedLink,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var commentText by remember(link.id) { mutableStateOf("") }
    val sortedComments = remember(link.comments) {
        link.comments.sortedBy { it.createdAt }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments") },
        text = {
            Column {
                if (sortedComments.isEmpty()) {
                    Text("No comments yet. Be the first to share a thought!")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedComments, key = { it.id }) { comment ->
                            CommentRow(comment)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add a comment") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(commentText)
                    commentText = ""
                },
                enabled = commentText.isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
