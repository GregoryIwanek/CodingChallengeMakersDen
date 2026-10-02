package pl.gi.codingchallenge.feature.detail.testing

// internal (not private) so androidTest can target elements by tag
// instead of by display text, which differs between repo and user items.
internal object DetailTestTags {
    const val SCREEN: String = "detail_screen"
    const val BACK_BUTTON: String = "detail_back_button"
    const val TITLE: String = "detail_title"
    const val SUBTITLE: String = "detail_subtitle"
    const val DESCRIPTION: String = "detail_description"
    const val STARS: String = "detail_stars"
    const val OPEN_BUTTON: String = "detail_open_button"
    const val SHARE_BUTTON: String = "detail_share_button"
    const val COPY_BUTTON: String = "detail_copy_button"
}
