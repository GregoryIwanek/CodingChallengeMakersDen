package pl.gi.codingchallenge.feature.detail

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Type-safe nav route. The whole item rides along as JSON (SearchResultItem
 * is already @Serializable), so the screen needs no repository lookup and
 * the route survives process death. internal: callers use [navigateToDetail].
 */
@Serializable
internal data class DetailRoute(val itemJson: String)

internal fun DetailRoute(item: SearchResultItem): DetailRoute =
    DetailRoute(itemJson = Json.encodeToString(SearchResultItem.serializer(), item))

internal fun DetailRoute.toItem(): SearchResultItem =
    Json.decodeFromString(SearchResultItem.serializer(), itemJson)
