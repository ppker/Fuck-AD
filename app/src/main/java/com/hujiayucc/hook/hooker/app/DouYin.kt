package com.hujiayucc.hook.hooker.app

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.hujiayucc.hook.annotation.Run
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

@Run(
    appName = "抖音",
    packageName = "com.ss.android.ugc.aweme",
    action = "奖励广告（小程序广告除外）"
)
object DouYin : Hooker() {

    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        "com.ss.android.excitingvideo.ExcitingVideoActivity".toClassOrNull()
            ?.method("onResume")
            ?.hook {
                after {
                    instance.javaClass.fields.forEach { field ->
                        if (field.type.name == "com.ss.android.excitingvideo.sdk.ExcitingVideoFragment") {
                            val obj = getField(instance, field.name)
                            runMainDelayed(1000) {
                                obj!!.runnable(instance)
                            }
                            return@after
                        }
                    }
                }
            }
    }

    private fun Any.runnable(any: Any) = Runnable {
        if (!check()) return@Runnable
        if (any is Activity) any.finish()
    }

    private fun Any.check(): Boolean {
        try {
            val method = this.javaClass.method("sendRewardWhenLiveNotAvailable")
            method.isAccessible = true
            method.invoke(this)
            return true
        } catch (_: Exception) {
        }
        return false
    }
}