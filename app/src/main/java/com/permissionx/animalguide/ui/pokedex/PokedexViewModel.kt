package com.permissionx.animalguide.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.achievement.AchievementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val achievementManager: AchievementManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 搜索过滤后的列表
    val animals: StateFlow<List<AnimalEntry>> = combine(
        animalRepository.getAllAnimals(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.animalName.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val animalCount = animalRepository.getAnimalCount()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun isAchievementUnlocked(id: String) = achievementManager.isUnlocked(id)

    fun deleteAnimal(animal: AnimalEntry) {
        viewModelScope.launch {
            animalRepository.deleteAnimalWithPhotos(animal)
        }
    }
}