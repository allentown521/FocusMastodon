package allen.town.focus.ads.service

import allen.town.core.service.AdService
import allen.town.focus.twitter.data.App
import allen.town.focus_common.BuildConfig
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.Timber
import com.google.android.gms.ads.MobileAds
import com.wyjson.router.annotation.Service


@Service(remark = "/app/ad/ad_service")
class AdServiceImpl : AdService {
    override fun getOpenAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/3419835294" else BasePreferenceUtil.admobOpenAdId
            ?: "ca-app-pub-6256483973048476/8575262819"
    }

    override fun getRewardedAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else BasePreferenceUtil.admobRewardAdId
            ?: "ca-app-pub-6256483973048476/8296061218"
    }

    override fun getBannerAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else BasePreferenceUtil.admobBannerAdId
            ?: "ca-app-pub-6256483973048476/2958267767"
    }

    override fun getInterstitialAdUnitId(): String {
        return if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else BasePreferenceUtil.admobInterstitialAdId
            ?: "ca-app-pub-6256483973048476/4639887238"
    }


    override fun init() {
        MobileAds.initialize(
            App.instance
        ) { Timber.d("onInitializationComplete") }
        MobileAds.setAppMuted(true)
    }

}