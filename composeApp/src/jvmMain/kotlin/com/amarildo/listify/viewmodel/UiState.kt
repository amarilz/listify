package com.amarildo.listify.viewmodel

data class UiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
)
