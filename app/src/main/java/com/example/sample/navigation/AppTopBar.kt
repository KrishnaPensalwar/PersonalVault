package com.example.sample.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Configuration holder for top bar customization from any screen.
 */
data class TopBarConfig(
    val title: String? = null,
    val titleContent: (@Composable () -> Unit)? = null,
    val actions: (@Composable RowScope.() -> Unit)? = null,
    val navigationIcon: (@Composable () -> Unit)? = null,
    val onBackClick: (() -> Unit)? = null,
    val isVisible: Boolean = true,
)

/**
 * State holder that manages top bar configurations keyed by Destination.
 */
class TopBarState {
    private val configs = mutableStateMapOf<Destinations, TopBarConfig>()

    fun setConfig(destination: Destinations, config: TopBarConfig) {
        configs[destination] = config
    }

    fun removeConfig(destination: Destinations) {
        configs.remove(destination)
    }

    fun getConfig(destination: Destinations): TopBarConfig? {
        return configs[destination]
    }
}

@Composable
fun rememberTopBarState(): TopBarState {
    return remember { TopBarState() }
}

/**
 * Allows a screen to dynamically customize the centralized TopAppBar.
 */
@Composable
fun SetTopBar(
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    isVisible: Boolean = true,
) {
    val destination = LocalCurrentDestination.current ?: return
    val topBarState = LocalTopBarState.current

    val config = TopBarConfig(
        title = title,
        titleContent = titleContent,
        actions = actions,
        navigationIcon = navigationIcon,
        onBackClick = onBackClick,
        isVisible = isVisible,
    )

    SideEffect {
        topBarState.setConfig(destination, config)
    }

    DisposableEffect(destination) {
        onDispose {
            topBarState.removeConfig(destination)
        }
    }
}

/**
 * Centralized TopAppBar rendered inside the root Scaffold in AppNavigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentDestination: Destinations,
    topBarState: TopBarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = topBarState.getConfig(currentDestination)
    if (config?.isVisible == false) return

    val isRootScreen = currentDestination.showBottomBar
    val titleText = config?.title ?: currentDestination.defaultTitle

    TopAppBar(
        modifier = modifier,
        title = {
            if (config?.titleContent != null) {
                config.titleContent.invoke()
            } else {
                Text(
                    text = titleText,
                    style = if (isRootScreen) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (config?.navigationIcon != null) {
                config.navigationIcon.invoke()
            } else if (!isRootScreen) {
                IconButton(onClick = {
                    if (config?.onBackClick != null) {
                        config.onBackClick.invoke()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = {
            config?.actions?.invoke(this)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
