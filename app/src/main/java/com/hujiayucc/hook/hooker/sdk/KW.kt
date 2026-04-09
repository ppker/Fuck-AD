package com.hujiayucc.hook.hooker.sdk

import android.annotation.SuppressLint
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
                    val view = (chain.args[0] as View).findViewById<View>(0x7f0923c9)
                    runMain { view.performClick() }
                }
            }

        "com.kwad.components.ad.splashscreen.widget.CircleSkipView".toClassOrNull()
            ?.declaredMethods?.hook {
                after {
                    val view = instance<View>()
                    runMain { view.performClick() }
                }
            }
    }
}