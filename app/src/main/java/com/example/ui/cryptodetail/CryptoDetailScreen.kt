package com.example.ui.cryptodetail

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CryptocurrencyUiModel
import com.example.data.model.PricePoint
import com.example.ui.ChartUiState
import com.example.ui.CryptoViewModel
import com.example.ui.cryptolist.formatCurrencyAndChange
import com.example.ui.theme.GreenUp
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.RedDown
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coin by viewModel.selectedCoin.collectAsState()
    val chartState by viewModel.chartUiState.collectAsState()
    val timeframe by viewModel.chartTimeframe.collectAsState()

    coin?.let { currentCoin ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = currentCoin.imageUrl,
                                contentDescription = "${currentCoin.name} logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentCoin.name} (${currentCoin.symbol})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(currentCoin) }) {
                            Icon(
                                imageVector = if (currentCoin.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite active coin",
                                tint = if (currentCoin.isFavorite) PremiumGold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Price Summary Block
                PriceHeaderSection(coin = currentCoin)

                Spacer(modifier = Modifier.height(16.dp))

                // Timeframe Chips + Price Graph Interactive Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Price History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Timeframe selector
                            TimeframeSelectorRow(
                                selected = timeframe,
                                onSelect = { viewModel.chartTimeframe.value = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Chart Render Box
                        InteractivePriceChart(
                            chartUiState = chartState,
                            timeframe = timeframe
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Generated Narrative Overview Description
                NarrativeOverviewSection(coin = currentCoin)

                Spacer(modifier = Modifier.height(20.dp))

                // Advanced Coin Statistics Metrics Grid
                AdvancedInfoMetricsGrid(coin = currentCoin)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } ?: run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PriceHeaderSection(coin: CryptocurrencyUiModel) {
    val (formattedPrice, percentStr) = formatCurrencyAndChange(coin.price, coin.percentChange24h)
    val colorFlag = if (coin.percentChange24h >= 0) GreenUp else RedDown

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Current Value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                text = formattedPrice,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Percentage indicator badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colorFlag.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (coin.percentChange24h >= 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colorFlag,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = percentStr,
                color = colorFlag,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TimeframeSelectorRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("1D", "7D", "30D", "1Y")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { timeframe ->
            val isActive = selected == timeframe
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(timeframe) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeframe,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun InteractivePriceChart(
    chartUiState: ChartUiState,
    timeframe: String
) {
    when (chartUiState) {
        is ChartUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is ChartUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chartUiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is ChartUiState.Success -> {
            val points = chartUiState.points
            if (points.isNotEmpty()) {
                ChartViewCanvas(points = points, timeframe = timeframe)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ChartViewCanvas(
    points: List<PricePoint>,
    timeframe: String
) {
    val prices = points.map { it.price }
    val maxPrice = prices.maxOrNull() ?: 1.0
    val minPrice = prices.minOrNull() ?: 0.0
    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    // Touch gesture dragging state indicators
    var activeIdx by remember { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf<Float?>(null) }

    val activeColor = MaterialTheme.colorScheme.primary
    val pathBrushGrad = Brush.verticalGradient(
        colors = listOf(
            activeColor.copy(alpha = 0.4f),
            activeColor.copy(alpha = 0.0f)
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Drag floating price info box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (activeIdx != null && activeIdx!! < points.size) {
                val pt = points[activeIdx!!]
                val df = when {
                    pt.price >= 1.0 -> DecimalFormat("$#,##0.00")
                    pt.price >= 0.01 -> DecimalFormat("$#,##0.0000")
                    else -> DecimalFormat("$#,##0.000000")
                }
                val formattedTime = remember(pt.timestamp) {
                    val sdf = when (timeframe) {
                        "1D" -> SimpleDateFormat("HH:mm", Locale.getDefault())
                        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    }
                    sdf.format(Date(pt.timestamp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = df.format(pt.price),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Text(
                    text = "Drag or hold over the chart to inspect prices",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Draw Interactive Pricing Line
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val unitColW = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / unitColW).toInt().coerceIn(0, points.size - 1)
                            activeIdx = idx
                            touchX = offset.x
                        },
                        onDragEnd = {
                            activeIdx = null
                            touchX = null
                        },
                        onDragCancel = {
                            activeIdx = null
                            touchX = null
                        },
                        onDrag = { change, _ ->
                            val unitColW = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                            val currX = change.position.x
                            val idx = (currX / unitColW).toInt().coerceIn(0, points.size - 1)
                            activeIdx = idx
                            touchX = currX
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val limitIndex = points.size - 1
            val unitCellWidth = width / limitIndex.coerceAtLeast(1)

            // Draw clean subtle gridlines
            val linesCount = 4
            for (i in 0..linesCount) {
                val gridY = (height / linesCount) * i
                drawLine(
                    color = activeColor.copy(alpha = 0.1f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Generate Path Points
            val pointsOffset = points.mapIndexed { idx, pt ->
                val x = unitCellWidth * idx
                val y = height - (((pt.price - minPrice) / priceRange) * height).toFloat()
                Offset(x, y)
            }

            // Path construction
            if (pointsOffset.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(pointsOffset[0].x, pointsOffset[0].y)
                    for (i in 1..pointsOffset.lastIndex) {
                        // Smooth curves or direct lines
                        lineTo(pointsOffset[i].x, pointsOffset[i].y)
                    }
                }

                // Curved Area brush fill
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = pathBrushGrad
                )

                // Draw Core Pricing Stroke
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw interaction highlighting indicator vertical line & dot if dragging
            if (touchX != null && activeIdx != null && activeIdx!! < pointsOffset.size) {
                val targetOffset = pointsOffset[activeIdx!!]

                // Draw vertical ruler line
                drawLine(
                    color = activeColor.copy(alpha = 0.5f),
                    start = Offset(targetOffset.x, 0f),
                    end = Offset(targetOffset.x, height),
                    strokeWidth = 1.dp.toPx()
                )

                // Highlight Dot outline ring
                drawCircle(
                    color = activeColor.copy(alpha = 0.2f),
                    radius = 12.dp.toPx(),
                    center = targetOffset
                )

                // Highlight Core Dot Center
                drawCircle(
                    color = activeColor,
                    radius = 5.dp.toPx(),
                    center = targetOffset
                )
            }
        }
    }
}

@Composable
fun NarrativeOverviewSection(coin: CryptocurrencyUiModel) {
    val df = DecimalFormat("$#,##0.00")
    val formatter = DecimalFormat("#,##0")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Professional description block
            val descriptionText = remember(coin) {
                val name = coin.name
                val symbol = coin.symbol
                val indexRank = coin.cmcRank
                val percentText = if (coin.percentChange24h >= 0) "gained ${"%.2f".format(coin.percentChange24h)}%" else "declined ${"%.2f".format(coin.percentChange24h)}%"
                val capitalization = df.format(coin.marketCap)
                val totalS = formatter.format(coin.totalSupply)

                StringBuilder().apply {
                    append("$name ($symbol) is ranked #$indexRank among all tracked cryptocurrencies by market capitalization on CoinMarketCap. ")
                    append("Over the last 24 hours, its pricing has $percentText, settling at a price of ${df.format(coin.price)}. ")
                    append("It commands a global market capitalization of $capitalization, with a registered circulating supply of ${formatter.format(coin.circulatingSupply)} coins ")
                    if (coin.maxSupply > 0) {
                        append("out of a hardcoded maximum cap of ${formatter.format(coin.maxSupply)} $symbol.")
                    } else {
                        append("from an existing total cap of $totalS $symbol.")
                    }
                }.toString()
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun AdvancedInfoMetricsGrid(coin: CryptocurrencyUiModel) {
    Column {
        Text(
            text = "Market Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Stat items lists
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val df = DecimalFormat("$#,##0.00")
                val floatF = DecimalFormat("#,##0.0")

                StatMetricRow(
                    label = "Market Cap",
                    value = df.format(coin.marketCap)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "24h Traded Volume",
                    value = df.format(coin.volume24h)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "Circulating Supply",
                    value = "${floatF.format(coin.circulatingSupply)} ${coin.symbol}"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "Total Supply Limit",
                    value = "${floatF.format(coin.totalSupply)} ${coin.symbol}"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "Maximum Supply Limit",
                    value = if (coin.maxSupply > 0) "${floatF.format(coin.maxSupply)} ${coin.symbol}" else "Infinite"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "1h Change percent",
                    value = "${"%.2f".format(coin.percentChange1h)}%",
                    valueColor = if (coin.percentChange1h >= 0) GreenUp else RedDown
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "7d Change percent",
                    value = "${"%.2f".format(coin.percentChange7d)}%",
                    valueColor = if (coin.percentChange7d >= 0) GreenUp else RedDown
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                StatMetricRow(
                    label = "30d Change percent",
                    value = "${"%.2f".format(coin.percentChange30d)}%",
                    valueColor = if (coin.percentChange30d >= 0) GreenUp else RedDown
                )
            }
        }
    }
}

@Composable
fun StatMetricRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
