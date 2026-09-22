package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for Indian Currency (INR / ₹) formatting,
 * Indian numbering system (Lakhs and Crores), Number to Words conversion,
 * and Date formatting.
 */
object IndianCurrencyUtils {

    fun formatInr(amount: Long, showSymbol: Boolean = true): String {
        val symbol = if (showSymbol) "₹" else ""
        val prefix = if (amount < 0) "- " else ""
        return "$prefix$symbol${formatIndianNumber(Math.abs(amount).toString())}"
    }

    /**
     * Formats a double amount into Indian Rupee currency format (e.g., ₹1,23,456.75 or ₹12,50,000.00).
     */
    fun formatInr(amount: Double, showSymbol: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)

        val longPart = absAmount.toLong()
        val decimalPart = Math.round((absAmount - longPart) * 100).toInt()

        val longPartStr = longPart.toString()
        val formattedLongPart = formatIndianNumber(longPartStr)
        val formattedDecimal = String.format(Locale.US, "%02d", decimalPart)

        val result = if (decimalPart > 0) {
            "$formattedLongPart.$formattedDecimal"
        } else {
            "$formattedLongPart.00"
        }

        val prefix = if (isNegative) "- " else ""
        val symbol = if (showSymbol) "₹" else ""
        return "$prefix$symbol$result"
    }

    /**
     * Formats integer string into standard Indian grouping:
     * Last 3 digits grouped together, followed by groups of 2 digits (Lakhs, Crores).
     * e.g. "1234567" -> "12,34,567"
     */
    fun formatIndianNumber(numberStr: String): String {
        if (numberStr.length <= 3) return numberStr

        val lastThree = numberStr.substring(numberStr.length - 3)
        val remaining = numberStr.substring(0, numberStr.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        val reversedRemaining = sb.reverse().toString()
        return "$reversedRemaining,$lastThree"
    }

    /**
     * Converts a numeric amount to Indian Rupee Words representation.
     * e.g., 54,250 -> "Fifty Four Thousand Two Hundred Fifty Rupees Only"
     */
    fun amountInWords(amount: Double): String {
        val longVal = amount.toLong()
        if (longVal <= 0) return "Zero Rupees"

        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertTwoDigits(n: Int): String {
            return when {
                n < 20 -> units[n]
                else -> tens[n / 10] + (if (n % 10 > 0) " " + units[n % 10] else "")
            }
        }

        fun convertThreeDigits(n: Int): String {
            val hundred = n / 100
            val remainder = n % 100
            val sb = StringBuilder()
            if (hundred > 0) {
                sb.append(units[hundred]).append(" Hundred")
                if (remainder > 0) sb.append(" ")
            }
            if (remainder > 0) {
                sb.append(convertTwoDigits(remainder))
            }
            return sb.toString().trim()
        }

        var temp = longVal
        val crore = (temp / 10000000).toInt()
        temp %= 10000000
        val lakh = (temp / 100000).toInt()
        temp %= 100000
        val thousand = (temp / 1000).toInt()
        temp %= 1000
        val hundredAndBelow = temp.toInt()

        val parts = mutableListOf<String>()
        if (crore > 0) parts.add("${convertThreeDigits(crore)} Crore")
        if (lakh > 0) parts.add("${convertTwoDigits(lakh)} Lakh")
        if (thousand > 0) parts.add("${convertTwoDigits(thousand)} Thousand")
        if (hundredAndBelow > 0) parts.add(convertThreeDigits(hundredAndBelow))

        val words = parts.joinToString(" ")
        return "$words Rupees Only"
    }

    /**
     * Formats timestamp into date string: "07 Sep 2026"
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into time string: "02:30 PM"
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into full date & time: "07 Sep 2026, 02:30 PM"
     */
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Returns start of day (midnight 00:00:00.000) for given timestamp.
     */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Returns end of day (23:59:59.999) for given timestamp.
     */
    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Checks if two timestamps fall on the same calendar day.
     */
    fun isSameDay(t1: Long, t2: Long): Boolean {
        return getStartOfDay(t1) == getStartOfDay(t2)
    }
}
