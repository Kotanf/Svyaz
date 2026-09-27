package ru.maximnosko.svyaz.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val networkData = remember(context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        
        val operatorName = telephonyManager?.networkOperatorName?.ifBlank {
            telephonyManager.simOperatorName.ifBlank { "Мобильный оператор" }
        } ?: "Неизвестно"

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val networkTypeStr = when {
            isWifi -> "Wi-Fi Сеть"
            isCellular -> "Мобильная сеть 4G/LTE"
            else -> "Сотовая сеть"
        }

        val operatorCode = telephonyManager?.networkOperator ?: ""
        val mccMnc = if (operatorCode.length >= 5) {
            "${operatorCode.take(3)} / ${operatorCode.substring(3)}"
        } else {
            "250 / 01"
        }

        val isRoaming = if (telephonyManager?.isNetworkRoaming == true) "Включено" else "Отключено"
        
        var ipAddress = "127.0.0.1"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val element = interfaces.nextElement()
                val addrs = element.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        ipAddress = addr.hostAddress ?: ipAddress
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        val dataState = try {
            if (telephonyManager?.isDataEnabled == true) "Передача данных активна" else "Подключено"
        } catch (_: SecurityException) {
            "Подключено"
        }

        mapOf(
            "operator" to operatorName,
            "networkType" to networkTypeStr,
            "mccMnc" to mccMnc,
            "roaming" to isRoaming,
            "ipAddress" to ipAddress,
            "dataState" to dataState
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Состояние сети") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            InfoSection("Основная SIM-карта") {
                InfoRow("Оператор", networkData["operator"] ?: "Оператор")
                InfoRow("Тип сети", networkData["networkType"] ?: "Сотовая сеть")
                InfoRow("IP-адрес", networkData["ipAddress"] ?: "127.0.0.1")
                InfoRow("Статус связи", networkData["dataState"] ?: "Подключено")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoSection("Техническая информация") {
                InfoRow("MCC / MNC", networkData["mccMnc"] ?: "250 / 01")
                InfoRow("Роуминг", networkData["roaming"] ?: "Отключено")
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
