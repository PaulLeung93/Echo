package com.example.echo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.echo.ui.theme.EchoTheme

/**
 * A placeholder that mirrors [PostCard]'s layout while the feed loads — a shimmer
 * avatar, author + meta lines, two message lines, and a footer. Shown a few at a
 * time it reads as "content is coming," which feels faster than a bare spinner.
 */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.width(12.dp))
                Column {
                    ShimmerBar(brush, height = 14.dp, width = 120.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(brush, height = 12.dp, width = 80.dp)
                }
            }
            Spacer(Modifier.height(16.dp))
            ShimmerBar(brush, height = 14.dp)
            Spacer(Modifier.height(8.dp))
            ShimmerBar(brush, height = 14.dp, fraction = 0.6f)
            Spacer(Modifier.height(16.dp))
            Row {
                ShimmerBar(brush, height = 14.dp, width = 36.dp)
                Spacer(Modifier.width(20.dp))
                ShimmerBar(brush, height = 14.dp, width = 36.dp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPostCardSkeleton() {
    EchoTheme {
        PostCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}
