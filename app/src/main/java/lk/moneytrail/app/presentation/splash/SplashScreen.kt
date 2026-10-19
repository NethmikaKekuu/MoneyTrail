package lk.moneytrail.app.presentation.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.moneytrail.app.presentation.components.MoneyTrailLogo
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme

// MoneyTrail-Figma-HTML/01-Splash.html
@Composable
fun SplashScreen(
    onFinished: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let(onFinished)
    }

    SplashContent(modifier)
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "logoPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primary),
        contentAlignment = Alignment.Center
    ) {
        // Rings centred behind the logo, as in the design.
        Canvas(Modifier.fillMaxSize()) {
            val ring = Stroke(width = 2.dp.toPx())
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 140.dp.toPx())
            drawCircle(Color.White.copy(alpha = 0.10f), radius = 220.dp.toPx(), style = ring)
            drawCircle(Color.White.copy(alpha = 0.10f), radius = 300.dp.toPx(), style = ring)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            MoneyTrailLogo(size = 112.dp, reversed = true, modifier = Modifier.scale(pulse))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("MoneyTrail", style = MaterialTheme.typography.displaySmall, color = colors.onPrimary)
                Text(
                    text = "Follow every rupee.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                color = colors.onPrimary,
                trackColor = Color.Black.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                modifier = Modifier
                    .width(140.dp)
                    .height(5.dp)
            )
            Text(
                text = "Loading your ledger…",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colors.onPrimary
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    MoneyTrailTheme {
        SplashContent()
    }
}
