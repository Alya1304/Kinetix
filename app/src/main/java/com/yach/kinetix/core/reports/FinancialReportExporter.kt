package com.yach.kinetix.core.reports

import android.content.Context
import android.os.Environment
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialReportExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Exporta el historial de ventas a un archivo CSV con cabecera UTF-8 BOM compatible con Microsoft Excel.
     */
    suspend fun exportSalesToCsv(orders: List<OrderEntity>): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "reporte_ventas_$timestamp.csv"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(targetDir, fileName)

            FileOutputStream(file).use { fos ->
                // Escribir marca BOM UTF-8 (\uFEFF) para que Excel reconozca tildes y caracteres especiales automáticamente
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.appendLine("Folio,Fecha,Tipo de Pedido,Metodo de Pago,Subtotal Base,IVA,Propina,Total")
                    for (o in orders) {
                        writer.appendLine("${o.orderNumber},\"${o.createdAt}\",\"${o.orderType}\",\"${o.paymentMethod ?: ""}\",${o.baseAmount},${o.taxAmount},${o.tipAmount},${o.totalAmount}")
                    }
                }
            }

            file
        }
    }

    /**
     * Genera un reporte de Estado de Resultados (P&L: Ingresos por Ventas - Gastos Operativos = Utilidad Neta).
     */
    suspend fun exportIncomeStatementToCsv(
        totalSales: BigDecimal,
        totalExpenses: BigDecimal,
        expensesList: List<ExpenseEntity>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "estado_resultados_$timestamp.csv"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(targetDir, fileName)

            val netProfit = totalSales.subtract(totalExpenses)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.appendLine("CONCEPTO,IMPORTE")
                    writer.appendLine("Ingresos Totales por Ventas,${totalSales.toPlainString()}")
                    writer.appendLine("Gastos Operativos Totales,${totalExpenses.toPlainString()}")
                    writer.appendLine("UTILIDAD NETA RESULTANTE,${netProfit.toPlainString()}")
                    writer.appendLine("")
                    writer.appendLine("--- DETALLE DE GASTOS OPERATIVOS ---")
                    writer.appendLine("ID,Monto,Estatus,Notas,Fecha")
                    for (e in expensesList) {
                        writer.appendLine("${e.id},${e.amount},${e.status},\"${e.notes ?: ""}\",\"${e.createdAt}\"")
                    }
                }
            }

            file
        }
    }
}
