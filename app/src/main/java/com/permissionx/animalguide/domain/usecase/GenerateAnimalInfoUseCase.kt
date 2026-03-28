package com.permissionx.animalguide.domain.usecase

import com.permissionx.animalguide.data.repository.RecognizeRepository
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.error.toAppError
import com.permissionx.animalguide.domain.model.AnimalInfo
import kotlinx.coroutines.delay
import javax.inject.Inject

class GenerateAnimalInfoUseCase @Inject constructor(
    private val recognizeRepository: RecognizeRepository
) {
    suspend operator fun invoke(
        animalName: String,
        onRetry: ((attempt: Int) -> Unit)? = null
    ): Result<AnimalInfo> {
        var attempt = 0
        var lastResult: Result<AnimalInfo>? = null

        while (attempt < 4) {
            if (attempt > 0) {
                onRetry?.invoke(attempt)
                delay(2000)
            }
            try {
                lastResult = recognizeRepository.generateAnimalInfoOnce(animalName)
                if (lastResult.isSuccess) return lastResult
                when (lastResult.exceptionOrNull()) {
                    is AppError.TimeoutError -> attempt++
                    else -> return lastResult
                }
            } catch (_: AppError.TimeoutError) {
                attempt++
            } catch (e: Exception) {
                return Result.failure(e.toAppError())
            }
        }
        return lastResult ?: Result.failure(AppError.TimeoutError())
    }
}