package com.pamurlykin.mortgagecalculator.util

import org.junit.Assert.*
import org.junit.Test

class MortgageCalculatorTest {
    private val loan = 5_280_000.0
    private val tolerance = 0.00001

    @Test
    fun zeroGracePeriodPreservesExistingPaymentsAndIgnoresGraceRate() {
        for (annuity in listOf(true, false)) {
            val expected = MortgageCalculator.calculateMonthlyPayment(loan, 12.0, 360, annuity)
            val actual = MortgageCalculator.calculatePayments(loan, 12.0, 360, annuity, 0, 1.0)
            assertEquals(expected, actual.monthlyPayment, 0.0)
            assertEquals(0.0, actual.graceMonthlyPayment, 0.0)
            val schedule = MortgageCalculator.calculatePaymentSchedule(loan, 12.0, 360, annuity, 0, 99.0)
            assertEquals(expected, schedule.first().paymentAmount, tolerance)
            assertTrue(schedule.none { it.isGracePeriod })
            assertEquals(loan, MortgageCalculator.calculateLoanAmount(expected, 12.0, 360, annuity), tolerance)
        }
    }

    @Test
    fun annuityPaymentChangesAfterGracePeriodUsingRemainingDebtAndTerm() {
        val payments = MortgageCalculator.calculatePayments(1_200_000.0, 12.0, 24, true, 12, 0.0)
        assertEquals(50_000.0, payments.graceMonthlyPayment, tolerance)
        assertEquals(
            MortgageCalculator.calculateMonthlyPayment(600_000.0, 12.0, 12, true),
            payments.monthlyPayment, tolerance
        )
        val schedule = MortgageCalculator.calculatePaymentSchedule(1_200_000.0, 12.0, 24, true, 12, 0.0)
        assertEquals(24, schedule.size)
        assertEquals(600_000.0, schedule[11].remainingBalance, tolerance)
        assertEquals(6_000.0, schedule[12].interestAmount, tolerance)
        assertTrue(schedule.take(12).all { it.isGracePeriod })
        assertTrue(schedule.drop(12).none { it.isGracePeriod })
        assertEquals(payments.monthlyPayment, schedule[12].paymentAmount, tolerance)
        assertEquals(0.0, schedule.last().remainingBalance, tolerance)
    }

    @Test
    fun differentiatedPaymentsKeepPrincipalConstantAcrossRateChange() {
        val payments = MortgageCalculator.calculatePayments(1_200_000.0, 12.0, 24, false, 12, 6.0)
        assertEquals(56_000.0, payments.graceMonthlyPayment, tolerance)
        assertEquals(56_000.0, payments.monthlyPayment, tolerance)
        val schedule = MortgageCalculator.calculatePaymentSchedule(1_200_000.0, 12.0, 24, false, 12, 6.0)
        assertTrue(schedule.all { kotlin.math.abs(it.principalAmount - 50_000.0) < tolerance })
        assertEquals(53_250.0, schedule[11].paymentAmount, tolerance)
        assertEquals(56_000.0, schedule[12].paymentAmount, tolerance)
    }

    @Test
    fun everyScheduleRepaysExactlyThePrincipalAndMatchesSummary() {
        for (annuity in listOf(true, false)) {
            for (months in listOf(1, 13, 360)) {
                for (graceMonths in listOf(0, 1, months / 2, months, months + 1)) {
                    for (rate in listOf(0.0, 0.01, 12.0, 100.0)) {
                        val schedule = MortgageCalculator.calculatePaymentSchedule(loan, rate, months, annuity, graceMonths, 2.0)
                        val payments = MortgageCalculator.calculatePayments(loan, rate, months, annuity, graceMonths, 2.0)
                        assertEquals(months, schedule.size)
                        assertEquals(loan, schedule.sumOf { it.principalAmount }, 0.01)
                        assertEquals(0.0, schedule.last().remainingBalance, 0.01)
                        assertTrue(schedule.all { it.paymentAmount.isFinite() && it.paymentAmount > 0.0 })
                        for (item in schedule) {
                            assertEquals(
                                "annuity=$annuity, term=$months, grace=$graceMonths, rate=$rate, month=${item.monthNumber}",
                                item.paymentAmount, item.principalAmount + item.interestAmount, 0.01
                            )
                        }
                        if (graceMonths > 0) assertEquals(payments.graceMonthlyPayment, schedule.first().paymentAmount, tolerance)
                        if (graceMonths < months) assertEquals(payments.monthlyPayment, schedule[graceMonths].paymentAmount, tolerance)
                    }
                }
            }
        }
    }

    @Test
    fun inverseCalculationUsesFirstRegularPaymentOrGracePaymentForFullTerm() {
        for (annuity in listOf(true, false)) {
            for (graceMonths in listOf(0, 1, 35, 36, 50)) {
                val amount = MortgageCalculator.calculateLoanAmount(50_000.0, 12.0, 36, annuity, graceMonths, 0.0)
                val payments = MortgageCalculator.calculatePayments(amount, 12.0, 36, annuity, graceMonths, 0.0)
                val payment = if (graceMonths >= 36) payments.graceMonthlyPayment else payments.monthlyPayment
                assertEquals(50_000.0, payment, tolerance)
            }
        }
    }

    @Test
    fun fullTermGraceHasNoRegularPaymentsAndIgnoresRegularRate() {
        for (annuity in listOf(true, false)) {
            val payments = MortgageCalculator.calculatePayments(loan, 100.0, 13, annuity, 13, 0.0)
            assertEquals(0.0, payments.monthlyPayment, 0.0)
            assertEquals(loan / 13, payments.graceMonthlyPayment, tolerance)
            val schedule = MortgageCalculator.calculatePaymentSchedule(loan, 100.0, 13, annuity, 13, 0.0)
            assertTrue(schedule.all { it.isGracePeriod && it.interestAmount == 0.0 })
        }
    }

    @Test
    fun identicalRatesPreservePaymentThroughTheBoundary() {
        val payments = MortgageCalculator.calculatePayments(loan, 12.0, 360, true, 25, 12.0)
        assertEquals(payments.graceMonthlyPayment, payments.monthlyPayment, tolerance)
        assertEquals(MortgageCalculator.calculateMonthlyPayment(loan, 12.0, 360, true), payments.monthlyPayment, tolerance)
    }

    @Test
    fun invalidLoanOrTermReturnsEmptyScheduleAndZeroPayments() {
        for ((amount, months) in listOf(0.0 to 360, -1.0 to 360, loan to 0, loan to -1)) {
            assertEquals(MortgagePayments(0.0), MortgageCalculator.calculatePayments(amount, 12.0, months, true, 12, 1.0))
            assertTrue(MortgageCalculator.calculatePaymentSchedule(amount, 12.0, months, true, 12, 1.0).isEmpty())
        }
        assertEquals(0.0, MortgageCalculator.calculateLoanAmount(0.0, 12.0, 360, true, 12, 1.0), 0.0)
    }
}
