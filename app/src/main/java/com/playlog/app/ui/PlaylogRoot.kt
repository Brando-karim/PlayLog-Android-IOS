package com.playlog.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.ui.components.AmbientBackground
import com.playlog.app.ui.screens.DiaryScreen
import com.playlog.app.ui.screens.DiscoverScreen
import com.playlog.app.ui.screens.GameDetailScreen
import com.playlog.app.ui.screens.HomeScreen
import com.playlog.app.ui.screens.LibraryScreen
import com.playlog.app.ui.screens.ListDetailScreen
import com.playlog.app.ui.screens.ListsScreen
import com.playlog.app.ui.screens.LogScreen
import com.playlog.app.ui.screens.ProfileScreen
import com.playlog.app.ui.screens.SearchScreen
import com.playlog.app.ui.screens.SettingsScreen
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassFill
import com.playlog.app.ui.theme.RimBrush
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal

private data class Tab(
    val route: String,
    val label: String,
    @param:DrawableRes val icon: Int,
)

private val tabs = listOf(
    Tab("home", "Home", R.drawable.ic_home),
    Tab("discover", "Discover", R.drawable.ic_compass),
    Tab("library", "Library", R.drawable.ic_books),
    Tab("lists", "Lists", R.drawable.ic_list),
    Tab("profile", "Profile", R.drawable.ic_user),
)

@Composable
fun PlaylogRoot(vm: PlaylogViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.route }

    Box(Modifier.fillMaxSize()) {
        AmbientBackground()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBar) {
                    FloatingNav(
                        route = route,
                        onSelect = { tab ->
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .then(if (showBar) Modifier else Modifier.navigationBarsPadding()),
                enterTransition = {
                    fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 16 }
                },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = {
                    fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 16 }
                },
                popExitTransition = {
                    fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { it / 16 }
                },
            ) {
                composable("home") {
                    HomeScreen(
                        vm = vm,
                        onGame = { nav.navigate("game/$it") },
                        onSeeAll = { nav.navigate("discover") },
                        onDiary = { nav.navigate("diary") },
                        onSettings = { nav.navigate("settings") },
                    )
                }

                composable("discover") {
                    DiscoverScreen(
                        vm = vm,
                        onGame = { nav.navigate("game/$it") },
                        onSearch = { nav.navigate("search") },
                    )
                }

                composable(
                    route = "search?listId={listId}",
                    arguments = listOf(
                        navArgument("listId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    SearchScreen(
                        vm = vm,
                        initialListId = entry.arguments?.getString("listId"),
                        onBack = { nav.popBackStack() },
                        onGame = { nav.navigate("game/$it") },
                        onAddedToList = { nav.popBackStack() },
                    )
                }

                composable(
                    route = "game/{gameId}",
                    arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("gameId") ?: 0L
                    GameDetailScreen(
                        vm = vm,
                        gameId = id,
                        onBack = { nav.popBackStack() },
                        onLog = { nav.navigate("log/$id") },
                    )
                }

                composable(
                    route = "log/{gameId}",
                    arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("gameId") ?: 0L
                    LogScreen(
                        vm = vm,
                        gameId = id,
                        onBack = { nav.popBackStack() },
                        onSaved = { nav.popBackStack() },
                    )
                }

                composable("library") {
                    LibraryScreen(
                        vm = vm,
                        onGame = { nav.navigate("game/$it") },
                        onDiscover = { nav.navigate("discover") },
                    )
                }

                composable("diary") {
                    DiaryScreen(
                        vm = vm,
                        onBack = { nav.popBackStack() },
                        onGame = { nav.navigate("game/$it") },
                    )
                }

                composable("lists") {
                    ListsScreen(vm = vm, onList = { nav.navigate("list/$it") })
                }

                composable(
                    route = "list/{listId}",
                    arguments = listOf(navArgument("listId") { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString("listId").orEmpty()
                    ListDetailScreen(
                        vm = vm,
                        listId = id,
                        onBack = { nav.popBackStack() },
                        onAddGames = { nav.navigate("search?listId=$it") },
                        onGame = { nav.navigate("game/$it") },
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        vm = vm,
                        onGame = { nav.navigate("game/$it") },
                        onDiary = { nav.navigate("diary") },
                    )
                }

                composable("settings") {
                    SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun FloatingNav(
    route: String?,
    onSelect: (Tab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(66.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(33.dp),
                ambientColor = Teal.copy(alpha = 0.28f),
                spotColor = Accent.copy(alpha = 0.22f),
            )
            .clip(RoundedCornerShape(33.dp))
            .background(GlassFill)
            .border(1.dp, RimBrush, RoundedCornerShape(33.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = route == tab.route
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.90f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.55f,
                    stiffness = 600f,
                ),
                label = "nav-scale",
            )
            val shape = RoundedCornerShape(27.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(if (selected) Accent.copy(alpha = 0.11f) else Color.Transparent)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, Accent.copy(alpha = 0.18f), shape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(tab.icon),
                    contentDescription = tab.label,
                    tint = if (selected) Accent else Sage.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tab.label,
                    color = if (selected) Accent else Sage.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    letterSpacing = 0.3.sp,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
