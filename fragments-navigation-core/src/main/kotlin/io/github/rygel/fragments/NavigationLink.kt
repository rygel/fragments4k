package io.github.rygel.fragments

/**
 * A single item in a navigation menu.
 *
 * Used by [NavigationMenuGenerator] to produce structured menu data for templates.
 *
 * @property label Display text for the menu link.
 * @property url Target URL or path for the menu link.
 * @property cssClass Optional CSS class applied to the link element; defaults to empty.
 * @property isActive When `true`, indicates the link represents the current page;
 *   templates can use this for active-state styling.
 */
data class NavigationLink(
    val label: String,
    val url: String,
    val cssClass: String = "",
    val isActive: Boolean = false,
)
