package com.permissionx.animalguide.domain.usecase

import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.data.repository.PhotoRepository
import com.permissionx.animalguide.domain.model.AnimalInfo
import javax.inject.Inject

data class SaveToPokedexResult(
    val isUpdate: Boolean  // true=更新已有, false=新增
)

class SaveToPokedexUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(
        animalName: String,
        imageUri: String,
        info: AnimalInfo,
        latitude: Double? = null,
        longitude: Double? = null,
        isManual: Boolean = false
    ): SaveToPokedexResult {
        val isUpdate = animalRepository.saveToPokedex(
            animalName = animalName,
            imageUri = imageUri,
            info = info,
            latitude = latitude,
            longitude = longitude,
            isManual = isManual
        )
        // 写入照片墙
        photoRepository.addAnimalPhoto(
            animalName = animalName,
            imageUri = imageUri
        )
        return SaveToPokedexResult(isUpdate = isUpdate)
    }
}