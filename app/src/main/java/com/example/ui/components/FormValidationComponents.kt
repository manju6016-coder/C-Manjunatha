package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.util.IndianCurrencyUtils

/**
 * Encapsulates the validation status of a single form field.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val warningMessage: String? = null
) {
    companion object {
        val Valid = ValidationResult(isValid = true)

        fun error(message: String): ValidationResult =
            ValidationResult(isValid = false, errorMessage = message)

        fun warning(message: String): ValidationResult =
            ValidationResult(isValid = true, warningMessage = message)
    }
}

/**
 * Standard validators for business forms.
 */
object FormValidators {

    /**
     * Validates that the input represents a strictly positive integer (> 0).
     */
    fun validatePositiveInteger(
        value: String,
        fieldName: String,
        isRequired: Boolean = true
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required and must be a positive integer (> 0)")
            } else {
                ValidationResult.Valid
            }
        }
        val number = trimmed.toLongOrNull()
            ?: return ValidationResult.error("$fieldName must be a valid whole number")

        return if (number <= 0L) {
            ValidationResult.error("$fieldName must be a positive integer greater than 0")
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates that the input represents a non-negative integer (>= 0).
     */
    fun validateNonNegativeInteger(
        value: String,
        fieldName: String,
        isRequired: Boolean = false
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required")
            } else {
                ValidationResult.Valid
            }
        }
        val number = trimmed.toLongOrNull()
            ?: return ValidationResult.error("$fieldName must be a valid whole number")

        return if (number < 0L) {
            ValidationResult.error("$fieldName cannot be negative")
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates that an amount is a positive integer and does not exceed an upper limit.
     */
    fun validateAmountWithinMax(
        value: String,
        fieldName: String,
        maxLimit: Long,
        maxLimitLabel: String,
        isRequired: Boolean = true,
        allowZero: Boolean = false
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required")
            } else {
                ValidationResult.Valid
            }
        }
        val number = trimmed.toLongOrNull()
            ?: return ValidationResult.error("$fieldName must be a valid whole number")

        if (!allowZero && number <= 0L) {
            return ValidationResult.error("$fieldName must be a positive integer greater than 0")
        }
        if (allowZero && number < 0L) {
            return ValidationResult.error("$fieldName cannot be negative")
        }
        if (number > maxLimit) {
            return ValidationResult.error(
                "$fieldName (${IndianCurrencyUtils.formatInr(number)}) cannot exceed $maxLimitLabel of ${IndianCurrencyUtils.formatInr(maxLimit)}"
            )
        }
        return ValidationResult.Valid
    }

    /**
     * Validates that a string is non-blank.
     */
    fun validateRequiredText(value: String, fieldName: String): ValidationResult {
        return if (value.trim().isEmpty()) {
            ValidationResult.error("$fieldName is required and cannot be blank")
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates that a value is a valid positive or non-negative Long integer.
     */
    fun validatePositiveLong(
        value: String,
        fieldName: String,
        isRequired: Boolean = true,
        allowZero: Boolean = false
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required")
            } else {
                ValidationResult.Valid
            }
        }
        val number = trimmed.toLongOrNull()
            ?: return ValidationResult.error("$fieldName must be a valid whole number")

        if (!allowZero && number <= 0L) {
            return ValidationResult.error("$fieldName must be greater than 0")
        }
        if (allowZero && number < 0L) {
            return ValidationResult.error("$fieldName cannot be negative")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates standard 10-digit Indian mobile number.
     */
    fun validateMobileNumber(
        value: String,
        fieldName: String = "Mobile Number",
        isRequired: Boolean = true
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required")
            } else {
                ValidationResult.Valid
            }
        }
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length != 10) {
            return ValidationResult.error("$fieldName must be exactly 10 digits (currently ${digitsOnly.length})")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates that a decimal amount is strictly positive (> 0.0).
     */
    fun validatePositiveDecimal(
        value: String,
        fieldName: String,
        isRequired: Boolean = true
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired) {
                ValidationResult.error("$fieldName is required")
            } else {
                ValidationResult.Valid
            }
        }
        val number = trimmed.toDoubleOrNull()
            ?: return ValidationResult.error("$fieldName must be a valid number")

        return if (number <= 0.0) {
            ValidationResult.error("$fieldName must be greater than 0")
        } else {
            ValidationResult.Valid
        }
    }
}

