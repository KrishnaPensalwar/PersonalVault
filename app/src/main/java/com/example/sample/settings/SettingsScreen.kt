package com.example.sample.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.security.BiometricAvailabilityChecker
import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.UnlockMethod
import com.example.sample.security.ui.BiometricUnlockScreen
import com.example.sample.security.ui.PinSetupFlow
import com.example.sample.security.ui.VaultBackground
import com.example.sample.security.ui.VaultNavy
import com.example.sample.security.ui.VaultSecondary
import com.example.sample.security.ui.VaultText
import org.koin.compose.koinInject
import java.util.concurrent.TimeUnit

@Composable
fun SettingsScreen(repository: AppLockRepository = koinInject()) {
    val backStack = LocalBackStack.current
    val method = repository.unlockMethod()

    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        SectionLabel("SECURITY")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
            SettingsRow(
                icon = Icons.Default.PhonelinkLock,
                title = "Unlock",
                subtitle = unlockStatus(method),
                onClick = { backStack.add(Destinations.Unlock) },
            )
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("LEGAL & COMPLIANCE")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "Vault terms and agreement",
                    onClick = { backStack.add(Destinations.Terms) },
                )
                HorizontalDivider(color = Color(0xFFEEF1F6))
                SettingsRow(
                    icon = Icons.Default.Shield,
                    title = "Privacy Policy",
                    subtitle = "On-device data policy",
                    onClick = { backStack.add(Destinations.Privacy) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
//        SecurityBadge(repository.hasBiometricWrap())
        VersionFooter()
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F6FB)) {
            Icon(icon, null, Modifier.padding(10.dp), tint = VaultNavy)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = VaultText, fontSize = 16.sp)
            Text(subtitle, color = VaultSecondary, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB7C0CE))
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        color = VaultSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val version = info.versionName ?: "1.0"
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    Text(
        "v$version ",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = VaultText,
        fontWeight = FontWeight.Medium,
    )
    Text(
        "Vault data stays on this device",
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        textAlign = TextAlign.Center,
        color = VaultSecondary,
        fontSize = 13.sp,
    )
}

internal fun unlockStatus(method: UnlockMethod): String = when (method) {
    UnlockMethod.DEVICE -> "Phone lock"
    UnlockMethod.PIN -> "Vault PIN"
    UnlockMethod.NONE -> "No lock"
    UnlockMethod.UNSET -> "Not set"
}

internal fun pinUpdatedLabel(changedAt: Long, now: Long = System.currentTimeMillis()): String {
    if (changedAt <= 0L) return "Not recorded on this install"
    val days = TimeUnit.MILLISECONDS.toDays(now - changedAt).coerceAtLeast(0)
    return when (days) {
        0L -> "Updated today"
        1L -> "Updated 1 day ago"
        else -> "Updated $days days ago"
    }
}
