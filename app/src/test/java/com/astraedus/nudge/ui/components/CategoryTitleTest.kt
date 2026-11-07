package com.astraedus.nudge.ui.components

import com.astraedus.nudge.testing.ukString
import com.astraedus.nudge.testing.enString
import com.astraedus.nudge.domain.groups.OFFERED_CATEGORIES
import com.astraedus.nudge.domain.packages.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chip titles and the row tags, over every offered category. The first build used Android's own
 * titles, and one of them read "Music and audio" next to seven "X & Y" chips (PR 6 review).
 */
class CategoryTitleTest {

    @Test
    fun `every chip title is Title Case and joins two words with an ampersand`() {
        OFFERED_CATEGORIES.forEach { category ->
            val title = enString(categoryTitleRes(category))
            assertFalse(title, title.contains(" and ", ignoreCase = true))
            title.split(" ").filter { it != "&" }.forEach { word ->
                assertTrue("$title: $word", word.first().isUpperCase())
            }
        }
    }

    @Test
    fun `music reads Music and Audio like the other chips`() {
        assertEquals("Music & Audio", enString(categoryTitleRes(AppCategory.AUDIO)))
    }

    @Test
    fun `chip titles are distinct and never blank`() {
        val languages: List<(Int) -> String> = listOf({ id -> enString(id) }, { id -> ukString(id) })
        for (resolve in languages) {
            val titles = OFFERED_CATEGORIES.map { resolve(categoryTitleRes(it)) }
            assertEquals(titles.size, titles.toSet().size)
            titles.forEach { assertTrue(it.isNotBlank()) }
        }
    }

    @Test
    fun `the row tag is the first part of the chip title`() {
        val expected = mapOf(
            AppCategory.SOCIAL to "Social",
            AppCategory.VIDEO to "Movies",
            AppCategory.GAME to "Games",
            AppCategory.NEWS to "News",
            AppCategory.AUDIO to "Music",
            AppCategory.IMAGE to "Photos",
            AppCategory.MAPS to "Maps",
            AppCategory.PRODUCTIVITY to "Productivity"
        )
        assertEquals(OFFERED_CATEGORIES.toSet(), expected.keys)
        OFFERED_CATEGORIES.forEach { category ->
            val tag = enString(shortCategoryTitleRes(category))
            assertEquals(expected.getValue(category), tag)
            assertTrue(enString(categoryTitleRes(category)).startsWith(tag))
            assertFalse(tag, tag.contains("&"))
        }
    }
}
