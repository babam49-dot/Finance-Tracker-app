package com.example.ethfinance.presentation.reports

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ethfinance.R
import com.example.ethfinance.core.designsystem.LocalExtendedColors
import com.example.ethfinance.core.util.MoneyFormatter
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reports_title),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Summary Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extendedColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "MONTHLY SUMMARY",
                            style = MaterialTheme.typography.labelLarge,
                            color = extendedColors.textLabel
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = MoneyFormatter.formatSantim(state.totalExpenseMinor, masked = state.areAmountsHidden),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = extendedColors.expenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Net Saved",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = MoneyFormatter.formatSantim(state.netSavedMinor, includeSign = true, masked = state.areAmountsHidden),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = extendedColors.incomeGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = extendedColors.divider)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.toggle_compare),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = state.compareWithPrevious,
                                onCheckedChange = { viewModel.toggleCompare() }
                            )
                        }
                    }
                }
            }

            // 2. Spending by Category Donut Chart
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extendedColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.report_spending_by_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Donut Chart via Compose Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(slices = state.categorySlices)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extendedColors.textLabel
                                )
                                Text(
                                    text = MoneyFormatter.formatSantim(state.totalExpenseMinor, masked = state.areAmountsHidden),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Legend
                        state.categorySlices.forEach { slice ->
                            val sliceColor = try {
                                Color(android.graphics.Color.parseColor(slice.colorHex))
                            } catch (e: Exception) {
                                Color.Gray
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(sliceColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.categoryName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "%.1f%%".format(slice.percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extendedColors.textLabel
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = MoneyFormatter.formatSantim(slice.amountMinor, masked = state.areAmountsHidden),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Spending Trend Area Chart
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extendedColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.report_spending_trend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        TrendAreaChart(
                            primaryColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Peak: 12 Nov (1,240.00 ETB)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Smart Insights
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.report_insights),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "• Food & Dining accounts for 26.7% of your total spend this month.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Your highest spending day this week was Friday.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Total fees and service charges paid: 1.14 ETB.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 5. Export Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportCsv(context, state) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, extendedColors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_export_csv))
                    }

                    Button(
                        onClick = { exportPdf(context, state) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_export_pdf))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DonutChart(slices: List<CategorySlice>) {
    Canvas(modifier = Modifier.size(180.dp)) {
        val strokeWidth = 26.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val centerOffset = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        if (slices.isEmpty()) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.4f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
        } else {
            slices.forEach { slice ->
                val sweep = (slice.percentage / 100f) * 360f
                val sliceColor = try {
                    Color(android.graphics.Color.parseColor(slice.colorHex))
                } catch (e: Exception) {
                    Color.Gray
                }

                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweep
            }
        }
    }
}

@Composable
fun TrendAreaChart(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val points = listOf(
            Offset(0f, h * 0.8f),
            Offset(w * 0.15f, h * 0.7f),
            Offset(w * 0.35f, h * 0.85f),
            Offset(w * 0.55f, h * 0.2f), // Peak
            Offset(w * 0.75f, h * 0.6f),
            Offset(w, h * 0.5f)
        )

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (p in points) {
                lineTo(p.x, p.y)
            }
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (p in points) {
                lineTo(p.x, p.y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            color = primaryColor.copy(alpha = 0.15f)
        )

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Peak point marker
        drawCircle(
            color = primaryColor,
            radius = 5.dp.toPx(),
            center = points[3]
        )
    }
}

private fun exportCsv(context: Context, state: ReportsUiState) {
    try {
        val file = File(context.cacheDir, "eth_finance_report.csv")
        file.bufferedWriter().use { writer ->
            writer.write("Category,Amount (ETB),Percentage\n")
            for (slice in state.categorySlices) {
                val amt = slice.amountMinor / 100.0
                writer.write("${slice.categoryName},$amt,${slice.percentage}%\n")
            }
        }
        shareFile(context, file, "text/csv")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun exportPdf(context: Context, state: ReportsUiState) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Ethiopian Finance Tracker - Financial Report", 40f, 60f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Total Income: ${state.totalIncomeMinor / 100.0} ETB", 40f, 100f, paint)
        canvas.drawText("Total Expenses: ${state.totalExpenseMinor / 100.0} ETB", 40f, 120f, paint)
        canvas.drawText("Net Flow: ${state.netSavedMinor / 100.0} ETB", 40f, 140f, paint)

        var y = 180f
        canvas.drawText("Category Breakdown:", 40f, y, paint)
        y += 20f

        for (slice in state.categorySlices) {
            canvas.drawText("${slice.categoryName}: ${slice.amountMinor / 100.0} ETB (%.1f%%)".format(slice.percentage), 60f, y, paint)
            y += 20f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "eth_finance_report.pdf")
        val output = FileOutputStream(file)
        document.writeTo(output)
        document.close()
        output.close()

        shareFile(context, file, "application/pdf")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Report"))
}
