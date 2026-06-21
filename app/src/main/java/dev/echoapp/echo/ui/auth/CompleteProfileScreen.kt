package dev.echoapp.echo.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.echoapp.echo.domain.usecase.user.UsernameStatus
import dev.echoapp.echo.navigation.Destinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    viewModel: CompleteProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Complete your profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Escape hatch: this screen can be the start destination for a
                    // signed-in user without a profile (e.g. a first-time Google
                    // sign-in, or an account that never finished setup), so without
                    // this there's no way out. "Cancel" discards the provisional
                    // account rather than just signing out, so it doesn't linger as
                    // an orphan — then drops back to Sign In (see RootNavHost).
                    TextButton(onClick = { authViewModel.cancelProfileSetup() }) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tell your neighbors who you are.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
            InitialsAvatar(firstName = state.firstName, lastName = state.lastName)
            Spacer(Modifier.height(24.dp))

            LabeledField(label = "First name") {
                FilledField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    placeholder = "How should we call you?",
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            }

            Spacer(Modifier.height(20.dp))
            LabeledField(label = "Last name") {
                FilledField(
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    placeholder = "Your family name",
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            }

            Spacer(Modifier.height(20.dp))
            LabeledField(label = "Username") {
                val usernameError = state.usernameStatus == UsernameStatus.Taken ||
                    state.usernameStatus == UsernameStatus.Invalid
                TextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = echoFieldColors(),
                    prefix = { Text("@") },
                    trailingIcon = { UsernameTrailing(state) },
                    isError = usernameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dynamic helper line: carries the reason for Taken / Invalid /
                // Error, otherwise the format hint.
                val helperText = when (state.usernameStatus) {
                    UsernameStatus.Taken -> "That username is taken — try another."
                    UsernameStatus.Invalid -> "Use 3–20 letters, numbers or underscores (no spaces)."
                    UsernameStatus.Error -> "Couldn't check availability. Check your connection and try again."
                    else -> "3–20 characters, letters, numbers and underscores. This is how neighbors will see you."
                }
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (usernameError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
            Button(
                onClick = {
                    viewModel.submit {
                        authViewModel.onProfileCompleted()
                        navController.navigate(Destinations.FEED) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                },
                enabled = state.canSubmit,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Create profile", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = buildAnnotatedString {
                    append("By joining, you agree to our ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("Community Guidelines") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Filled, borderless input on a warm peach container — matches the wireframe fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun echoFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outlineVariant
)

/** A field with its label sitting above it, as in the wireframe. */
@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        content()
    }
}

/** Standard text field shared by the name inputs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    imeAction: ImeAction
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = echoFieldColors(),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Coral circle with up to two initials, derived from the name as it's typed.
 * Wrapped in expanding "echo" ripple rings with a floating edit badge, matching
 * the wireframe. The badge is decorative for now (no avatar upload yet).
 */
@Composable
private fun InitialsAvatar(firstName: String, lastName: String) {
    val initials = buildString {
        firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifEmpty { "?" }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        EchoRipples(Modifier.fillMaxSize())

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp,
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(128.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 44.dp, y = 44.dp)
                .size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit photo",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Three expanding rings behind the avatar — the "echo" motif from the wireframe. */
@Composable
private fun EchoRipples(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "ripple")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "phase"
    )
    val color = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier) {
        val maxRadius = size.minDimension / 2f
        val minRadius = maxRadius * 0.4f
        repeat(3) { i ->
            val p = (phase + i / 3f) % 1f
            drawCircle(
                color = color,
                radius = minRadius + (maxRadius - minRadius) * p,
                alpha = (1f - p) * 0.5f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/** Right-side availability indicator for the username field. */
@Composable
private fun UsernameTrailing(state: CompleteProfileViewModel.State) {
    when {
        state.isCheckingUsername -> CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        state.usernameStatus == UsernameStatus.Available -> Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Available",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
        }
        // Taken / Invalid: red cue; the helper line below explains why.
        state.usernameStatus == UsernameStatus.Taken ||
            state.usernameStatus == UsernameStatus.Invalid -> Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = "Unavailable",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        else -> {}
    }
}
