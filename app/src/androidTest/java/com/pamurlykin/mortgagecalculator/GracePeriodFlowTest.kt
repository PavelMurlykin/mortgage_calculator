package com.pamurlykin.mortgagecalculator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GracePeriodFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun enableValidateSaveAndRestoreGracePeriod() {
        lateinit var model: MortgageViewModel
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[MortgageViewModel::class.java]
        }
        compose.waitUntil(10_000) { model.finalPropertyValue.value > 0.0 }
        compose.runOnIdle {
            model.updateTermMonths(25)
            model.updateGracePeriodMonths(1000)
            assertEquals(25, model.gracePeriodMonths.value)
            model.updateTermMonths(10)
            assertEquals(10, model.gracePeriodMonths.value)
            model.updateGracePeriodYears(1000)
            assertEquals(10, model.gracePeriodMonths.value)
            model.updateTermYears(0)
            assertEquals(0, model.gracePeriodMonths.value)
            model.updateGracePeriodMonths(-1)
            assertEquals(0, model.gracePeriodMonths.value)
            model.updateGraceInterestRate(-1.0)
            assertEquals(0.0, model.graceInterestRate.value, 0.0)
            model.updateGraceInterestRate(101.0)
            assertEquals(100.0, model.graceInterestRate.value, 0.0)
            model.updateTermMonths(25)
            model.updateGracePeriodMonths(13)
            model.updateGraceInterestRate(0.0)
            model.updateShowGracePeriodOption(false)
            model.updateAdditionalGroupExpanded(true)
        }
        compose.waitUntil(10_000) { !model.showGracePeriodOption.value && model.isAdditionalGroupExpanded.value }
        compose.onNodeWithText("Срок льготного периода").assertDoesNotExist()
        compose.onNodeWithText("Настройки").performClick()
        compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
        compose.waitUntil(10_000) { model.showGracePeriodOption.value }
        compose.onNode(hasText("Расчет") and isSelectable()).performClick()
        compose.onNodeWithText("Ежемесячный льготный платеж").assertIsDisplayed()
        compose.onNodeWithText("Льготная процентная ставка").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Сохраненный расчет").performScrollTo().performClick()
        compose.onNodeWithText("Список").performClick()
        compose.waitUntil(10_000) { model.savedCalculations.value.firstOrNull()?.gracePeriodMonths == 13 }
        val saved = model.savedCalculations.value.first()
        assertEquals(25, saved.termMonths)
        assertEquals(13, saved.gracePeriodMonths)
        assertTrue(saved.showGracePeriod)
        assertEquals(0.0, saved.graceInterestRate, 0.0)
        compose.onAllNodesWithText("Льготный период").onFirst().assertIsDisplayed()
        compose.runOnIdle {
            model.updateTermMonths(360)
            model.updateGracePeriodMonths(0)
            model.updateGraceInterestRate(99.0)
        }
        compose.onAllNodesWithContentDescription("Редактировать").onFirst().performClick()
        compose.waitUntil(10_000) { model.termMonths.value == 25 && model.gracePeriodMonths.value == 13 }
        assertEquals(0.0, model.graceInterestRate.value, 0.0)
        assertTrue(model.showGracePeriodOption.value)
        compose.onNodeWithText("График >").performClick()
        compose.onAllNodesWithText("Льготный").onFirst().assertIsDisplayed()
    }
}