/**
 * Validated Number Input Field providing real-time visual feedback:
 * - Red border and container highlights when input is invalid
 * - Green checkmark badge when input is valid and populated
 * - Inline error message with icon directly below the field before form submission
 * - Status tags (e.g. "Required", "Invalid", "Valid")
 */
@Composable
fun ValidatedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationResult: ValidationResult,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    prefix: @Composable (() -> Unit)? = { Text("₹ ") },
    isRequired: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    ruleBadgeText: String? = null,
    supportingHelperText: String? = null,
    testTag: String = ""
) {
    val isError = !validationResult.isValid
    val isValidAndFilled = validationResult.isValid && value.isNotBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        // Field Label Row with Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (isRequired) {
                    Text(
                        text = " *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Status Indicator Badges
            when {
                isError -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Invalid",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("${testTag}_badge_invalid")
                        )
                    }
                }
                ruleBadgeText != null -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SaffronGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ruleBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                isValidAndFilled -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Valid ✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("${testTag}_badge_valid")
                        )
                    }
                }
                isRequired -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Field with reactive feedback styling
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                // Ensure only digits can be entered for whole number fields
                val filtered = input.filter { it.isDigit() }
                onValueChange(filtered)
            },
            readOnly = readOnly,
            enabled = enabled,
            prefix = prefix,
            placeholder = { Text(placeholder) },
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
                focusedBorderColor = if (isValidAndFilled) EmeraldGreen else PrimaryIndigo,
                unfocusedBorderColor = if (isValidAndFilled) EmeraldGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
            ),
            trailingIcon = {
                when {
                    isError -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Invalid Input",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    isValidAndFilled -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid Input",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            supportingText = {
                when {
                    isError && validationResult.errorMessage != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .testTag("${testTag}_error")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = validationResult.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                    validationResult.warningMessage != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SaffronDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = validationResult.warningMessage,
                                color = SaffronDark,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    supportingHelperText != null -> {
                        Text(
                            text = supportingHelperText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

/**
 * Validated General Text Input Field providing real-time visual feedback:
 * - Red border and container highlight when invalid
 * - Clear error message and error icon before submit
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationResult: ValidationResult,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    prefix: (@Composable () -> Unit)? = null,
    ruleBadgeText: String? = null,
    isRequired: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType? = null,
    keyboardOptions: KeyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
    testTag: String = ""
) {
    val isError = !validationResult.isValid
    val isValidAndFilled = validationResult.isValid && value.isNotBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (isRequired) {
                    Text(
                        text = " *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            when {
                isError -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Invalid",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("${testTag}_badge_invalid")
                        )
                    }
                }
                ruleBadgeText != null -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SaffronGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ruleBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                isValidAndFilled -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Valid ✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                isRequired -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            prefix = prefix,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
                focusedBorderColor = if (isValidAndFilled) EmeraldGreen else PrimaryIndigo,
                unfocusedBorderColor = if (isValidAndFilled) EmeraldGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
            ),
            trailingIcon = {
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Invalid Input",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isValidAndFilled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valid Input",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            supportingText = {
                if (isError && validationResult.errorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .testTag("${testTag}_error")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = validationResult.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

/**
 * Validated General Text Input Field with String prefix overload.
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationResult: ValidationResult,
    prefix: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    ruleBadgeText: String? = null,
    isRequired: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType? = null,
    keyboardOptions: KeyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
    testTag: String = ""
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        validationResult = validationResult,
        modifier = modifier,
        placeholder = placeholder,
        prefix = { Text(prefix) },
        ruleBadgeText = ruleBadgeText,
        isRequired = isRequired,
        singleLine = singleLine,
        keyboardType = keyboardType,
        keyboardOptions = keyboardOptions,
        testTag = testTag
    )
}

/**
 * Prominent visual summary banner alerting the user of all active validation issues
 * before attempting to proceed or submit.
 */
@Composable
fun FormValidationSummaryBanner(
    errors: List<String>,
    modifier: Modifier = Modifier,
    title: String = "Please correct the following before continuing:"
) {
    AnimatedVisibility(
        visible = errors.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("form_validation_summary_banner"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                errors.forEach { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
