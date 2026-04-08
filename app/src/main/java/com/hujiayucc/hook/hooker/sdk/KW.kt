package com.hujiayucc.hook.hooker.sdk

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

/** 快手 */
object KW : Hooker() {
    @SuppressLint("ResourceType")
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        "com.duowan.kiwi.adsplash.view.AdSplashFragment".toClassOrNull()
            ?.methods("findViews")
            ?.hook {
                after {
                    runCatching {
                        val view = (chain.args[0] as View).findViewById<View>(0x7f0923c9)
                        view.performClick()
                    }
                }
            }

        "com.kwad.components.ad.splashscreen.widget.CircleSkipView".toClassOrNull()
            ?.declaredMethods?.hook {
                after {
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        val view = instance<View>()
                        view.performClick()
                    }, 200)
                }
            }
    }
}