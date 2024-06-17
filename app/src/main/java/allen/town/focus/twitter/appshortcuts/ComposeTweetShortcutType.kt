package allen.town.focus.twitter.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.os.Build
import code.name.monkey.appthemehelper.shortcut.AppShortcutIconGenerator
import code.name.monkey.appthemehelper.shortcut.BaseShortcutType
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.compose.LauncherCompose

@TargetApi(Build.VERSION_CODES.N_MR1)
class ComposeTweetShortcutType(context: Context) : BaseShortcutType(context) {
    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(
            context,
            id
        ).setShortLabel(context.getString(R.string.shortcut_compose_short)).setIcon(
            AppShortcutIconGenerator.generateThemedIcon(
                context,
                R.drawable.ic_fab_pencil
            )
        ).setIntent(getPlaySongsIntent(LauncherCompose::class.java, null, null))
            .build()

    companion object {

        val id: String
            get() = ID_PREFIX + "ComposeTweet"
    }
}