package pl.gi.codingchallenge.shared.catfact

import kotlinx.serialization.Serializable

@Serializable
data class CatFact(
    val fact: String,
    val length: Int,
)
