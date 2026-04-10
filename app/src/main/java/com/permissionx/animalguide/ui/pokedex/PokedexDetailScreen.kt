package com.permissionx.animalguide.ui.pokedex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.result.components.ConservationBadge
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import com.permissionx.animalguide.ui.pokedex.components.AnimalHeaderSection
import com.permissionx.animalguide.ui.pokedex.components.AnimalInfoSection
import com.permissionx.animalguide.ui.pokedex.components.DiscoverySection
import com.permissionx.animalguide.ui.pokedex.components.NoteSection
import com.permissionx.animalguide.ui.pokedex.components.PhotoGallery
import com.permissionx.animalguide.ui.navigation.Routes
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import com.permissionx.animalguide.ui.auth.LoginViewModel

@Composable
fun PokedexDetailScreen(
    animalName: String,
    navController: NavController,
    viewModel: PokedexDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(animalName) {
        viewModel.loadAnimal(animalName)
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
        }

        is DetailUiState.Success -> {
            var showLoginDialog by remember { mutableStateOf(false) }

            val loginViewModel: LoginViewModel = hiltViewModel(
                viewModelStoreOwner = LocalContext.current as ComponentActivity
            )

            if (showLoginDialog) {
                AlertDialog(
                    onDismissRequest = { showLoginDialog = false },
                    title = { Text("登录后才能分享") },
                    text = { Text("登录后可以分享动物到社区！") },
                    confirmButton = {
                        Button(onClick = {
                            showLoginDialog = false
                            loginViewModel.resetState()
                            navController.navigate(Routes.LOGIN)
                        }) { Text("去登录") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLoginDialog = false }) {
                            Text("暂不登录")
                        }
                    }
                )
            }

            PokedexDetailContent(
                state = s,
                navController = navController,
                onRefreshInfo = { viewModel.refreshAnimalInfo() },
                onSaveNote = { viewModel.saveNote(it) },
                onStartEditNote = { viewModel.startEditNote() },
                onCancelEditNote = { viewModel.cancelEditNote() },
                onDelete = {
                    viewModel.deleteAnimal {
                        navController.popBackStack()
                    }
                },
                onDeletePhoto = { photo -> viewModel.deletePhoto(photo) },
                onSetCover = { photo -> viewModel.setCoverPhoto(photo) },
                onShareToSocial = {
                    if (viewModel.isLoggedIn) {
                        navController.navigate(Routes.publishWithAnimal(s.animal.animalName))
                    } else {
                        showLoginDialog = true
                    }
                }
            )
        }
    }
}

@Composable
fun PokedexDetailContent(
    state: DetailUiState.Success,
    navController: NavController,
    onRefreshInfo: () -> Unit,
    onSaveNote: (String) -> Unit,
    onStartEditNote: () -> Unit,
    onCancelEditNote: () -> Unit,
    onDelete: () -> Unit,
    onDeletePhoto: (AnimalPhoto) -> Unit,
    onSetCover: (AnimalPhoto) -> Unit,
    onShareToSocial: () -> Unit
) {
    val animal = state.animal
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.refreshMessage) {
        if (state.refreshMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.refreshMessage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部图片区域
            AnimalHeaderSection(
                animal = animal,
                navController = navController,
                onDelete = onDelete
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                // 照片墙
                if (state.photos.size > 1) {
                    PhotoGallery(
                        photos = state.photos,
                        coverUri = animal.imageUri,
                        onDeletePhoto = onDeletePhoto,
                        onSetCover = onSetCover
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 濒危等级 + 识别次数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConservationBadge(status = animal.conservationStatus)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "已识别 ${animal.recognizeCount} 次",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 发现记录
                DiscoverySection(
                    animal = animal,
                    address = state.address
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 科普信息
                AnimalInfoSection(
                    animal = animal,
                    isRefreshing = state.isRefreshingInfo,
                    onRefresh = onRefreshInfo
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 备注
                NoteSection(
                    animal = animal,
                    isEditing = state.isEditingNote,
                    onStartEdit = onStartEditNote,
                    onSave = onSaveNote,
                    onCancel = onCancelEditNote
                )
                // 分享到社区
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onShareToSocial() },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享到社区")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}