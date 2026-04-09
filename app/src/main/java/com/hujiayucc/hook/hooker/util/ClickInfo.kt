package com.hujiayucc.hook.hooker.util

import android.view.View
import android.widget.TextView
import com.hujiayucc.hook.ModuleMain
import com.hujiayucc.hook.utils.AppInfoUtil
import io.github.libxposed.api.XposedModuleInterface

object ClickInfo : Hooker() {
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        View::class.java.method("performClick")
            .hook {
                before {
                    if (click) printInfo(instance as View)
                    if (stackTrack) printStackTrace(Throwable("堆栈信息"))
                }
            }

        "android.view.View.DeclaredOnClickListener".toClassOrNull()
            ?.method("onClick")
            ?.hook {
                before {
                    if (click) printInfo(instance as View)
                    if (stackTrack) printStackTrace(Throwable("堆栈信息"))
                }
            }
    }

    val click: Boolean get() = ModuleMain.prefs.getBoolean("clickInfo", false)
    val stackTrack: Boolean get() = ModuleMain.prefs.getBoolean("stackTrack", false)

    private fun printStackTrace(throwable: Throwable) {
        logD("StackTrace:", throwable)
    }

    private fun printInfo(view: View) {
        // 输出完整信息
        logD(
            """
                ====== 点击事件详情 ======
                View 类: ${view::class.java.name}
                View 父类：${view.javaClass.superclass?.name ?: "Unknown"}
                View ID: 0x${view.id.toHexString()} ${AppInfoUtil.getResourceName(view, view.id)}
                View 文本: ${if (view is TextView) view.text.toString() else ""}
                所在 Activity: ${AppInfoUtil.getActivityFromView(view)?.javaClass?.name ?: "Unknown"}
            """.trimIndent()
        )
    }
}