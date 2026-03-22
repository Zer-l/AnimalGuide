package com.permissionx.animalguide.ui.common

sealed class RecognizeState {
    object Idle : RecognizeState()
    object Loading : RecognizeState()
    data class Success(val results: List<Pair<String, Float>>) : RecognizeState()
    data class Error(val message: String) : RecognizeState()
}