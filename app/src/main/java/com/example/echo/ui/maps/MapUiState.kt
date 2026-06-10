package com.example.echo.ui.maps

import com.example.echo.domain.model.Poi
import com.example.echo.domain.model.Post
import com.google.android.gms.maps.model.LatLng

/**
 * Represents a group of nearby posts (clustered by location).
 */
data class ClusterGroup(
    val position: LatLng,
    val posts: List<Post>
)

/**
 * UI State for the Map screen.
 */
data class MapUiState(
    val posts: List<Post> = emptyList(),
    val pois: List<Poi> = emptyList(),
    val clusters: List<ClusterGroup> = emptyList(),
    val selectedPost: Post? = null,
    val selectedCluster: ClusterGroup? = null,
    val selectedPoi: Poi? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTag: String? = null,
    val activeFilters: Set<String> = setOf("user posts", "landmark", "park", "college")
)
