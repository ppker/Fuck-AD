package com.hujiayucc.hook.hooker.app

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.hujiayucc.hook.annotation.Run
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

@Run(
    appName = "百度网盘",
    packageName = "com.baidu.netdisk",
    action = "开屏广告",
    versions = [
        "13.8.0"
    ]
)
object BaiduPan : Hooker() {
    @SuppressLint("ResourceType")
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        loadSdk(this, pangle = true)
        "com.qumeng.advlib.__remote__.ui.elements.SplashCountdownView".toClass()
            .methods.hook {
                after {
                    val view = instance<View>()
                    if (view.isClickable) view.performClick()
                }
            }

        "com.beizi.fusion.widget.SkipView".toClass()
            .method("onTextChanged")
            .hook {
                after {
                    val view = instance<TextView>()
                    view.performClick()
                }
            }

        "com.baidu.sdk.container.widget.RectangleCountDownView".toClass()
            .methods.hook {
                after {
                    val view = instance<View>()
                    if (view.isClickable) view.performClick()
                }
            }
    }
}