package com.hujiayucc.hook.hook.sdk

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker


/** 腾讯广告 */
object Tencent : YukiBaseHooker() {
    override fun onHook() {
        findClass("com.qq.e.comm.managers.GDTADManager").hook {
            injectMember {
                method { name = "isInitialized" }
                replaceToFalse()
            }

            injectMember {
                method { name = "getInstance" }
                replaceTo(null)
            }

            injectMember {
                method { name = "initWith" }
                replaceToFalse()
            }
        }.ignoredHookClassNotFoundFailure()

        findClass("com.qq.e.comm.constants.CustomPkgConstants").hook {
            injectMember {
                method { name = "getAssetPluginDir" }
                replaceTo("")
            }

            injectMember {
                method { name = "getAssetPluginName" }
                replaceTo("")
            }

            injectMember {
                method { name = "getADActivityName" }
                replaceTo("")
            }

            injectMember {
                method { name = "getADActivityClass" }
                replaceTo(null)
            }
        }.ignoredHookClassNotFoundFailure()
    }
}