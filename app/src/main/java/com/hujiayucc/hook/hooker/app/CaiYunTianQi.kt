package com.hujiayucc.hook.hooker.app

import com.hujiayucc.hook.annotation.Run
import com.hujiayucc.hook.hooker.util.Hooker
import io.github.libxposed.api.XposedModuleInterface

@Run(
    appName = "彩云天气",
    packageName = "com.nowcasting.activity",
    action = "解锁会员"
)
object CaiYunTianQi : Hooker() {
    override fun XposedModuleInterface.PackageReadyParam.onPackageReady() {
        val userInfo = "com.nowcasting.entity.UserInfo".toClassOrNull()
        // 设置是会员
        userInfo?.method("setVIP")
            ?.hook {
                replaceUnit {
                    val mArgs = chain.args.toMutableList()
                    mArgs[0] = true
                    chain.proceedWith(chain.thisObject, mArgs.toTypedArray())
                }
            }
        // 设置会员类型 超级会员
        userInfo?.method("setVip_type")
            ?.hook {
                replaceUnit {
                    val mArgs = chain.args.toMutableList()
                    mArgs[0] = "svip"
                    chain.proceedWith(chain.thisObject, mArgs.toTypedArray())
                }
            }
        // 设置超级会员到期时间
        userInfo?.method("setSvip_expired_at")
            ?.hook {
                replaceUnit {
                    val mArgs = chain.args.toMutableList()
                    mArgs[0] = 4701859200L
                    chain.proceedWith(chain.thisObject, mArgs.toTypedArray())
                }
            }
    }
}
