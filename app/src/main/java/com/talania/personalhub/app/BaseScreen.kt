package com.talania.personalhub.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talania.personalhub.R

@Composable
fun BaseScreen(state: BootstrapState, onRetry: () -> Unit) {
    Scaffold { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(
                insets,
            ).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            when (state) {
                BootstrapState.Loading -> Text(stringResource(R.string.storage_loading))

                BootstrapState.StorageError -> {
                    Text(stringResource(R.string.storage_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.storage_retry)) }
                }

                is BootstrapState.Ready -> {
                    Text(stringResource(R.string.base_ready))
                    Text(stringResource(R.string.base_description))
                }
            }
        }
    }
}
