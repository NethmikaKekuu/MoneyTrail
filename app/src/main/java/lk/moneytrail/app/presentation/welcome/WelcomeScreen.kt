package lk.moneytrail.app.presentation.welcome

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import lk.moneytrail.app.R
import lk.moneytrail.app.presentation.auth.AuthPrimaryButton
import lk.moneytrail.app.presentation.auth.AuthSecondaryButton
import lk.moneytrail.app.presentation.components.MoneyTrailLogo
import lk.moneytrail.app.presentation.theme.MoneyTrailTheme
import lk.moneytrail.app.presentation.theme.Rust

// MoneyTrail-Figma-HTML/02-Welcome.html. The design only fills in the first slide;
// the other two preview the Folders and Income insights screens.
private data class Slide(val title: String, val message: String)

private val Slides = listOf(
    Slide(
        title = "Every day, on one calendar.",
        message = "Log income and spending in three taps and add a quick note so you remember what it was for."
    ),
    Slide(
        title = "Every rupee in its folder.",
        message = "Entries sort themselves into folders by category, so you can see where the month went at a glance."
    ),
    Slide(
        title = "Know what you really earn.",
        message = "Salary, freelance, AdSense and crypto side by side, with confirmed money kept apart from what you're still expecting."
    )
)

@Composable
fun WelcomeScreen(
    onCreateAccountClick: () -> Unit,
    onLogInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(pageCount = { Slides.size })
    val scope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage == Slides.lastIndex
    val showSlide: (Int) -> Unit = { page -> scope.launch { pagerState.animateScrollToPage(page) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 20.dp, end = 8.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoneyTrailLogo(size = 30.dp)
                Text("MoneyTrail", style = MaterialTheme.typography.titleMedium)
            }
            if (!isLastSlide) {
                TextButton(onClick = { showSlide(Slides.lastIndex) }) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge, fontSize = 14.sp)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            SlidePage(page)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Slides.indices.forEach { index ->
                    SlideDot(
                        index = index,
                        isSelected = index == pagerState.currentPage,
                        onClick = { showSlide(index) }
                    )
                }
            }
            if (!isLastSlide) {
                OutlinedIconButton(
                    onClick = { showSlide(pagerState.currentPage + 1) },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, colors.outlineVariant),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = "Next slide",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuthPrimaryButton(text = "Create account", onClick = onCreateAccountClick)
            AuthSecondaryButton(text = "I already have an account", onClick = onLogInClick)
        }
    }
}

@Composable
private fun SlidePage(page: Int) {
    val slide = Slides[page]
    Column(Modifier.fillMaxSize()) {
        // Up to 360dp tall as in the design, shrinking on short screens.
        Box(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 360.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> CalendarIllustration()
                1 -> FoldersIllustration()
                else -> IncomeIllustration()
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 26.dp)
                .heightIn(min = 150.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(slide.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = slide.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SlideDot(index: Int, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dotWidth")
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Slide ${index + 1}" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = width, height = 8.dp)
                .background(
                    color = if (isSelected) colors.primary else colors.primary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

/** White card floating on the cream panel, holding a mini preview of an app screen. */
@Composable
private fun PreviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .width(250.dp)
            .shadow(16.dp, shape, ambientColor = Rust.copy(alpha = 0.12f), spotColor = Rust.copy(alpha = 0.12f))
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun NoteChip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class DayMark { NONE, LOGGED, BUSY, TODAY, NOTED }

// Which days of the sample month have entries, as drawn in the design.
private val SampleDays = mapOf(
    2 to DayMark.LOGGED, 5 to DayMark.BUSY, 7 to DayMark.LOGGED,
    8 to DayMark.BUSY, 10 to DayMark.TODAY, 14 to DayMark.NOTED
)

@Composable
private fun CalendarIllustration() {
    PreviewCard(title = "October") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (week in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (weekday in 1..7) {
                        val day = week * 7 + weekday
                        DayCell(day, SampleDays[day] ?: DayMark.NONE, Modifier.weight(1f))
                    }
                }
            }
        }
        NoteChip("Team lunch · 2,400 · “farewell”")
    }
}

@Composable
private fun DayCell(day: Int, mark: DayMark, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val (background, text) = when (mark) {
        DayMark.NONE -> Color.Transparent to colors.outline
        DayMark.LOGGED -> colors.primaryContainer to colors.onPrimaryContainer
        DayMark.BUSY -> colors.secondary to colors.onSecondary
        DayMark.TODAY -> colors.primary to colors.onPrimary
        DayMark.NOTED -> Color.Transparent to colors.secondary
    }
    Box(
        modifier = modifier
            .height(26.dp)
            .background(background, shape)
            .then(if (mark == DayMark.NOTED) Modifier.border(1.5.dp, colors.secondary, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = text
        )
    }
}

@Composable
private fun FoldersIllustration() {
    PreviewCard(title = "Expenses · October") {
        AmountRow("Rent", "34,000", 1f)
        AmountRow("Groceries", "6,850", 0.2f)
        AmountRow("Coffee", "2,950", 0.09f)
        AmountRow("Rides", "1,730", 0.05f)
        NoteChip("12 entries · 65,580 so far")
    }
}

@Composable
private fun IncomeIllustration() {
    PreviewCard(title = "Income · September") {
        AmountRow("Salary", "133,400", 1f)
        AmountRow("Freelance", "35,000", 0.26f)
        AmountRow("AdSense (USD)", "12,600", 0.09f)
        NoteChip("Crypto kept out of savings plans")
    }
}

/** A labelled amount with a bar showing its share of the biggest one. */
@Composable
private fun AmountRow(label: String, amount: String, fraction: Float) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.primaryContainer, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.primary, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 844)
@Composable
private fun WelcomeScreenPreview() {
    MoneyTrailTheme {
        WelcomeScreen(onCreateAccountClick = {}, onLogInClick = {})
    }
}
