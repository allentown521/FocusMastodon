package allen.town.focus.twitter.widget.timeline

import android.content.Intent
import android.widget.RemoteViewsService

class TimelineWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return TimelineRemoteViewsFactory(this)
    }
}
