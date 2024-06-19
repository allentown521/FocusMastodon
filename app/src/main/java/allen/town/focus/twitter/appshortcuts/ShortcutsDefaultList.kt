package allen.town.focus.twitter.appshortcuts

import android.content.Context
import android.content.pm.ShortcutInfo

class ShortcutsDefaultList(private val context: Context) {
    public val defaultShortcuts: List<ShortcutInfo>
        get() = ArrayList<ShortcutInfo>()/*.apply {
            add(ComposeTweetShortcutType(context).shortcutInfo)
        }*/

}