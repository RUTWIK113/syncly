package com.ppicalendar.app.presentation.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun NativeDatePickerField(
    date: String,
    onDateSelected: (String) -> Unit,
    label: String = "Date",
    modifier: Modifier = Modifier,
    placeholder: String = "Select date"
) {
    val context = LocalContext.current
    val parsedDate = remember(date) {
        try {
            if (date.isNotBlank()) LocalDate.parse(date) else null
        } catch (e: Exception) {
            null
        }
    }

    val calendar = Calendar.getInstance()
    val initialYear = parsedDate?.year ?: calendar.get(Calendar.YEAR)
    val initialMonth = parsedDate?.let { it.monthValue - 1 } ?: calendar.get(Calendar.MONTH)
    val initialDay = parsedDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)

    fun openPicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            initialYear,
            initialMonth,
            initialDay
        ).show()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { openPicker() }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        // Click overlay to ensure clicking anywhere on the text field opens picker
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { openPicker() }
        )
    }
}

@Composable
fun NativeTimePickerField(
    time: String,
    onTimeSelected: (String) -> Unit,
    label: String = "Time",
    modifier: Modifier = Modifier,
    placeholder: String = "Select time"
) {
    val context = LocalContext.current
    val parsedTime = remember(time) {
        try {
            if (time.isNotBlank()) LocalTime.parse(time) else null
        } catch (e: Exception) {
            null
        }
    }

    val calendar = Calendar.getInstance()
    val initialHour = parsedTime?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = parsedTime?.minute ?: calendar.get(Calendar.MINUTE)

    fun openPicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format("%02d:%02d", hourOfDay, minute)
                onTimeSelected(formatted)
            },
            initialHour,
            initialMinute,
            true // 24-hr format standard
        ).show()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = time,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Pick Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { openPicker() }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        // Click overlay to ensure clicking anywhere on the field opens picker
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { openPicker() }
        )
    }
}
