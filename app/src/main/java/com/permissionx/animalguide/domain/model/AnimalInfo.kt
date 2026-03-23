package com.permissionx.animalguide.domain.model

data class AnimalInfo(
    val name: String,
    val scientificName: String,
    val habitat: String,
    val diet: String,
    val lifespan: String,
    val conservationStatus: String,
    val description: String
)