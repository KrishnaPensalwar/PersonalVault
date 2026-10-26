package com.example.sample.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.sample.dashboard.DashboardScreen
import com.example.sample.family.FamilyMemberProfileScreen
import com.example.sample.family.FamilyScreen
import com.example.sample.password.PasswordDetailScreen
import com.example.sample.password.PasswordFormScreen
import com.example.sample.password.PasswordScreen
import com.example.sample.storage.DocumentDetailScreen
import com.example.sample.storage.DocumentViewerScreen
import com.example.sample.storage.StorageScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Destinations.Dashboard)
    val currentDestination = backStack.last() as Destinations

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination.showBottomBar) {
                BottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        if (currentDestination != destination) {
                            backStack.add(destination)
                        }
                    }
                )
            }
        },
    ) { innerPadding ->

        CompositionLocalProvider(
            LocalBackStack provides backStack,
        ) {
            NavDisplay(
                backStack = backStack,
                modifier = modifier.padding(innerPadding),
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryProvider = entryProvider {

                    entry<Destinations.Dashboard> {
                        DashboardScreen()
                    }

                    entry<Destinations.Password> {
                        PasswordScreen()
                    }

                    entry<Destinations.Storage> {
                        StorageScreen()
                    }

                    entry<Destinations.Family> {
                        FamilyScreen()
                    }

                    entry<Destinations.FamilyMemberProfile> { dest ->
                        FamilyMemberProfileScreen(memberId = dest.memberId)
                    }

                    entry<Destinations.DocumentDetailScreen> { document ->
                        DocumentDetailScreen(id = document.id)
                    }

                    entry<Destinations.DocumentViewer> { document ->
                        DocumentViewerScreen(id = document.id)
                    }

                    // ── Password sub-screens ──────────────────────────────────
                    entry<Destinations.PasswordForm> { dest ->
                        PasswordFormScreen(passwordId = dest.passwordId)
                    }

                    entry<Destinations.PasswordDetail> { dest ->
                        PasswordDetailScreen(passwordId = dest.passwordId)
                    }
                },
            )
        }
    }
}

@Composable
fun BottomBar(
    currentDestination: Destinations,
    onDestinationSelected: (Destinations) -> Unit,
) {
    val items = listOf(
        BottomNavItem(Destinations.Dashboard, "Dashboard", Icons.Default.Home),
        BottomNavItem(Destinations.Storage, "Storage", Icons.Default.Storage),
        BottomNavItem(Destinations.Password, "Password", Icons.Default.Password),
        BottomNavItem(Destinations.Family, "Family", Icons.Default.FamilyRestroom),
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentDestination == item.destination,
                onClick = { onDestinationSelected(item.destination) },
                icon = {
                    Image(imageVector = item.icon, contentDescription = item.label)
                },
                label = { Text(item.label) },
            )
        }
    }
}

data class BottomNavItem(
    val destination: Destinations,
    val label: String,
    val icon: ImageVector,
)
