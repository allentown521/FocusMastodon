package allen.town.focus.twitter.arouter_service

import allen.town.core.service.AppService
import android.content.Context
import allen.town.focus.twitter.data.App
import com.wyjson.router.annotation.Service


@Service(remark = "/app/common/app_service")
class AppServiceImpl : AppService {
    override fun isForeground(): Boolean {
        return !App.instance.isAppRunningBackground()
    }


    override fun init() {

    }


}