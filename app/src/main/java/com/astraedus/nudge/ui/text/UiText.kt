package com.astraedus.nudge.ui.text

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.astraedus.nudge.domain.text.TextResolver
import com.astraedus.nudge.domain.text.UiText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The production [TextResolver]: real `Resources`, so the user's (per-app) language applies. */
class ResourcesTextResolver(private val resources: Resources) : TextResolver {
    override fun string(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)

    override fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        resources.getQuantityString(id, count, *args)

    override fun date(date: LocalDate, weekday: Boolean): String {
        val locale = resources.configuration.locales[0]
        // The platform knows where each language puts the weekday, month and day ("Thu, Aug 21"
        // in English, "чт, 21 серп." in Ukrainian), so the pattern is asked for, not written here.
        val pattern = DateFormat.getBestDateTimePattern(locale, if (weekday) "EEEMMMd" else "MMMd")
        return DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}

fun Context.textResolver(): TextResolver = ResourcesTextResolver(resources)

fun UiText.resolve(context: Context): String = resolve(context.textResolver())

/** The resolver for the current composition; changes with the configuration (a locale switch). */
@Composable
fun rememberTextResolver(): TextResolver {
    val context = LocalContext.current
    // Reading the configuration makes a locale change recompose every caller.
    @Suppress("UNUSED_VARIABLE") val configuration = LocalConfiguration.current
    return ResourcesTextResolver(context.resources)
}

@Composable
fun UiText.asString(): String = resolve(rememberTextResolver())
