package com.permissionx.animalguide.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.history.components.HistoryDetailContent

@Composable
fun HistoryDetailScreen(
    historyId: Int,
    navController: NavController,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(historyId) {
        viewModel.loadDetail(historyId)
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is HistoryDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is HistoryDetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
        }

        is HistoryDetailUiState.Success -> {
            HistoryDetailContent(state = s, navController = navController)
        }
    }
}