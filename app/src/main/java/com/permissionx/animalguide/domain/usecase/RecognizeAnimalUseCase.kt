package com.permissionx.animalguide.domain.usecase

import android.net.Uri
import com.permissionx.animalguide.data.repository.RecognizeRepository
import com.permissionx.animalguide.domain.error.AppError
import javax.inject.Inject

class RecognizeAnimalUseCase @Inject constructor(
    private val recognizeRepository: RecognizeRepository
) {
    suspend operator fun invoke(uri: Uri): Result<List<Pair<String, Float>>> {
        val result = recognizeRepository.recognizeAnimal(uri)
        result.onSuccess { results ->
            val top = results.firstOrNull()
            if (top?.first == "非动物") {
                return Result.failure(AppError.NotAnimalError())
            }
        }
        return result
    }
}