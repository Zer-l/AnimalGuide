package com.permissionx.animalguide.domain.usecase

import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.achievement.Achievement
import com.permissionx.animalguide.domain.achievement.AchievementManager
import javax.inject.Inject

class CheckAchievementUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val achievementManager: AchievementManager
) {
    suspend operator fun invoke(): List<Achievement> {
        val currentCount = animalRepository.getAnimalCountOnce()
        return achievementManager.checkAchievements(currentCount)
    }
}