package allen.town.focus.twitter.settings.font

class Font(
    val path: String?,
    val cssName: String,
    val label: String,
    val isCharge: Boolean,
    val isZhCn: Boolean,
    val index: Int,
    val isExternal: Boolean = false
) {
    companion object {
        const val DEFAULT_FONT_KEY = "System"
        const val SANS_SERIF_KEY = "Sans Serif"

        @JvmField
        var fontList = ArrayList<Font>()


        init {
            //system-ui意思是使用系统的字体，考虑有些手机可以换系统字体如果不使用这个值好像一般会使用系统默认sans-serif而不是系统当前字体
            fontList.add(Font(null, "system-ui", DEFAULT_FONT_KEY, false, false, fontList.size))
            fontList.add(
                Font(
                    null, "sans-serif", SANS_SERIF_KEY,
                    false, false, fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Roboto-Regular.ttf",
                    "roboto",
                    "Roboto",
                    false,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/RobotoSlab-Regular.ttf",
                    "roboto_slab",
                    "Roboto Slab",
                    false,
                    false,
                    fontList.size
                )
            )
            fontList.add(Font("fonts/Lora-Regular.ttf", "lora", "Lora", true, false, fontList.size))
            fontList.add(
                Font(
                    "fonts/Alegreya-Regular.ttf",
                    "alegreya",
                    "Alegreya",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/SourceSansPro-Regular.ttf",
                    "source_sans_pro",
                    "Source Sans Pro",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Georgia.ttf",
                    "georgia",
                    "Georgia",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Lato-Regular-2.ttf",
                    "lato",
                    "Lato",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(Font("fonts/Ubuntu-R.ttf", "ubuntu", "Ubuntu", true, false, fontList.size))
            fontList.add(
                Font(
                    "fonts/OpenDyslexicAlta-Regular.otf",
                    "openDyslxic",
                    "OpenDyslexic",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Roboto-Light.ttf",
                    "roboto_light",
                    "Roboto Light",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Montserrat-Regular-8.otf",
                    "montserrat",
                    "Montserrat",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Merriweather-Regular-9.ttf",
                    "merriweather",
                    "Merriweather",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/NotoSans-Regular-2.ttf",
                    "noto_sans",
                    "Noto Sans",
                    true,
                    true,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/HindVadodara-Regular.woff2",
                    "hind_vadodara",
                    "Hind Vadodara",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Questrial-Regular.woff2",
                    "questrial",
                    "Questrial",
                    true,
                    false,
                    fontList.size
                )
            )
            fontList.add(
                Font(
                    "fonts/Lexend-Regular.woff2",
                    "lexend",
                    "Lexend",
                    true,
                    false,
                    fontList.size
                )
            )

            fontList.add(
                Font(
                    "fonts/Raleway-Regular.ttf",
                    "Raleway",
                    "Raleway",
                    true,
                    false,
                    fontList.size
                )
            )

            fontList.add(
                Font(
                    "fonts/PlayfairDisplay-Regular.ttf",
                    "PlayfairDisplay",
                    "PlayfairDisplay",
                    true,
                    false,
                    fontList.size
                )
            )

            fontList.add(
                Font(
                    "fonts/Rubik-Regular.ttf",
                    "Rubik",
                    "Rubik",
                    true,
                    false,
                    fontList.size
                )
            )

            fontList.add(
                Font(
                    "fonts/OpenSans-Regular.ttf",
                    "Open Sans",
                    "Open Sans",
                    true,
                    false,
                    fontList.size
                )
            )

            fontList.add(
                Font(
                    "fonts/WorkSans-Regular.ttf",
                    "Work Sans",
                    "Work Sans",
                    true,
                    false,
                    fontList.size
                )
            )

        }

        @JvmField
        var DEFAULT_FONT = fontList[0]
    }
}