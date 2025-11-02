package com.lohith.scrollsense.ui.components

import androidx.compose.foundation.layout.Box // <-- IMPORT ADDED
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor // <-- IMPORT ADDED
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider // <-- IMPORT ADDED
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCategory(
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                title()
            }
        }
        content()
    }
}