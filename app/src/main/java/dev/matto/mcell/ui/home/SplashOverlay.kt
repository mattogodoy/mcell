package dev.matto.mcell.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.matto.mcell.R
import dev.matto.mcell.ui.theme.Bg
import kotlinx.coroutines.delay

private const val SPLASH_HOLD_MS = 1200L
private const val SPLASH_FADE_MS = 350

/**
 * Renders a fullscreen splash on top of the host content for [SPLASH_HOLD_MS], then fades out.
 * Place inside a `Box(Modifier.fillMaxSize())` after the actual content so it stacks on top.
 */
@Composable
fun SplashOverlay() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SPLASH_HOLD_MS)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(0)),
        exit = fadeOut(animationSpec = tween(SPLASH_FADE_MS)),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            Image(
                painter = painterResource(R.drawable.splash),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
