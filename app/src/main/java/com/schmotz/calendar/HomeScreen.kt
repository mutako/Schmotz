package com.schmotz.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: FirestoreRepository,
    profile: UserProfile,
    onSignOut: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Calendar", "Upcoming", "Links", "Search")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schmotz") },
                navigationIcon = { AppLogo() },
                actions = {
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                onClick = {
                                    showSettings = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> CalendarScreen(
                    repo = repo,
                    profile = profile
                )
                1 -> UpcomingScreen(
                    repo = repo,
                    profile = profile
                )
                2 -> LinksScreen(repo = repo, profile = profile)
                else -> SearchScreen(repo = repo, profile = profile)
            }
        }
    }
}

private const val SCHMOTZ_LOGO_URL = "https://instagram.fvie2-1.fna.fbcdn.net/v/t51.2885-19/16583500_1306233692797649_222891476764327936_a.jpg?stp=dst-jpg_s150x150_tt6&efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLmRqYW5nby4xOTYuYzIifQ&_nc_ht=instagram.fvie2-1.fna.fbcdn.net&_nc_cat=105&_nc_oc=Q6cZ2QFS_2_pVTDpVTmD1SvVGLhENCx2eOupCu34Ne3WRFNGyLO9bWxn9U0rgEux5giSDAA&_nc_ohc=VKQo6AYqJFoQ7kNvwFiGkZh&_nc_gid=YJKxfJoN2o0APJ9ogyhA3g&edm=ACE-g0gBAAAA&ccb=7-5&oh=00_Afa70ktRc_lTxMeYe6atAYxS0LX_KcLmIZ_Oa8zxiwHqwA&oe=68DC2E79&_nc_sid=b15361"

@Composable
private fun AppLogo() {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(SCHMOTZ_LOGO_URL)
            .crossfade(true)
            .build(),
        contentDescription = "Schmotz logo",
        modifier = Modifier
            .padding(start = 16.dp)
            .size(40.dp)
            .clip(CircleShape),
        loading = { LogoFallback() },
        error = { LogoFallback() }
    )
}

@Composable
private fun LogoFallback() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "S",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
