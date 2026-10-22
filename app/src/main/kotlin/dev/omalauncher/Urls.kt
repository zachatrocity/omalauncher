package dev.omalauncher

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens [url] in whatever app handles it. False when none does (no browser, an unknown scheme). */
fun Context.openUrl(url: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

/** Searches with Android's chosen web-search handler, without choosing a provider for the user. */
fun Context.searchWeb(query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return false
    return try {
        startActivity(
            Intent(Intent.ACTION_WEB_SEARCH)
                .putExtra(SearchManager.QUERY, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
