package com.example.ui.cryptolist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CryptocurrencyUiModel
import com.example.ui.CryptoViewModel
import com.example.ui.theme.GreenUp
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.RedDown
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoListScreen(
    viewModel: CryptoViewModel,
    onCoinClick: (CryptocurrencyUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val coinList by viewModel.cryptocurrencies.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()

    // Trigger paging when list is scrolled near the bottom list end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .filter { layoutInfo ->
                val totalItemsCount = layoutInfo.totalItemsCount
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 10
            }
            .collect {
                viewModel.loadNextPage()
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Box
        SearchAndFilterSection(
            searchQuery = searchQuery,
            onSearchChange = { viewModel.updateSearchQuery(it) },
            sortOption = sortOption,
            onSortChange = { viewModel.updateSortOption(it) }
        )

        // Show Error Snackbar of sort if present
        errorMsg?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error Logo",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // List Section or placeholders
        Box(modifier = Modifier.weight(1f)) {
            if (coinList.isEmpty() && (isRefreshing || isLoading)) {
                // Elegant Shimmer Skeleton Screen
                ShimmerSkeletonList()
            } else if (coinList.isEmpty()) {
                // Empty search listings layout
                EmptyStateView()
            } else {
                // Listing lazy column
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Stately Top Movers horizontal cards block at the top if search feels empty
                    if (searchQuery.trim().isEmpty()) {
                        val topMovers = coinList
                            .filter { kotlin.math.abs(it.percentChange24h) > 0.01 }
                            .sortedByDescending { kotlin.math.abs(it.percentChange24h) }
                            .take(5)
                        if (topMovers.isNotEmpty()) {
                            item {
                                TopMovingCoinsCarousel(
                                    movingCoins = topMovers,
                                    onCoinClick = onCoinClick
                                )
                            }

                            item {
                                Text(
                                    text = "Market Listings",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = coinList,
                        key = { _, coin -> coin.id }
                    ) { index, coin ->
                        AnimatedEntranceContainer(index = index) {
                            CryptoItemRow(
                                coin = coin,
                                onClick = { onCoinClick(coin) },
                                onFavoriteClick = { viewModel.toggleFavorite(coin) }
                            )
                        }

                        // Visual Divider
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Bottom progress bar when fetching more pages
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOption: CryptoViewModel.SortOption,
    onSortChange: (CryptoViewModel.SortOption) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_indicator")
                .clip(RoundedCornerShape(24.dp)),
            placeholder = { Text("Search cryptocurrency...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search icon",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search text",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sorting filters list chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortFilterChip(
                selected = sortOption == CryptoViewModel.SortOption.RankAsc,
                label = "Rank #",
                onClick = { onSortChange(CryptoViewModel.SortOption.RankAsc) }
            )
            SortFilterChip(
                selected = sortOption == CryptoViewModel.SortOption.PriceDesc,
                label = "Price Down ↓",
                onClick = { onSortChange(CryptoViewModel.SortOption.PriceDesc) }
            )
            SortFilterChip(
                selected = sortOption == CryptoViewModel.SortOption.PriceAsc,
                label = "Price Up ↑",
                onClick = { onSortChange(CryptoViewModel.SortOption.PriceAsc) }
            )
            SortFilterChip(
                selected = sortOption == CryptoViewModel.SortOption.Change24hDesc,
                label = "Change % Down ↓",
                onClick = { onSortChange(CryptoViewModel.SortOption.Change24hDesc) }
            )
            SortFilterChip(
                selected = sortOption == CryptoViewModel.SortOption.Change24hAsc,
                label = "Change % Up ↑",
                onClick = { onSortChange(CryptoViewModel.SortOption.Change24hAsc) }
            )
        }
    }
}

@Composable
fun SortFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search empty",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Ensure correct spelling or try toggling filters.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CryptoItemRow(
    coin: CryptocurrencyUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank designation
        Text(
            text = "${coin.cmcRank}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .width(28.dp)
                .padding(end = 4.dp),
            textAlign = TextAlign.Start
        )

        // Coin Icon Coil Async Image
        AsyncImage(
            model = coin.imageUrl,
            contentDescription = "${coin.name} logo image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Titles Coin Symbol & Name
        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = coin.symbol,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = coin.name,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Price formatting block
        val (formattedPrice, percentageChange) = formatCurrencyAndChange(coin.price, coin.percentChange24h)

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1.5f)
        ) {
            Text(
                text = formattedPrice,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Beautiful status indicator badge
            val colorFlag = if (coin.percentChange24h >= 0) GreenUp else RedDown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorFlag.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (coin.percentChange24h >= 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Trend direction arrow",
                    tint = colorFlag,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = percentageChange,
                    color = colorFlag,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Favorite Button toggle
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (coin.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Toggle coin favorite state",
                tint = if (coin.isFavorite) PremiumGold else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Global utility helper to format pricing with proper decimal depth
fun formatCurrencyAndChange(price: Double, change: Double): Pair<String, String> {
    val priceFormatter = when {
        price >= 1.0 -> DecimalFormat("$#,##0.00")
        price >= 0.01 -> DecimalFormat("$#,##0.0000")
        else -> DecimalFormat("$#,##0.000000")
    }
    val changeFormatter = DecimalFormat("0.00%")
    val sign = if (change >= 0) "+" else ""
    return priceFormatter.format(price) to "$sign${changeFormatter.format(change / 100.0)}"
}

@Composable
fun AnimatedEntranceContainer(
    index: Int,
    content: @Composable () -> Unit
) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index % 12) * 40L)
        visible.value = true
    }
    AnimatedVisibility(
        visible = visible.value,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(350, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(350)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}

@Composable
fun ShimmerSkeletonList() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shimmer header block
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        repeat(7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Skeleton
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Circle Logo
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Code Name
                Column(modifier = Modifier.weight(1.2f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Financials values
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer_transition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_offset"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun TopMovingCoinsCarousel(
    movingCoins: List<CryptocurrencyUiModel>,
    onCoinClick: (CryptocurrencyUiModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Aesthetic Section Header with neon pulse accent icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Top Daily Movers",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            movingCoins.forEachIndexed { index, coin ->
                TopMoverCard(
                    coin = coin,
                    onClick = { onCoinClick(coin) }
                )
            }
        }
    }
}

@Composable
fun TopMoverCard(
    coin: CryptocurrencyUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = coin.percentChange24h >= 0
    val trendColor = if (isPositive) GreenUp else RedDown

    // Bounce interaction when drawing
    val scale = remember { Animatable(0.9f) }
    LaunchedEffect(key1 = coin.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .width(150.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            color = trendColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = coin.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                
                // Percent change badge
                val formattedChange = remember(coin.percentChange24h) {
                    val sign = if (isPositive) "+" else ""
                    val formatter = DecimalFormat("0.0%")
                    "$sign${formatter.format(coin.percentChange24h / 100.0)}"
                }
                Text(
                    text = formattedChange,
                    color = trendColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = coin.symbol,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = coin.name,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mini line-chart drawing in the gainer/loser card!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = Path()
                    val changeFactor = (coin.percentChange24h.toFloat() / 10f).coerceIn(-1.5f, 1.5f)
                    
                    // Generate 5 keyframe points of trace path waves
                    val y1 = h * 0.5f
                    val y2 = h * (0.5f - (changeFactor * 0.15f))
                    val y3 = h * (0.5f + (changeFactor * 0.25f))
                    val y4 = h * (0.5f - (changeFactor * 0.40f))
                    val y5 = h * (0.5f - (changeFactor * 0.75f))
                    val offsets = listOf(
                        Offset(0f, y1),
                        Offset(w * 0.25f, y2),
                        Offset(w * 0.5f, y3),
                        Offset(w * 0.75f, y4),
                        Offset(w, y5)
                    )
                    
                    path.moveTo(offsets[0].x, offsets[0].y)
                    for (i in 1 until offsets.size) {
                        path.lineTo(offsets[i].x, offsets[i].y)
                    }
                    
                    drawPath(
                        path = path,
                        color = trendColor,
                        style = Stroke(width = 1.35.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val formattedPrice = remember(coin.price) {
                val f = when {
                    coin.price >= 1.0 -> DecimalFormat("$#,##0.00")
                    coin.price >= 0.01 -> DecimalFormat("$#,##0.0000")
                    else -> DecimalFormat("$#,##0.000000")
                }
                f.format(coin.price)
            }
            Text(
                text = formattedPrice,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
