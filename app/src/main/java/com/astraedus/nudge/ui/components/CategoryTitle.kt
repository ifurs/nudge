package com.astraedus.nudge.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import com.astraedus.nudge.domain.packages.AppCategory

/**
 * A category's name on a chip: "Social & Communication", "Music & Audio". Nudge's own titles, in one
 * style (Title Case, joined by "&"). Android's `ApplicationInfo.getCategoryTitle` was used first, and
 * it named seven categories "X & Y" and one "Music and audio", so one chip read differently from the
 * other seven. Nudge's own strings (translated in `values-uk`) lose nothing and read the same in
 * every language the platform would have varied them in.
 *
 * UNDEFINED is never offered as a chip; it is named for exhaustiveness only.
 */
@StringRes
fun categoryTitleRes(category: AppCategory): Int = when (category) {
    AppCategory.SOCIAL -> R.string.components_category_social
    AppCategory.VIDEO -> R.string.components_category_video
    AppCategory.GAME -> R.string.components_category_game
    AppCategory.NEWS -> R.string.components_category_news
    AppCategory.AUDIO -> R.string.components_category_audio
    AppCategory.IMAGE -> R.string.components_category_image
    AppCategory.MAPS -> R.string.components_category_maps
    AppCategory.PRODUCTIVITY -> R.string.components_category_productivity
    AppCategory.UNDEFINED -> R.string.components_category_undefined
}

@Composable
fun categoryTitle(category: AppCategory): String = stringResource(categoryTitleRes(category))

/**
 * The tag on a member row: the first part of the title ("Social", "Music"). The full title
 * ellipsised on every row on the bench ("Social & Communica…"), so each language names a short form.
 */
@StringRes
fun shortCategoryTitleRes(category: AppCategory): Int = when (category) {
    AppCategory.SOCIAL -> R.string.components_category_short_social
    AppCategory.VIDEO -> R.string.components_category_short_video
    AppCategory.GAME -> R.string.components_category_game
    AppCategory.NEWS -> R.string.components_category_short_news
    AppCategory.AUDIO -> R.string.components_category_short_audio
    AppCategory.IMAGE -> R.string.components_category_short_image
    AppCategory.MAPS -> R.string.components_category_short_maps
    AppCategory.PRODUCTIVITY -> R.string.components_category_productivity
    AppCategory.UNDEFINED -> R.string.components_category_undefined
}

@Composable
fun shortCategoryTitle(category: AppCategory): String = stringResource(shortCategoryTitleRes(category))
