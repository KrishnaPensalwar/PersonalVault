package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.settings.PrivacyScreen
import com.example.sample.settings.TermsScreen

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var legal by remember { mutableStateOf<String?>(null) }
    if (legal != null) {
        Column(Modifier.fillMaxSize().background(VaultBackground)) {
            TextButton(onClick = { legal = null }) { Text("Back") }
            if (legal == "terms") TermsScreen() else PrivacyScreen()
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp)
            .padding(WindowInsets.statusBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFE7EEF9)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, null, tint = VaultNavy, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Fortress Vault", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = VaultText)
        Text(
            "Your records stay on this phone.",
            color = Color(0xFF3D5AFE),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Passwords, family details, and documents are encrypted on this device. Nothing in the vault is uploaded.",
            color = VaultSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
//        Surface(shape = CircleShape, color = Color(0xFFE7F6EF)) {
//            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
//                Icon(Icons.Default.Verified, null, tint = Color(0xFF067647), modifier = Modifier.size(16.dp))
//                Text("  AES-256 on this device", color = Color(0xFF067647), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
//            }
//        }
        Spacer(Modifier.height(18.dp))
        WelcomeCard(Icons.Default.Lock, "Encrypted on this device", "The vault key is wrapped by the Android Keystore, a vault PIN, or both. There is no account server.")
        Spacer(Modifier.height(10.dp))
        WelcomeCard(Icons.Default.Groups, "Family and personal records", "Keep passwords and documents for you and family members in one vault.")
        Spacer(Modifier.height(10.dp))
        WelcomeCard(Icons.Default.Bolt, "Offline", "The vault works without a network. Backup of the database and vault files is turned off.")
        Spacer(Modifier.height(22.dp))
        VaultPrimaryButton("Get Started", Icons.AutoMirrored.Filled.ArrowForward, onGetStarted)
        Spacer(Modifier.height(12.dp))
        val agreement = buildAnnotatedString {
            append("By continuing, you agree to the ")
            pushStringAnnotation("link", "terms")
            withStyle(SpanStyle(color = VaultNavy, fontWeight = FontWeight.SemiBold)) { append("Terms") }
            pop()
            append(" and ")
            pushStringAnnotation("link", "privacy")
            withStyle(SpanStyle(color = VaultNavy, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
            pop()
            append(".")
        }
        Text(
            agreement,
            color = VaultSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                legal = "terms"
            },
        )
//        Row(horizontalArrangement = Arrangement.Center) {
//            Text("Terms", color = VaultNavy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.clickable { legal = "terms" }.padding(6.dp))
//            Text("Privacy Policy", color = VaultNavy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.clickable { legal = "privacy" }.padding(6.dp))
//        }
    }
}

@Composable
private fun WelcomeCard(icon: ImageVector, title: String, body: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F6FB)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = VaultNavy)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = VaultText)
                Text(body, color = VaultSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
