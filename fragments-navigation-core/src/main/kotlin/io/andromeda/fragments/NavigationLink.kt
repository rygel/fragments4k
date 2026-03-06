package io.andromeda.fragments

data class NavigationLink(
    val label: String,
    val url: String,
    val cssClass: String = "",
    val isActive: Boolean = false
)
