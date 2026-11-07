package com.astraedus.nudge.testing

import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * L4 gate on the shipped translations (`docs/i18n.md`): Ukrainian has every key English has, with the
 * same placeholders, the four plural forms Ukrainian needs, and the style the glossary asks for.
 *
 * Lint's `MissingTranslation` already fails the build for a missing key; this adds what lint cannot
 * see: a translation that drops or invents a `%1$d`, a plural missing `few`/`many`, an array of a
 * different length, and a handful of words the style guide bans.
 */
class TranslationParityTest {

    private class Entry(val kind: String, val name: String, val text: String)

    /** Every string / plural item / array item in a resource folder, as (kind, name, quantity-or-index) -> text. */
    private fun load(dirName: String): Map<String, Entry> {
        val out = LinkedHashMap<String, Entry>()
        SourceScan.mainFile("res/$dirName").listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }.forEach { file ->
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            for (i in 0 until root.childNodes.length) {
                val node = root.childNodes.item(i) as? Element ?: continue
                val name = node.getAttribute("name")
                when (node.tagName) {
                    "string" -> out["string:$name"] = Entry("string", name, XmlTextResolver.unescape(node.textContent))
                    "plurals", "string-array" -> {
                        var index = 0
                        for (j in 0 until node.childNodes.length) {
                            val item = node.childNodes.item(j) as? Element ?: continue
                            val key = if (node.tagName == "plurals") item.getAttribute("quantity") else (index++).toString()
                            out["${node.tagName}:$name:$key"] = Entry(
                                node.tagName, name, XmlTextResolver.unescape(item.textContent)
                            )
                        }
                    }
                }
            }
        }
        return out
    }

    private val en by lazy { load("values") }
    private val uk by lazy { load("values-uk") }

    private val placeholder = Regex("%(\\d+)\\$[sd]")

    private fun placeholders(text: String): Set<String> = placeholder.findAll(text).map { it.groupValues[1] }.toSet()

    /** `stats_hour_*`: the 12-hour value is %1$d in English, the 24-hour %2$d in Ukrainian, by design. */
    private val placeholderExceptions = setOf("string:stats_hour_am", "string:stats_hour_pm")

    @Test
    fun `ukrainian has every string, plural and array item english has, and nothing else`() {
        // Plural quantities differ by language, so compare plurals by NAME and arrays by item.
        fun names(map: Map<String, Entry>) = map.values.map { it.kind + ":" + it.name }.toSet() +
            map.keys.filter { it.startsWith("string-array:") }
        assertEquals("only in English", emptySet<String>(), names(en) - names(uk))
        assertEquals("only in Ukrainian", emptySet<String>(), names(uk) - names(en))
    }

    @Test
    fun `every ukrainian string keeps the english placeholders`() {
        val problems = en.filter { it.value.kind == "string" }.mapNotNull { (key, e) ->
            val u = uk[key] ?: return@mapNotNull null
            if (key in placeholderExceptions) return@mapNotNull null
            if (placeholders(e.text) != placeholders(u.text)) "$key: en=${placeholders(e.text)} uk=${placeholders(u.text)}" else null
        }
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test
    fun `every ukrainian plural has the four forms and its items use only english placeholders`() {
        val problems = ArrayList<String>()
        val enNames = en.values.filter { it.kind == "plurals" }.map { it.name }.toSet()
        for (name in enNames) {
            val forms = uk.filterKeys { it.startsWith("plurals:$name:") }.keys.map { it.substringAfterLast(':') }.toSet()
            if (forms != setOf("one", "few", "many", "other")) problems += "$name: ukrainian forms $forms"
            val enForms = en.filterKeys { it.startsWith("plurals:$name:") }.keys.map { it.substringAfterLast(':') }.toSet()
            if (enForms != setOf("one", "other")) problems += "$name: english forms $enForms"
            val allowed = en.filterKeys { it.startsWith("plurals:$name:") }.values.flatMap { placeholders(it.text) }.toSet()
            uk.filterKeys { it.startsWith("plurals:$name:") }.forEach { (key, u) ->
                if (!allowed.containsAll(placeholders(u.text))) problems += "$key: uses ${placeholders(u.text)}, english has $allowed"
            }
        }
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test
    fun `ukrainian follows the glossary and typography`() {
        val banned = listOf(
            // docs/i18n.md: застосунок, not «додаток», for an app.
            Regex("додат(ок|ки|ку|ків|ком)", RegexOption.IGNORE_CASE) to "use «застосунок», not «додаток» (an app)",
            // Russian letters that do not exist in Ukrainian.
            Regex("[ыэёъ]") to "a Russian-only letter",
            // The apostrophe is U+2019, not ASCII ' (an unescaped one would not even compile).
            Regex("[А-Яа-яІіЇїЄєҐґ]'[А-Яа-яІіЇїЄєҐґ]") to "use the typographic apostrophe ’",
            Regex("[А-Яа-яІіЇїЄєҐґ]ʼ[А-Яа-яІіЇїЄєҐґ]") to "use ’ (U+2019), not the modifier letter ʼ",
            Regex("  +") to "double space"
        )
        val problems = uk.flatMap { (key, entry) ->
            banned.filter { (regex, _) -> regex.containsMatchIn(entry.text) }.map { (_, why) -> "$key: $why: ${entry.text}" }
        }
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test
    fun `no ukrainian string is left as untranslated english`() {
        // A translation that equals its English source is allowed for brand words, numbers and
        // technical tokens; anything else with words in it is a forgotten translation.
        val allowedSame = setOf(
            "string:app_name",             // deliberately identical, see values-uk/strings.xml
            "string:settings_bounce_title", // the owner's name for the feature, kept as written
            "string:service_bounce_title"
        )
        val forgotten = en.filter { (key, e) ->
            val u = uk[key]
            val words = e.text.replace(placeholder, " ").replace("Nudge", " ").split(Regex("[^A-Za-z]+"))
            u != null && u.text == e.text && key !in allowedSame && words.any { it.length > 3 }
        }.keys
        assertEquals("untranslated: $forgotten", emptySet<String>(), forgotten)
    }

    @Test
    fun `the english plural helper and the ukrainian one pick the cldr forms`() {
        val plural = com.astraedus.nudge.R.plurals.components_summary_group_title
        fun uk(n: Int) = XmlTextResolver.ukrainian.plural(plural, n, "Група", n)
        assertEquals("Група · 1 застосунок", uk(1))
        assertEquals("Група · 2 застосунки", uk(2))
        assertEquals("Група · 5 застосунків", uk(5))
        assertEquals("Група · 11 застосунків", uk(11))
        assertEquals("Група · 21 застосунок", uk(21))
        assertEquals("Група · 24 застосунки", uk(24))
        assertEquals("Група · 112 застосунків", uk(112))
        assertTrue(XmlTextResolver.english.plural(plural, 1, "G", 1).endsWith("1 app"))
    }
}
