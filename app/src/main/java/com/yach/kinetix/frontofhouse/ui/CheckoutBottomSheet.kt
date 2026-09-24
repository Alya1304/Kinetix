package com.yach.kinetix.frontofhouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    totalAmount: BigDecimal,
    onDismiss: () -> Unit,
    onConfirmCheckout: (PaymentMethod, tip: BigDecimal) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var receivedCashStr by remember { mutableStateOf("") }
    var tipStr by remember { mutableStateOf("") }

    val receivedCash = receivedCashStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val tipAmount = tipStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val totalWithTip = totalAmount + tipAmount
    val changeAmount = if (selectedMethod == PaymentMethod.CASH) {
        (receivedCash - totalWithTip).coerceAtLeast(BigDecimal.ZERO)
    } else BigDecimal.ZERO

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Completar Pago",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Select Payment Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodChip(
                    method = PaymentMethod.CASH,
                    icon = Icons.Default.AttachMoney,
                    label = "Efectivo",
                    selected = selectedMethod == PaymentMethod.CASH,
                    onClick = { selectedMethod = PaymentMethod.CASH }
                )
                PaymentMethodChip(
                    method = PaymentMethod.CARD,
                    icon = Icons.Default.CreditCard,
                    label = "Tarjeta",
                    selected = selectedMethod == PaymentMethod.CARD,
                    onClick = { selectedMethod = PaymentMethod.CARD }
                )
                PaymentMethodChip(
                    method = PaymentMethod.TRANSFER,
                    icon = Icons.Default.PhoneIphone,
                    label = "Transfer",
                    selected = selectedMethod == PaymentMethod.TRANSFER,
                    onClick = { selectedMethod = PaymentMethod.TRANSFER }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tipStr,
                onValueChange = { tipStr = it },
                label = { Text("Propina (Opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("$") }
            )

            if (selectedMethod == PaymentMethod.CASH) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = receivedCashStr,
                    onValueChange = { receivedCashStr = it },
                    label = { Text("Efectivo Recibido") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") }
                )
                
                // Quick Cash Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val roundedTotal = Math.ceil(totalWithTip.toDouble() / 50.0) * 50.0
                    val amounts = listOf(roundedTotal.toInt().toString(), "100", "200", "500")
                    amounts.forEach { amt ->
                        SuggestionChip(
                            onClick = { receivedCashStr = amt },
                            label = { Text("$$amt") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total a Pagar:", style = MaterialTheme.typography.titleMedium)
                Text("$$totalWithTip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (selectedMethod == PaymentMethod.CASH && receivedCash > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cambio:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("$$changeAmount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isConfirmEnabled = if (selectedMethod == PaymentMethod.CASH) receivedCash >= totalWithTip else true

            Button(
                onClick = { onConfirmCheckout(selectedMethod, tipAmount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isConfirmEnabled
            ) {
                Text(
                    text = if (selectedMethod == PaymentMethod.CASH) "Cobrar y dar $$changeAmount cambio" else "Confirmar Pago",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.PaymentMethodChip(
    method: PaymentMethod,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.weight(1f)
    )
}
