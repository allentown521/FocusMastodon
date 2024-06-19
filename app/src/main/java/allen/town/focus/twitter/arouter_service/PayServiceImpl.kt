package allen.town.focus.twitter.arouter_service

import allen.town.core.service.PayService
import android.content.Context
import allen.town.focus.twitter.data.App
import com.wyjson.router.annotation.Service


@Service(remark = "/app/pay/pay_service")
class PayServiceImpl : PayService {


    override fun init() {

    }

    override fun isAdBlocker(): Boolean {
        return App.instance.isAdBlockUser()
    }

    override fun isAliPay(): Boolean {
        return false
    }

    override fun isPurchase(context: Context?,gotoPro:Boolean): Boolean {
        return App.instance.checkSupporter(context,gotoPro)
    }

    override fun setPurchase(purchase: Boolean) {
        App.instance.setSubSupporter(purchase)
    }

    override fun setRemoveAdPurchase(purchase: Boolean) {
        App.instance.setAdSupporter(purchase)
    }


    override fun purchaseDbName(): String {
        return "purchase.db"
    }

    override fun dbVersion(): Int {
        return 27
    }

}