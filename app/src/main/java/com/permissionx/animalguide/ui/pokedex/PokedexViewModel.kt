package com.permissionx.animalguide.ui.pokedex

import androidx.lifecycle.ViewModel
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.achievement.ALL_ACHIEVEMENTS
import com.permissionx.animalguide.domain.achievement.AchievementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val repository: AnimalRepository,
    private val achievementManager: AchievementManager
) : ViewModel() {
    val animals = repository.getAllAnimals()
    val animalCount = repository.getAnimalCount()

    fun getAllAchievements() = ALL_ACHIEVEMENTS
    fun isAchievementUnlocked(id: String) = achievementManager.isUnlocked(id)
}