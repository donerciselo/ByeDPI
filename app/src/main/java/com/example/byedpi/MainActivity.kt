package com.example.byedpi

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var isConnected by mutableStateOf(false)

    private val vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startDpiService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        isConnected = isServiceRunning(DpiBypassService::class.java)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VpnScreen(
                    isConnected = isConnected,
                    onConnectClick = {
                        if (isConnected) {
                            stopDpiService()
                        } else {
                            prepareVpn()
                        }
                    }
                )
            }
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnRequestLauncher.launch(intent)
        } else {
            startDpiService()
        }
    }

    private fun startDpiService() {
        val intent = Intent(this, DpiBypassService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isConnected = true
    }

    private fun stopDpiService() {
        val intent = Intent(this, DpiBypassService::class.java)
        stopService(intent)
        isConnected = false
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

@Composable
fun VpnScreen(isConnected: Boolean, onConnectClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E2C),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("Ana Sayfa", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2B83FA),
                        selectedTextColor = Color(0xFF2B83FA),
                        indicatorColor = Color(0xFF2A2A3C),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Text("💎", fontSize = 20.sp) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Premium", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Yakında",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2B83FA),
                        selectedTextColor = Color(0xFF2B83FA),
                        indicatorColor = Color(0xFF2A2A3C),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1A)) // Koyu ve daha estetik arka plan
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // Top Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tek dokunuşla",
                        color = Color.LightGray,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Güvenliğe Bağlan",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Main App Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color(0xFF1E1E2C))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛡️", fontSize = 24.sp)
                            Text(text = "ByeDPI Proxy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "⚙️", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Connect Button with Glowing effect
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF2B83FA).copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.1f))
                                .clickable { onConnectClick() }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF2B83FA).copy(alpha = 0.3f) else Color.DarkGray.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) Color(0xFF2B83FA) else Color(0xFF333344))
                                ) {
                                    Text(
                                        text = "⏻",
                                        color = if (isConnected) Color.White else Color.Gray,
                                        fontSize = 56.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(35.dp))

                        Text(
                            text = if (isConnected) "BAĞLANDI" else "BAĞLI DEĞİL",
                            color = if (isConnected) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Configuration Info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 30.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2A2A3C))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Strateji", color = Color.Gray, fontSize = 12.sp)
                                Text(text = "Split TLS / HTTP Proxy", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Text(text = "⚡", fontSize = 24.sp)
                        }
                    }
                }
            } else {
                // Premium Tab
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💎", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Premium Özellikler",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Çok Yakında!",
                            color = Color(0xFFFF9800),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
