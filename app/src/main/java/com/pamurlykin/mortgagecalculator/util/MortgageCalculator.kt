package com.pamurlykin.mortgagecalculator.util

import java.util.Calendar
import kotlin.math.pow

data class PaymentItem(
    val monthNumber: Int,
    val paymentAmount: Double,
    val principalAmount: Double,
    val interestAmount: Double,
    val remainingBalance: Double,
    val date: Calendar,
    val isGracePeriod: Boolean = false
)

data class MortgagePayments(
    val monthlyPayment: Double,
    val graceMonthlyPayment: Double = 0.0
)

object MortgageCalculator {

    fun calculateFinalPropertyValue(
        propertyValue: Double,
        discountAmount: Double,
        isMarkup: Boolean,
        showDiscount: Boolean
    ): Double {
        if (!showDiscount) return propertyValue
        return if (isMarkup) propertyValue + discountAmount 
        else (propertyValue - discountAmount).coerceAtLeast(0.0)
    }

    fun calculateMonthlyPayment(
        loanAmount: Double,
        interestRate: Double,
        termMonths: Int,
        isAnnuity: Boolean
    ): Double {
        if (loanAmount <= 0 || termMonths <= 0) return 0.0
        val monthlyRate = interestRate / 100 / 12
        return if (isAnnuity) {
            if (monthlyRate == 0.0) loanAmount / termMonths
            else loanAmount * (monthlyRate * (1 + monthlyRate).pow(termMonths)) / ((1 + monthlyRate).pow(termMonths) - 1)
        } else {
            // Для дифференцированного платежа обычно показывают первый (максимальный) платеж
            (loanAmount / termMonths) + (loanAmount * monthlyRate)
        }
    }

    fun calculateLoanAmount(
        monthlyPayment: Double,
        interestRate: Double,
        termMonths: Int,
        isAnnuity: Boolean,
        gracePeriodMonths: Int = 0,
        graceInterestRate: Double = 0.0
    ): Double {
        if (monthlyPayment <= 0 || termMonths <= 0) return 0.0
        if (gracePeriodMonths > 0) {
            val payments = calculatePayments(1.0, interestRate, termMonths, isAnnuity, gracePeriodMonths, graceInterestRate)
            val paymentPerUnit = if (gracePeriodMonths >= termMonths) payments.graceMonthlyPayment else payments.monthlyPayment
            return monthlyPayment / paymentPerUnit
        }
        val monthlyRate = interestRate / 100 / 12
        return if (isAnnuity) {
            if (monthlyRate == 0.0) monthlyPayment * termMonths
            else monthlyPayment * ((1 + monthlyRate).pow(termMonths) - 1) / (monthlyRate * (1 + monthlyRate).pow(termMonths))
        } else {
            monthlyPayment / ((1.0 / termMonths) + monthlyRate)
        }
    }

    fun calculatePayments(
        loanAmount: Double,
        interestRate: Double,
        termMonths: Int,
        isAnnuity: Boolean,
        gracePeriodMonths: Int = 0,
        graceInterestRate: Double = 0.0
    ): MortgagePayments {
        if (loanAmount <= 0 || termMonths <= 0) return MortgagePayments(0.0)
        val graceMonths = gracePeriodMonths.coerceIn(0, termMonths)
        if (graceMonths == 0) {
            return MortgagePayments(calculateMonthlyPayment(loanAmount, interestRate, termMonths, isAnnuity))
        }

        val gracePayment = calculateMonthlyPayment(loanAmount, graceInterestRate, termMonths, isAnnuity)
        val remainingMonths = termMonths - graceMonths
        val balance = if (isAnnuity) {
            calculateLoanAmount(gracePayment, graceInterestRate, remainingMonths, true)
        } else {
            loanAmount * remainingMonths / termMonths
        }
        val regularPayment = calculateMonthlyPayment(balance, interestRate, remainingMonths, isAnnuity)
        return MortgagePayments(regularPayment, gracePayment)
    }

    fun calculatePaymentSchedule(
        loanAmount: Double,
        interestRate: Double,
        totalMonths: Int,
        isAnnuity: Boolean,
        gracePeriodMonths: Int = 0,
        graceInterestRate: Double = 0.0
    ): List<PaymentItem> {
        val scheduleList = mutableListOf<PaymentItem>()
        if (loanAmount <= 0 || totalMonths <= 0) return scheduleList

        val graceMonths = gracePeriodMonths.coerceIn(0, totalMonths)
        val payments = calculatePayments(loanAmount, interestRate, totalMonths, isAnnuity, graceMonths, graceInterestRate)
        var remainingBalanceValue = loanAmount
        val startDate = Calendar.getInstance()
        val fixedPrincipalPart = loanAmount / totalMonths
        for (month in 1..totalMonths) {
            val isGracePeriod = month <= graceMonths
            val monthlyRate = (if (isGracePeriod) graceInterestRate else interestRate) / 100 / 12
            val interestPart = remainingBalanceValue * monthlyRate
            val payment = if (isAnnuity) {
                if (isGracePeriod) payments.graceMonthlyPayment else payments.monthlyPayment
            } else {
                fixedPrincipalPart + interestPart
            }
            // Present value of the remaining payments avoids accumulating floating-point
            // error over long terms, especially near the upper interest-rate limit.
            val nextBalance = if (isAnnuity) {
                calculateLoanAmount(payment, if (isGracePeriod) graceInterestRate else interestRate, totalMonths - month, true)
            } else {
                loanAmount * (totalMonths - month) / totalMonths
            }.coerceIn(0.0, remainingBalanceValue)
            val principalPart = remainingBalanceValue - nextBalance
            remainingBalanceValue = nextBalance
            val paymentDate = (startDate.clone() as Calendar).apply { add(Calendar.MONTH, month) }
            scheduleList.add(
                PaymentItem(month, payment, principalPart, interestPart, remainingBalanceValue.coerceAtLeast(0.0), paymentDate, isGracePeriod)
            )
        }
        return scheduleList
    }
}
