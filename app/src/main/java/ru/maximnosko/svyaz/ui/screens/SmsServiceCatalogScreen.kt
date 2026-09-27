package ru.maximnosko.svyaz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

data class SmsServiceItem(
    val shortNumber: String,
    val name: String,
    val category: String,
    val description: String,
    val defaultCommand: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsServiceCatalogScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenServiceChat: (String) -> Unit
) {
    var parkingTimerMinutes by remember { mutableIntStateOf(60) }
    var isParkingTimerActive by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(3600) }

    LaunchedEffect(isParkingTimerActive) {
        if (isParkingTimerActive) {
            while (remainingSeconds > 0 && isParkingTimerActive) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds <= 0) {
                isParkingTimerActive = false
            }
        }
    }

    val services = listOf(
        SmsServiceItem("7757", "Парковки Москвы", "Транспорт", "Оплата и продление парковки (команды: p100*1 или p100*2)", "p100*1"),
        SmsServiceItem("0919", "Госуслуги", "Госсервисы", "Оценка качества предоставления государственных услуг", "1"),
        SmsServiceItem("900", "Сбербанк", "Финансы", "СМС-банкинг: баланс, переводы и выписки", "Баланс"),
        SmsServiceItem("7377", "Городские услуги", "Транспорт", "Проверка штрафов и эвакуации автомобиля", "Штраф"),
        SmsServiceItem("5115", "Транспортная карта", "Транспорт", "Пополнение и баланс карты Стрелка / Тройка", "Баланс")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("СМС-Сервисы и Таймеры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Parking / Reminder Timer Card
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalParking, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Таймер парковки и напоминания", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        if (isParkingTimerActive) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            val timeStr = "${if (mins < 10) "0" else ""}$mins:${if (secs < 10) "0" else ""}$secs"
                            Text(
                                text = "До окончания: $timeStr мин",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { isParkingTimerActive = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Остановить таймер")
                            }
                        } else {
                            Text("Установите таймер для напоминания о завершении оплаты парковки:", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 60, 120).forEach { mins ->
                                    FilterChip(
                                        selected = parkingTimerMinutes == mins,
                                        onClick = { parkingTimerMinutes = mins },
                                        label = { Text("$mins мин") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    remainingSeconds = parkingTimerMinutes * 60
                                    isParkingTimerActive = true
                                },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Запустить таймер")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Каталог проверенных СМС-сервисов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(services) { service ->
                Card(
                    onClick = { onOpenServiceChat(service.shortNumber) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    service.shortNumber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(service.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { onOpenServiceChat(service.shortNumber) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Открыть чат", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
