package com.example.sample.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import com.vaulet.app.R

private val SplashTop = Color(0xFFF8FAFC)
private val SplashBottom = Color(0xFFE8F1FC)
private val Title = Color(0xFF1E3258)
private val Accent = Color(0xFF1688F8)

@Composable
fun LaunchSplash(onFinished: () -> Unit) {

    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val floating by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    LaunchedEffect(Unit) {

        coroutineScope {

            listOf(
                launch {
                    logo.animateTo(
                        1f,
                        tween(1000, easing = FastOutSlowInEasing)
                    )
                },
                launch {
                    delay(650)
                    title.animateTo(
                        1f,
                        tween(800, easing = FastOutSlowInEasing)
                    )
                },
                launch {
                    delay(1300)
                    tagline.animateTo(
                        1f,
                        tween(850, easing = FastOutSlowInEasing)
                    )
                }
            ).joinAll()

            delay(1100)

            exit.animateTo(
                0f,
                tween(450, easing = FastOutSlowInEasing)
            )

            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exit.value
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        SplashTop,
                        Color(0xFFF1F6FD),
                        SplashBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Decorative background circles
        Box(
            modifier = Modifier
                .size(310.dp)
                .border(
                    1.dp,
                    Accent.copy(alpha = 0.10f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .border(
                    1.dp,
                    Accent.copy(alpha = 0.08f),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Animated logo container
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer {
                        val scale = 0.4f + 0.6f * logo.value

                        scaleX = scale * pulse
                        scaleY = scale * pulse
                        alpha = logo.value
                        translationY =
                            (1f - logo.value) * 55f + floating
                    }
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(42.dp),
                        ambientColor = Accent.copy(alpha = 0.20f),
                        spotColor = Accent.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(42.dp))
                    .background(Color(0xFFEAF3FF))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.9f),
                        RoundedCornerShape(42.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter = painterResource(
                        R.mipmap.ic_launcher_foreground
                    ),
                    contentDescription = "Vaulet logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            // App name
            Text(
                text = stringResource(R.string.app_name),
                color = Title,
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.8f + 0.2f * title.value
                    scaleY = scaleX
                    alpha = title.value
                    translationY = (1f - title.value) * 30f
                }
            )

            Spacer(Modifier.height(12.dp))

            // Decorative accent
            Box(
                modifier = Modifier
                    .width(55.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF60A5FA),
                                Accent
                            )
                        )
                    )
            )

            Spacer(Modifier.height(18.dp))

            // Tagline
            Text(
                text = stringResource(R.string.splash_tagline),
                color = Color(0xFF526B91),
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.85f + 0.15f * tagline.value
                    scaleY = scaleX
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 24f
                }
            )
        }

        // Bottom security label
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 55.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "YOUR PRIVACY. YOUR CONTROL.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color(0xFF7891B3).copy(alpha = exit.value)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Securely store what matters.",
                fontSize = 13.sp,
                color = Color(0xFF8CA1BC).copy(alpha = exit.value)
            )
        }
    }
}
