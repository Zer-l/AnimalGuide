package com.permissionx.animalguide.domain.achievement

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val requiredCount: Int
)

val ALL_ACHIEVEMENTS = listOf(
    Achievement("explorer", "初级探险家", "收录10种动物", "🥉", 10),
    Achievement("naturalist", "中级博物学家", "收录30种动物", "🥈", 30),
    Achievement("zoologist", "高级动物学家", "收录60种动物", "🥇", 60),
    Achievement("legend", "传奇收藏家", "收录100种动物", "💎", 100)
)

@Singleton
class AchievementManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    // 检测新解锁的成就，返回新获得的成就列表
    fun checkAchievements(currentCount: Int): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        ALL_ACHIEVEMENTS.forEach { achievement ->
            if (currentCount >= achievement.requiredCount && !isUnlocked(achievement.id)) {
                unlock(achievement.id)
                newlyUnlocked.add(achievement)
            }
        }
        return newlyUnlocked
    }

    fun isUnlocked(achievementId: String): Boolean {
        return prefs.getBoolean(achievementId, false)
    }

    private fun unlock(achievementId: String) {
        prefs.edit().putBoolean(achievementId, true).apply()
    }

    fun getUnlockedCount(): Int {
        return ALL_ACHIEVEMENTS.count { isUnlocked(it.id) }
    }
}