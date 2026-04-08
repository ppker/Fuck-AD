package com.hujiayucc.hook.hooker.app

import android.os.Handler
import android.os.Looper
import android.view.View
import com.hujiayucc.hook.annotation.Run
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

@Run(
    appName = "哔哩哔哩",
    packageName = "tv.danmaku.bili",
    action = "开屏广告",
    versions = [
        "8.54.0"
    ]
)
object Bilibili : Hooker() {
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        "tv.danmaku.bili.ui.splash.ad.page.FullImageSplash".toClassOrNull()
                ?.methods("y6")
                ?.hook {
                    after {
                        val view = getField(instance(), "v") as View
                        Handler(Looper.getMainLooper()).postDelayed({
                            view.performClick()
                        }, 100)
                    }
                }
    }
}