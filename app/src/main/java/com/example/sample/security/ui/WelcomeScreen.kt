package com.example.sample.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaulet.app.R
import com.example.sample.navigation.AppTopBar
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.rememberTopBarState
import com.example.sample.settings.PrivacyScreen
import com.example.sample.settings.TermsScreen

private val WelcomeBlue = Color(0xFF1D4ED8)
private val WelcomeTitle = Color(0xFF0B1F44)

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var legal by remember { mutableStateOf<Destinations?>(null) }
    val page = legal
    if (page != null) {
        BackHandler { legal = null }
        val topBarState = rememberTopBarState()
        Scaffold(
            topBar = {
                AppTopBar(
                    currentDestination = page,
                    topBarState = topBarState,
                    onBack = { legal = null },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Destinations.Terms -> TermsScreen()
                    else -> PrivacyScreen()
                }
            }
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFEAF2FF)))),
    ) {
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopStart)
                .background(Brush.radialGradient(listOf(Color(0xFFD6E6FF), Color.Transparent))),
        )
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .background(Brush.radialGradient(listOf(Color(0xFFD7E7FF), Color.Transparent))),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(R.drawable.ic_vault_shield),
                contentDescription = null,
                modifier = Modifier.size(168.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Vaulet",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = WelcomeTitle,
                fontFamily = FontFamily.Cursive
            )
            Text(
                "Your records stay on this phone.",
                color = WelcomeBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(56.dp).height(1.dp).background(Color(0xFFD5DEEE)))
                Box(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE7F0FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Security, null, tint = WelcomeBlue, modifier = Modifier.size(14.dp))
                }
                Box(Modifier.width(56.dp).height(1.dp).background(Color(0xFFD5DEEE)))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Passwords, family details, and documents are encrypted on this device. Nothing in the vault is uploaded.",
                color = Color(0xFF5C6578),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(18.dp))
            WelcomeCard(Icons.Default.Lock, "Encrypted on this device", "The vault key is wrapped by the Android Keystore, a vault PIN, or both. There is no account server.")
            Spacer(Modifier.height(12.dp))
            WelcomeCard(Icons.Default.Groups, "Family and personal records", "Keep passwords and documents for you and family members in one vault.")
            Spacer(Modifier.height(12.dp))
            WelcomeCard(Icons.Default.Bolt, "Offline", "The vault works without a network. Backup of the database and vault files is turned off.")
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WelcomeBlue, contentColor = Color.White),
            ) {
                Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
            val linkStyle = TextLinkStyles(SpanStyle(color = WelcomeBlue, fontWeight = FontWeight.SemiBold))
            val agreement = buildAnnotatedString {
                append("By continuing, you agree to the ")
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { legal = Destinations.Terms }) { append("Terms") }
                append(" and ")
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { legal = Destinations.Privacy }) { append("Privacy Policy") }
                append(".")
            }
            BasicText(
                agreement,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WelcomeCard(icon: ImageVector, title: String, body: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F7FC)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = WelcomeBlue, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = WelcomeTitle, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(body,fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
