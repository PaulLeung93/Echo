package com.example.echo.ui.map

import android.location.Location.distanceBetween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.model.Poi
import com.example.echo.domain.model.Post
import com.example.echo.domain.usecase.poi.GetPoisUseCase
import com.example.echo.domain.usecase.post.GetPostsUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getPoisUseCase: GetPoisUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase
) : ViewModel() {

    private val _currentTag = MutableStateFlow<String?>(null)
    private val _activeFilters = MutableStateFlow(setOf("user posts", "landmark", "park", "college"))
    private val _selectedPost = MutableStateFlow<Post?>(null)
    private val _selectedCluster = MutableStateFlow<ClusterGroup?>(null)
    private val _selectedPoiId = MutableStateFlow<String?>(null)
    private val _currentZoom = MutableStateFlow(12f)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MapUiState> = combine(
        getPostsUseCase(),
        getPoisUseCase(),
        _currentTag,
        _activeFilters,
        _selectedPost,
        _selectedCluster,
        _selectedPoiId,
        _currentZoom
    ) { args: Array<Any?> ->
        val posts = args[0] as List<Post>
        val pois = args[1] as List<Poi>
        val tag = args[2] as String?
        val filters = args[3] as Set<String>
        val selected = args[4] as Post?
        val selectedCluster = args[5] as ClusterGroup?
        val selectedPoiId = args[6] as String?
        val zoom = args[7] as Float

        val filteredPosts = if ("user posts" in filters) {
            posts.filter { post ->
                post.latitude != null && post.longitude != null &&
                (tag == null || post.tags.any { it.equals(tag, ignoreCase = true) })
            }
        } else {
            emptyList()
        }

        val filteredPois = pois.filter { it.type.lowercase() in filters }
        val selectedPoi = filteredPois.find { it.id == selectedPoiId }
        
        MapUiState(
            posts = filteredPosts,
            pois = filteredPois,
            clusters = clusterPosts(filteredPosts, zoom),
            selectedPost = selected,
            selectedCluster = selectedCluster,
            selectedPoi = selectedPoi,
            currentTag = tag,
            activeFilters = filters,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState(isLoading = true)
    )

    fun setTagFilter(tag: String, camera: CameraPositionState?) {
        _currentTag.value = tag
        _selectedPost.value = null
        _selectedPoiId.value = null
        
        viewModelScope.launch {
            val state = uiState.value
            if (camera != null && state.posts.isNotEmpty()) {
                val cameraLatLng = camera.position.target
                val nearest = state.posts.minByOrNull {
                    distanceBetween(cameraLatLng, LatLng(it.latitude!!, it.longitude!!))
                }

                nearest?.let {
                    camera.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude!!, it.longitude!!),
                            camera.position.zoom
                        )
                    )
                }
            }
        }
    }

    fun clearTagFilter() {
        _currentTag.value = null
        _selectedPost.value = null
        _selectedPoiId.value = null
    }

    fun filterByMarkerTypes(types: Set<String>) {
        _activeFilters.value = types
    }

    fun updateZoom(zoom: Float) {
        _currentZoom.value = zoom
    }

    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post
        _selectedCluster.value = null
        _selectedPoiId.value = null
        val latLng = LatLng(post.latitude ?: return, post.longitude ?: return)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    fun setSelectedPoi(poi: Poi, camera: CameraPositionState) {
        _selectedPoiId.value = poi.id
        _selectedPost.value = null
        _selectedCluster.value = null
        val latLng = LatLng(poi.latitude, poi.longitude)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    fun clearSelectedPost() {
        _selectedPost.value = null
        _selectedCluster.value = null
        _selectedPoiId.value = null
    }

    fun onClusterClick(cluster: ClusterGroup, camera: CameraPositionState) {
        _selectedCluster.value = cluster
        _selectedPost.value = cluster.posts.firstOrNull()
        _selectedPoiId.value = null
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(cluster.position))
        }
    }

    fun onSelectedPostChanged(post: Post) {
        _selectedPost.value = post
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                toggleLikeUseCase(postId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    private fun clusterPosts(posts: List<Post>, zoom: Float): List<ClusterGroup> {
        val radius = when {
            zoom >= 17 -> 0f
            zoom >= 15 -> 60f
            zoom >= 13 -> 120f
            else -> 200f
        }

        if (radius == 0f) {
            return posts.map {
                ClusterGroup(LatLng(it.latitude!!, it.longitude!!), listOf(it))
            }
        }

        val clusters = mutableListOf<ClusterGroup>()
        val used = mutableSetOf<Post>()

        for (post in posts) {
            if (post in used || post.latitude == null || post.longitude == null) continue

            val cluster = mutableListOf(post)
            val latLng = LatLng(post.latitude, post.longitude)
            used.add(post)

            for (other in posts) {
                if (other in used || other.latitude == null || other.longitude == null) continue
                val otherLatLng = LatLng(other.latitude, other.longitude)
                if (distanceBetween(latLng, otherLatLng) <= radius) {
                    cluster.add(other)
                    used.add(other)
                }
            }

            clusters.add(ClusterGroup(latLng, cluster))
        }

        return clusters
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }
}
