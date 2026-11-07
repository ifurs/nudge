package com.astraedus.nudge.testing

import com.astraedus.nudge.domain.text.TextResolver
import com.astraedus.nudge.domain.text.UiText
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Resolves string and plural resource ids against the SHIPPED the `values` and `values-uk` resource folders, on the JVM,
 * with no Robolectric. A test of a pure formatting helper that used to assert an English literal
 * asserts the same literal through this class, so it now also pins the copy in the resource file
 * (and, with [ukrainian], the translation's plural forms and placeholders).
 *
 * Mirrors what aapt2 + `Resources` do for the subset of syntax the project uses: `\'` `\"` `\n`
 * `\\` `\@` `\?` `\uXXXX` escapes, a quoted string kept verbatim, whitespace collapsed otherwise,
 * `%1$s` positional arguments, and `<plurals>` quantity selection (CLDR rules for `en` and `uk`).
 */
class XmlTextResolver private constructor(
    private val dirName: String,
    private val locale: Locale,
    private val pluralCategory: (Int) -> String
) : TextResolver {

    private val strings = HashMap<String, String>()
    private val arrays = HashMap<String, List<String>>()
    private val plurals = HashMap<String, Map<String, String>>()
    private val stringNames = rFields("string")
    private val pluralNames = rFields("plurals")
    private val arrayNames = rFields("array")

    init {
        val dir = SourceScan.mainFile("res/$dirName")
        dir.listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }.forEach { load(it) }
    }

    private fun load(file: File) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        for (i in 0 until root.childNodes.length) {
            val node = root.childNodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            when (node.tagName) {
                "string" -> strings[name] = unescape(node.textContent)
                "string-array" -> {
                    val items = ArrayList<String>()
                    for (j in 0 until node.childNodes.length) {
                        val item = node.childNodes.item(j) as? Element ?: continue
                        items += unescape(item.textContent)
                    }
                    arrays[name] = items
                }
                "plurals" -> {
                    val items = HashMap<String, String>()
                    for (j in 0 until node.childNodes.length) {
                        val item = node.childNodes.item(j) as? Element ?: continue
                        items[item.getAttribute("quantity")] = unescape(item.textContent)
                    }
                    plurals[name] = items
                }
            }
        }
    }

    /** The raw (unformatted) string for [id]; null when this locale does not define it. */
    fun rawString(id: Int): String? = strings[stringNames.getValue(id)]

    /** Quantity -> raw string for the plural [id]; null when this locale does not define it. */
    fun rawPlural(id: Int): Map<String, String>? = plurals[pluralNames.getValue(id)]

    /** The items of a `<string-array>` in this locale. */
    fun stringArray(id: Int): List<String> {
        val name = arrayNames[id] ?: error("no R.array with id $id")
        return arrays[name] ?: error("array '$name' missing from res/$dirName")
    }

    override fun string(id: Int, vararg args: Any): String {
        val name = stringNames[id] ?: error("no R.string with id $id")
        val raw = strings[name] ?: error("string '$name' missing from res/$dirName")
        return if (args.isEmpty()) raw else String.format(Locale.ENGLISH, raw, *args)
    }

    override fun plural(id: Int, count: Int, vararg args: Any): String {
        val name = pluralNames[id] ?: error("no R.plurals with id $id")
        val items = plurals[name] ?: error("plurals '$name' missing from res/$dirName")
        val raw = items[pluralCategory(count)] ?: items["other"] ?: error("plurals '$name' has no 'other'")
        return String.format(Locale.ENGLISH, raw, *(if (args.isEmpty()) arrayOf<Any>(count) else args))
    }

    override fun date(date: java.time.LocalDate, weekday: Boolean): String {
        val pattern = if (dirName == "values") {
            if (weekday) "EEE, MMM d" else "MMM d"
        } else {
            if (weekday) "EEE, d MMM" else "d MMM"
        }
        return java.time.format.DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }

    companion object {
        val english: XmlTextResolver by lazy { XmlTextResolver("values", Locale.ENGLISH) { n -> if (n == 1) "one" else "other" } }

        val ukrainian: XmlTextResolver by lazy {
            XmlTextResolver("values-uk", Locale.forLanguageTag("uk")) { n ->
                val m10 = n % 10
                val m100 = n % 100
                when {
                    m10 == 1 && m100 != 11 -> "one"
                    m10 in 2..4 && m100 !in 12..14 -> "few"
                    else -> "many"
                }
            }
        }

        /** All the keys declared in the English (default) resources, for the translation gates. */
        fun stringKeys(dirName: String = "values"): Set<String> = names(dirName, "string")

        fun arrayKeys(dirName: String = "values"): Set<String> = names(dirName, "string-array")

        fun pluralKeys(dirName: String = "values"): Set<String> = names(dirName, "plurals")

        private fun names(dirName: String, tag: String): Set<String> {
            val out = HashSet<String>()
            SourceScan.mainFile("res/$dirName").listFiles { f -> f.extension == "xml" }.orEmpty().forEach { file ->
                val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
                for (i in 0 until root.childNodes.length) {
                    val e = root.childNodes.item(i) as? Element ?: continue
                    if (e.tagName == tag) out += e.getAttribute("name")
                }
            }
            return out
        }

        private fun rFields(kind: String): Map<Int, String> {
            val cls = Class.forName("com.astraedus.nudge.R\$$kind")
            return cls.fields.associate { (it.get(null) as Int) to it.name }
        }

        fun unescape(textContent: String): String {
            val trimmed = textContent.trim()
            val quoted = trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')
            val body = if (quoted) trimmed.substring(1, trimmed.length - 1) else trimmed.replace(Regex("\\s+"), " ")
            val sb = StringBuilder()
            var i = 0
            while (i < body.length) {
                val c = body[i]
                if (c == '\\' && i + 1 < body.length) {
                    when (val n = body[i + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'u' -> {
                            sb.append(body.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 4
                        }
                        else -> sb.append(n)
                    }
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}

/** A string resource as the English user reads it (no arguments needed: pass them for `%1$s`). */
fun enString(id: Int, vararg args: Any): String = XmlTextResolver.english.string(id, *args)

/** A string resource as the Ukrainian user reads it. */
fun ukString(id: Int, vararg args: Any): String = XmlTextResolver.ukrainian.string(id, *args)

/** [UiText] as the English user would read it. */
fun UiText.en(): String = resolve(XmlTextResolver.english)

/** [UiText] as the Ukrainian user would read it. */
fun UiText.uk(): String = resolve(XmlTextResolver.ukrainian)
