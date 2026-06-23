package dev.echoapp.echo.ui.feed

import dev.echoapp.echo.domain.model.Coordinates
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.usecase.post.DeletePostUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsByTagUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.post.UpdatePostUseCase
import dev.echoapp.echo.domain.usecase.report.SubmitReportUseCase
import dev.echoapp.echo.domain.usecase.user.BlockUserUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import dev.echoapp.echo.ui.common.MapFocusManager
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
    private val deletePostUseCase: DeletePostUseCase = mockk()
    private val updatePostUseCase: UpdatePostUseCase = mockk()
    private val submitReportUseCase: SubmitReportUseCase = mockk()
    private val blockUserUseCase: BlockUserUseCase = mockk()
    private val observeBlockedUserIdsUseCase: ObserveHiddenAuthorIdsUseCase = mockk()
    private val locationProvider: LocationProvider = mockk()
    private val mapFocusManager: MapFocusManager = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        every { getPostsUseCase() } returns flowOf(emptyList())
        every { getPostsUseCase.feed() } returns flowOf(emptyList())
        coEvery { getPostsUseCase.refresh(any()) } returns emptyList()
        every { getPostsByTagUseCase(any()) } returns flowOf(emptyList())
        every { observeBlockedUserIdsUseCase() } returns flowOf(emptySet())
        every { authRepository.getCurrentUser() } returns null
    }

    private fun createViewModel() = FeedViewModel(
        getPostsUseCase = getPostsUseCase,
        getPostsByTagUseCase = getPostsByTagUseCase,
        toggleLikeUseCase = toggleLikeUseCase,
        deletePostUseCase = deletePostUseCase,
        updatePostUseCase = updatePostUseCase,
        submitReportUseCase = submitReportUseCase,
        blockUserUseCase = blockUserUseCase,
        observeHiddenAuthorIdsUseCase = observeBlockedUserIdsUseCase,
        locationProvider = locationProvider,
        mapFocusManager = mapFocusManager,
        authRepository = authRepository
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
