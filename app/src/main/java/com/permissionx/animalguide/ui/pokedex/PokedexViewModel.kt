package com.permissionx.animalguide.ui.pokedex

import androidx.lifecycle.ViewModel
import com.permissionx.animalguide.data.repository.AnimalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val repository: AnimalRepository
) : ViewModel() {
    val animals = repository.getAllAnimals()
    val animalCount = repository.getAnimalCount()
}