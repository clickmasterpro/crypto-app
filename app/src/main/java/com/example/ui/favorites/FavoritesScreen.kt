package com.example.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptocurrencyUiModel
import com.example.ui.CryptoViewModel
import com.example.ui.cryptolist.CryptoItemRow

@Composable
fun FavoritesScreen(
    viewModel: CryptoViewModel,
    onCoinClick: (CryptocurrencyUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoritesList by viewModel.favorites.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (favoritesList.isEmpty()) {
            EmptyWatchlistView()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = favoritesList,
                    key = { coin -> coin.id }
                ) { coin ->
                    CryptoItemRow(
                        coin = coin,
                        onClick = { onCoinClick(coin) },
                        onFavoriteClick = { viewModel.toggleFavorite(coin) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyWatchlistView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Empty watchlist",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
        Text(
            text = "Your Watchlist is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Keep track of specific cryptocurrencies by tapping the star icon next to them on the Market screen.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
