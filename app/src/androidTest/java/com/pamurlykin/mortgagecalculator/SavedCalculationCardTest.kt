package com.pamurlykin.mortgagecalculator

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.ui.screens.CalculationCard
import com.pamurlykin.mortgagecalculator.ui.screens.CalculationSummaryCard
import com.pamurlykin.mortgagecalculator.ui.screens.createCalculationShareIntent
import com.pamurlykin.mortgagecalculator.ui.screens.listBackground
import com.pamurlykin.mortgagecalculator.ui.theme.MortgageCalculatorTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SavedCalculationCardTest {
    @get:Rule val compose = createComposeRule()

    private val ordinary = MortgageEntity(
        id = 1, propertyValue = 6_600_000.0, downPayment = 1_320_000.0,
        termYears = 30, interestRate = 12.0, isAnnuity = true, title = "Сохраненный расчет"
    )
    private val grace = ordinary.copy(
        propertyValue = 7_000_000.0, discountAmount = 350_000.0, showDiscount = true,
        isMarkup = true, interestRate = 12.5, showGracePeriod = true,
        gracePeriodMonths = 12, graceInterestRate = 1.5
    )

    @Test
    fun cardUsesActiveGraceTermAndAllThreePriceBadges() {
        val current = mutableStateOf(ordinary.copy(showGracePeriod = true, gracePeriodMonths = 0))
        compose.setContent { MortgageCalculatorTheme(false) { CalculationSummaryCard(current.value) } }
        compose.onNodeWithText("Льготный период").assertDoesNotExist()
        compose.onAllNodesWithText("Платеж").assertCountEquals(1)
        compose.onNodeWithText("Без удорожания").assertIsDisplayed()

        compose.runOnIdle { current.value = grace.copy(showGracePeriod = false) }
        compose.onNodeWithText("Льготный период").assertDoesNotExist()
        compose.onNodeWithText("С удорожанием 5 %").assertIsDisplayed()

        compose.runOnIdle { current.value = grace }
        compose.onNodeWithText("Льготный период").assertIsDisplayed()
        compose.onNodeWithText("Основной период").assertIsDisplayed()
        compose.onAllNodesWithText("Платеж").assertCountEquals(2)
        compose.onNodeWithText("1 год").assertIsDisplayed()
        compose.onNodeWithText("30 лет").assertIsDisplayed()
        compose.onNodeWithText("7 350 000 ₽").assertIsDisplayed()

        compose.runOnIdle { current.value = ordinary.copy(showDiscount = true, discountAmount = 330_000.0) }
        compose.onNodeWithText("Со скидкой 5 %").assertIsDisplayed()
        compose.onNodeWithText("6 270 000 ₽").assertIsDisplayed()
    }

    @Test
    fun shareContainsReadableFullPngWithTemporaryReadPermission() = runBlocking {
        lateinit var host: ViewGroup
        lateinit var output: File
        compose.setContent {
            host = LocalView.current.rootView as ViewGroup
            output = File(LocalContext.current.getExternalFilesDir(null), "list-design").apply { mkdirs() }
            MortgageCalculatorTheme(false) { CalculationSummaryCard(ordinary) }
        }
        compose.waitForIdle()
        val cases = listOf(
            "ordinary" to ordinary,
            "grace" to grace,
            "zero-grace-discount" to ordinary.copy(showGracePeriod = true, gracePeriodMonths = 0, showDiscount = true, discountAmount = 330_000.0),
            "whole-term-grace" to grace.copy(termMonths = 12, termYears = 1, gracePeriodMonths = 12)
        )
        val heights = mutableListOf<Int>()
        for ((name, calculation) in cases) {
            val intent = createCalculationShareIntent(host, calculation)
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("image/png", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertEquals("content", uri.scheme)
            assertEquals("com.pamurlykin.mortgagecalculator.calculation-images", uri.authority)
            assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
            val bytes = host.context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            File(output, "$name.png").writeBytes(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
            assertEquals(1260, bitmap.width)
            assertTrue(bitmap.height > 600)
            // A blank or transparent view is not a screenshot of the calculation.
            val colors = mutableSetOf<Int>()
            for (y in 0 until bitmap.height step 8) for (x in 0 until bitmap.width step 8) colors += bitmap.getPixel(x, y)
            assertTrue("Image must contain the rendered text, tiles and badges", colors.size > 30)
            assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            heights += bitmap.height
            bitmap.recycle()
        }
        assertTrue("Grace image includes both full period panels", heights[1] > heights[0])
    }

    @Test
    fun narrowCardWithLargeTextKeepsActionsAndGraceDetailsAccessible() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                MortgageCalculatorTheme(true) {
                    // A scrollable host allows the accessible layout to exceed the screen.
                    androidx.compose.foundation.lazy.LazyColumn {
                        item {
                            Box(Modifier.width(300.dp).background(listBackground()).padding(8.dp)) {
                                CalculationCard(grace, {}, {}, {}, {})
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Поделиться расчетом").assertIsDisplayed()
        compose.onNodeWithText("Льготный период").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Основной период").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("С удорожанием 5 %").performScrollTo().assertIsDisplayed()
    }
}
