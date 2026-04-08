package com.hujiayucc.hook.hooker.sdk

import android.view.View
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

/** 穿山甲 */
object Pangle : Hooker() {
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        "com.bytedance.sdk.openadsdk.TTAdSdk".toClassOrNull()
            ?.declaredMethods?.hook {
                before {
                    result = when (chain.proceed()) {
                        is Boolean -> false
                        else -> null
                    }
                }
            }

        "com.bytedance.sdk.openadsdk.api.ln".toClassOrNull()
            ?.declaredMethods?.hook {
                before {
                    result = when (chain.proceed()) {
                        is Boolean -> false
                        else -> null
                    }
                }
            }

        "com.bytedance.sdk.openadsdk.core.AdSdkInitializerHolder".toClassOrNull()
            ?.declaredMethods?.hook {
                before {
                    result = when (chain.proceed()) {
                        is Boolean -> false
                        else -> null
                    }
                }
            }

        "com.bytedance.sdk.openadsdk.CSJConfig".toClassOrNull()
            ?.declaredMethods?.hook {
                before {
                    result = when (chain.proceed()) {
                        is Boolean -> false
                        else -> null
                    }
                }
            }

        $$"com.bytedance.sdk.openadsdk.AdSlot$Builder".toClassOrNull()
            ?.declaredMethods?.hook {
                before {
                    result = when (chain.proceed()) {
                        is Boolean -> false
                        else -> null
                    }
                }
            }

        "com.bytedance.sdk.openadsdk.core.component.splash.countdown.TTCountdownViewForCircle".toClassOrNull()
            ?.declaredMethods?.hook {
                after {
                    val view = chain.thisObject as View
                    view.performClick()
                }
            }

        "com.bytedance.sdk.openadsdk.core.component.splash.e.r$1".toClassOrNull()
            ?.method("run")?.hook {
                replaceUnit {}
            }
    }
}