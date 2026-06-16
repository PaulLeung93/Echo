package com.example.echo.ui.feed

import com.example.echo.domain.model.Coordinates
import com.example.echo.domain.repository.LocationProvider
import com.example.echo.domain.usecase.post.GetPostsUseCase
import com.example.echo.domain.usecase.post.GetPostsByTagUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.example.echo.domain.usecase.report.SubmitReportUseCase
import com.example.echo.domain.usecase.user.BlockUserUseCase
import com.example.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import com.google.firebase.auth.FirebaseAuth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val getPostsUseCase: GetPostsUseCase = mockk()
    private val getPostsByTagUseCase: GetPostsByTagUseCase = mockk()
    private val toggleLikeUseCase: ToggleLikeUseCase = mockk()
    private val submitReportUseCase: SubmitReportUseCase = mockk()
    private val blockUserUseCase: BlockUserUseCase = mockk()
    private val observeBlockedUserIdsUseCase: ObserveHiddenAuthorIdsUseCase = mockk()
    private val locationProvider: LocationProvider = mockk()
    private val auth: FirebaseAuth = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        every { getPostsUseCase() } returns flowOf(emptyList())
        coEvery { getPostsUseCase.page(any(), any()) } returns emptyList()
        every { getPostsByTagUseCase(any()) } returns flowOf(emptyList())
        every { observeBlockedUserIdsUseCase() } returns flowOf(emptySet())
        every { auth.currentUser } returns null
    }

    private fun createViewModel() = FeedViewModel(
        getPostsUseCase,
        getPostsByTagUseCase,
        toggleLikeUseCase,
        submitReportUseCase,
        blockUserUseCase,
        observeBlockedUserIdsUseCase,
        locationProvider,
        auth
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches coordinates and neighborhood name successfully`() = runTest {
        val expectedCoords = Coordinates(40.7128, -74.0060)
        coEvery { locationProvider.getCurrentCoordinates() } returns expectedCoords
        coEvery { locationProvider.getNeighborhoodName(expectedCoords) } returns "Financial District"

        val viewModel = createViewModel()

        assertEquals(expectedCoords, viewModel.userCoordinates.value)
        assertEquals("Financial District", viewModel.neighborhoodName.value)
    }

    @Test
    fun `init handles null coordinates and leaves neighborhood null`() = runTest {
        coEvery { locationProvider.getCurrentCoordinates() } returns null

        val viewModel = createViewModel()

        assertEquals(null, viewModel.userCoordinates.value)
        assertEquals(null, viewModel.neighborhoodName.value)
    }
}
