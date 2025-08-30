package com.amarildo.listify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amarildo.listify.service.FolderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    suspend fun selectDirectory(): String {
        val result: Result<String> = FolderParser.selectDirectory()
        try {
            return result.getOrThrow()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Unexpected error: ${e.message}",
            )
            return ""
        }
    }

    fun parseAndCreateFile(basePath: String, prefixesToFilter: String) {
        if (basePath.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Base path cannot be empty",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val outputFilePath = withContext(Dispatchers.IO) {
                    FolderParser.processDirectory(basePath, prefixesToFilter)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Completed: $outputFilePath",
                )
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Invalid path: ${e.message}",
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "File error: ${e.message}",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}",
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            successMessage = null,
            error = null,
        )
    }
}
