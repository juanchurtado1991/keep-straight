package com.keepstraight.desktop.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.keepstraight.desktop.navigation.HomeScreenRoute

@Composable
fun DesktopApp(
    onQuit: () -> Unit,
    onHideToTray: (() -> Unit)?,
) {
    Navigator(HomeScreenRoute(onHideToTray, onQuit)) { navigator ->
        SlideTransition(navigator)
    }
}
