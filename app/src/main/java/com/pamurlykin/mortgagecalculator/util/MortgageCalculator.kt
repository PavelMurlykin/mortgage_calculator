package com.pamurlykin.mortgagecalculator.util

import java.util.Calendar
import kotlin.math.pow

data class PaymentItem(
    val monthNumber: Int,
    val paymentAmount: Double,
    val principalAmount: Double,
    val interestAmount: Double,
    val remainingBalance: Double,
    val date: Calendar
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
        isAnnuity: Boolean
    ): Double {
        if (monthlyPayment <= 0 || termMonths <= 0) return 0.0
        val monthlyRate = interestRate / 100 / 12
        return if (isAnnuity) {
            if (monthlyRate == 0.0) monthlyPayment * termMonths
            else monthlyPayment * ((1 + monthlyRate).pow(termMonths) - 1) / (monthlyRate * (1 + monthlyRate).pow(termMonths))
        } else {
            monthlyPayment / ((1.0 / termMonths) + monthlyRate)
        }
    }

    fun calculatePaymentSchedule(
        loanAmount: Double,
        interestRate: Double,
        totalMonths: Int,
        isAnnuity: Boolean
    ): List<PaymentItem> {
        val scheduleList = mutableListOf<PaymentItem>()
        if (loanAmount <= 0 || totalMonths <= 0) return scheduleList

        val monthlyInterestRate = interestRate / 100 / 12
        var remainingBalanceValue = loanAmount
        val startDate = Calendar.getInstance()

        if (isAnnuity) {
            val annuityPayment = if (monthlyInterestRate == 0.0) {
                loanAmount / totalMonths
            } else {
                loanAmount * (monthlyInterestRate * (1 + monthlyInterestRate).pow(totalMonths)) /
                        ((1 + monthlyInterestRate).pow(totalMonths) - 1)
            }

            for (month in 1..totalMonths) {
                val interestPart = remainingBalanceValue * monthlyInterestRate
                val principalPart = annuityPayment - interestPart
                remainingBalanceValue -= principalPart
                val paymentDate = (startDate.clone() as Calendar).apply { add(Calendar.MONTH, month) }

                scheduleList.add(
                    PaymentItem(month, annuityPayment, principalPart, interestPart, remainingBalanceValue.coerceAtLeast(0.0), paymentDate)
                )
            }
        } else {
            val fixedPrincipalPart = loanAmount / totalMonths
            for (month in 1..totalMonths) {
                val interestPart = remainingBalanceValue * monthlyInterestRate
                val currentMonthlyPayment = fixedPrincipalPart + interestPart
                remainingBalanceValue -= fixedPrincipalPart
                val paymentDate = (startDate.clone() as Calendar).apply { add(Calendar.MONTH, month) }

                scheduleList.add(
                    PaymentItem(month, currentMonthlyPayment, fixedPrincipalPart, interestPart, remainingBalanceValue.coerceAtLeast(0.0), paymentDate)
                )
            }
        }
        return scheduleList
    }
}
