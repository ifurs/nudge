package com.astraedus.nudge.domain.text

import java.time.LocalDate

/**
 * Turns a resource id into the user's language. The one seam between "which words" (an id and its
 * arguments, decided by pure helpers and ViewModels) and "in which language" (decided where a
 * `Resources` exists). See `docs/i18n.md`.
 *
 * An interface so a JVM test can resolve against the shipped `res/values*.xml` without Robolectric
 * (`XmlTextResolver` in the test tree), and so pure formatting code never needs an Android class.
 * The ids are `R.string` / `R.plurals` ints; the production resolver lives in `ui/text`.
 */
interface TextResolver {
    fun string(id: Int, vararg args: Any): String
    fun plural(id: Int, count: Int, vararg args: Any): String

    /** A calendar date in the user's locale: "Thu, Aug 21" with [weekday], "Aug 21" without. */
    fun date(date: LocalDate, weekday: Boolean): String
}

/**
 * User-visible text as DATA: a string-resource id plus its arguments, a plural id plus its count,
 * or a piece that is not translatable (an app label, a group name the user typed).
 *
 * ViewModels and pure helpers return this instead of English, so a test asserts the id and
 * arguments and the UI layer decides the language ([resolve], or `asString()` in a composable).
 * Arguments may themselves be [UiText]; they are resolved first, so texts nest.
 */
sealed interface UiText {

    /** A `<string>` resource, with positional [args] (`%1$s`, `%2$d`). */
    data class Res(val id: Int, val args: List<Any> = emptyList()) : UiText

    /** A `<plurals>` resource; [args] defaults to just the count (`%1$d`). */
    data class Plural(val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    /** Text that is data, not copy: an app label, a name the user typed, a number. */
    data class Raw(val value: String) : UiText

    /** A calendar date, worded by the locale at resolve time (see [TextResolver.date]). */
    data class Date(val date: LocalDate, val weekday: Boolean = false) : UiText

    /** [parts] joined by [separator] (punctuation such as " · ", never a word). */
    data class Joined(val parts: List<UiText>, val separator: String) : UiText

    fun resolve(resolver: TextResolver): String = when (this) {
        is Res -> resolver.string(id, *resolveArgs(args, resolver))
        is Plural -> resolver.plural(id, count, *resolveArgs(args, resolver))
        is Raw -> value
        is Date -> resolver.date(date, weekday)
        is Joined -> parts.joinToString(separator) { it.resolve(resolver) }
    }

    companion object {
        fun of(id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun plural(id: Int, count: Int, vararg args: Any): UiText =
            Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

        fun raw(value: String): UiText = Raw(value)

        private fun resolveArgs(args: List<Any>, resolver: TextResolver): Array<Any> =
            Array(args.size) { i -> (args[i] as? UiText)?.resolve(resolver) ?: args[i] }
    }
}
