package com.hujiayucc.hook

import android.content.SharedPreferences
import android.util.Log
import com.hujiayucc.hook.hooker.sdk.GDT
import com.hujiayucc.hook.hooker.sdk.KW
import com.hujiayucc.hook.hooker.sdk.Pangle
import com.hujiayucc.hook.hooker.util.ClickInfo
import com.hujiayucc.hook.hooker.util.Hooker
import com.hujiayucc.hook.hooker.util.Loader
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

class ModuleMain : XposedModule() {
    companion object {
        init {
            System.loadLibrary("dexkit")
        }

        private const val TAG = "ModuleMain"
        private const val PREFS_NAME = "config"
        private const val BASE_APK_SUFFIX = "/base.apk"
        private const val HOOKER_PACKAGE = "com.hujiayucc.hook.hooker.app"
        val BUILTIN_HOOKERS = listOf(Loader, ClickInfo)
        val SDK_HOOKERS = listOf(GDT, KW, Pangle)

        lateinit var prefs: SharedPreferences
            private set
        lateinit var module: XposedModule
            private set
        lateinit var bridge: DexKitBridge
            private set
        var hookers: MutableList<Hooker> = mutableListOf()
            private set
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        try {
            module = this
            prefs = getRemotePreferences(PREFS_NAME)
            bridge = DexKitBridge.create(javaClass.classLoader!!, true)
        } catch (e: Exception) {
            logIfDebug("onModuleLoaded", e)
        }
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            if (!param.applicationInfo.sourceDir.endsWith(BASE_APK_SUFFIX)) return
            hookers.addAll(BUILTIN_HOOKERS)
            javaClass.classLoader?.let { classLoader ->
                bridge.findClass {
                    searchPackages(HOOKER_PACKAGE)
                    matcher {
                        annotations {
                            add {
                                addElement {
                                    name = "packageName"
                                    stringValue(param.packageName, StringMatchType.Equals)
                                }
                            }
                        }
                    }
                }.forEach { data ->
                    hookers.add(data.getInstance(classLoader).getDeclaredConstructor().newInstance() as Hooker)
                }
            }
        } catch (e: Exception) {
            logIfDebug("onPackageLoaded", e)
        }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        try {
            if (!param.applicationInfo.sourceDir.endsWith(BASE_APK_SUFFIX)) return
            if (hookers.isNotEmpty()) {
                hookers.forEach { it.call(param) }
                hookers.clear()
            } else {
                SDK_HOOKERS.forEach { it.loadSdk(param) }
            }
        } catch (e: Exception) {
            logIfDebug("onPackageReady", e)
        }
    }

    private fun logIfDebug(stage: String, error: Exception) {
        if (prefs.getBoolean("errorLog", false)) log(Log.ERROR, TAG, "$stage error", error)
    }
}
