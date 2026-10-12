package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModelSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WritingAssistantScreen
import com.example.ui.theme.ElectricBlue

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("হোম", Icons.Default.Home),
    CHAT("চ্যাট", Icons.Default.ChatBubbleOutline),
    WRITING("টুলস", Icons.Default.AutoAwesome),
    MODELS("মডেল", Icons.Default.Memory),
    SETTINGS("সেটিংস", Icons.Default.Settings),
    ABOUT("পরিচিতি", Icons.Default.Info)
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { currentScreen = Screen.CHAT },
                    onNavigateToWritingTools = { currentScreen = Screen.WRITING },
                    onNavigateToModels = { currentScreen = Screen.MODELS },
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                    onNavigateToAbout = { currentScreen = Screen.ABOUT }
                )
                Screen.CHAT -> ChatScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.HOME }
                )
                Screen.WRITING -> WritingAssistantScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { currentScreen = Screen.CHAT }
                )
                Screen.MODELS -> ModelSetupScreen(
                    viewModel = viewModel
                )
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToModels = { currentScreen = Screen.MODELS }
                )
                Screen.ABOUT -> AboutScreen()
            }
        }
    }
}
