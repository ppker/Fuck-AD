package com.hujiayucc.hook.hooker.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hujiayucc.hook.ModuleMain.Companion.module
import com.hujiayucc.hook.ModuleMain.Companion.prefs
import com.hujiayucc.hook.annotation.Run
import com.hujiayucc.hook.annotation.RunJiaGu
import com.hujiayucc.hook.hooker.sdk.GDT
import com.hujiayucc.hook.hooker.sdk.KW
import com.hujiayucc.hook.hooker.sdk.Pangle
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
abstract class Hooker {
    private object UnsetResult

    protected lateinit var appName: String
    protected lateinit var action: String
    protected var classLoader: ClassLoader? = null

    abstract fun XposedModuleInterface.PackageReadyParam.onPackageReady()
    fun call(param: XposedModuleInterface.PackageReadyParam) {
        classLoader = param.classLoader
        var isJiaGu = false
        this.javaClass.annotations.forEach { annotation ->
            when (annotation) {
                is Run -> {
                    appName = annotation.appName
                    action = annotation.action
                }

                is RunJiaGu -> {
                    appName = annotation.appName
                    action = annotation.action
                    isJiaGu = true
                }
            }
        }

        if (isJiaGu) {
            try {
                classLoader = getRealClassLoader()
            } catch (e: Exception) {
                if (prefs.getBoolean("errorLog", false))
                    logE("Failed to get real ClassLoader for $appName", e)
            }
        }

        param.runHook()
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun getRealClassLoader(): ClassLoader {
        val threadCl = Thread.currentThread().contextClassLoader
        if (threadCl != null && threadCl !== classLoader) return threadCl

        var realClassLoader: ClassLoader? = null
        module.hook(Application::class.java.method("attachBaseContext")).intercept { chain ->
            val result = chain.proceedWith(chain.thisObject, chain.args.toTypedArray())
            val context = chain.args[0] as Context
            realClassLoader = context.classLoader
            result
        }

        if (realClassLoader != null && realClassLoader !== classLoader) return realClassLoader

        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getDeclaredMethod("currentApplication").invoke(null) as? Application
            currentApp?.classLoader?.let { appCl ->
                if (appCl !== classLoader) return appCl
            }
        } catch (_: Throwable) {}

        return classLoader!!
    }

    fun XposedModuleInterface.PackageReadyParam.runHook() {
        onPackageReady()
        runCatching { module.log(Log.INFO, "Fuck AD", "$appName => $action") }
    }

    @Throws(ClassNotFoundException::class)
    fun String.toClass(): Class<*> {
        return classLoader?.loadClass(this) ?: throw ClassNotFoundException(this)
    }

    fun String.toClassOrNull(): Class<*>? {
        return try {
            classLoader?.loadClass(this)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    fun Class<*>.method(name: String, vararg parameterTypes: Class<*>): Method {
        if (parameterTypes.isEmpty()) {
            return declaredMethods.first { it.name == name }
        }
        return getDeclaredMethod(name, *parameterTypes)
    }

    fun Class<*>.constructor(): Array<out Constructor<*>>? {
        return try {
            declaredConstructors
        } catch (_: NoSuchMethodException) {
            null
        }
    }

    fun Class<*>.methods(name: String): List<Method> {
        return declaredMethods.filter { it.name == name }
    }

    fun Class<*>.methodContains(name: String): List<Method> {
        return declaredMethods.filter { it.name.contains(name) }
    }

    class HookDsl internal constructor() {
        internal var replaceBlock: (HookCallback.() -> Any?)? = null
        internal var replaceUnitBlock: (HookCallback.() -> Unit)? = null
        internal var beforeBlock: (HookCallback.() -> Unit)? = null
        internal var afterBlock: (HookCallback.() -> Unit)? = null

        fun replace(block: HookCallback.() -> Any?) {
            replaceBlock = block
        }

        fun replaceTo(value: Any?) {
            replaceBlock = { value }
        }

        fun replaceUnit(block: HookCallback.() -> Unit = {}) {
            replaceUnitBlock = block
        }

        fun before(block: HookCallback.() -> Unit) {
            beforeBlock = block
        }

        fun after(block: HookCallback.() -> Unit) {
            afterBlock = block
        }
    }

    private fun Executable.hookInternal(dsl: HookDsl) {
        module.hook(this).intercept { hookChain ->
            var currentResult: Any? = UnsetResult
            var originalExecuted = false
            val callback = object : HookCallback {
                override val chain: XposedInterface.Chain
                    get() = hookChain
                override var result: Any?
                    get() = if (currentResult === UnsetResult) null else currentResult
                    set(value) {
                        currentResult = value
                    }
            }

            dsl.replaceBlock?.let { replace ->
                return@intercept replace.invoke(callback)
            }
            dsl.replaceUnitBlock?.let { replaceUnit ->
                replaceUnit.invoke(callback)
                return@intercept null
            }

            dsl.beforeBlock?.invoke(callback)
            if (currentResult === UnsetResult) {
                originalExecuted = true
                callback.result = hookChain.proceedWith(hookChain.thisObject, hookChain.args.toTypedArray())
            }

            if (originalExecuted) {
                dsl.afterBlock?.invoke(callback)
            }
            callback.result
        }
    }

    fun Method.hook(block: HookDsl.() -> Unit) {
        val dsl = HookDsl().apply(block)
        val hookBlockCount = listOf(
            dsl.replaceBlock,
            dsl.replaceUnitBlock,
            dsl.beforeBlock,
            dsl.afterBlock
        ).count { it != null }
        require(hookBlockCount == 1) {
            "Hook DSL requires exactly one of replace/replaceTo/replaceUnit/before/after."
        }
        hookInternal(dsl)
    }

    fun Constructor<*>.hook(block: HookDsl.() -> Unit) {
        val dsl = HookDsl().apply(block)
        val hookBlockCount = listOf(
            dsl.replaceBlock,
            dsl.replaceUnitBlock,
            dsl.beforeBlock,
            dsl.afterBlock
        ).count { it != null }
        require(hookBlockCount == 1) {
            "Hook DSL requires exactly one of replace/replaceTo/replaceUnit/before/after."
        }
        hookInternal(dsl)
    }

    fun List<Method>?.hook(block: HookDsl.() -> Unit) {
        if (this.isNullOrEmpty()) return
        asSequence()
            .distinctBy { it.toGenericString() }
            .forEach { method -> method.hook(block) }
    }

    fun Array<Method>?.hook(block: HookDsl.() -> Unit) {
        if (this.isNullOrEmpty()) return
        asSequence()
            .distinctBy { it.toGenericString() }
            .forEach { method -> method.hook(block) }
    }

    fun getField(obj: Any, fieldName: String): Any? {
        return obj.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.get(obj)
    }

    fun setField(obj: Any, fieldName: String, value: Any?) {
        obj.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.set(obj, value)
    }

    fun loadSdk(
        param: XposedModuleInterface.PackageReadyParam,
        pangle: Boolean = false,
        gdt: Boolean = false,
        kw: Boolean = false
    ) {
        if (pangle) Pangle.call(param)
        if (gdt) GDT.call(param)
        if (kw) KW.call(param)
    }

    fun loadAllSDK(param: XposedModuleInterface.PackageReadyParam) {
        GDT.call(param)
        KW.call(param)
        Pangle.call(param)
    }

    protected fun logI(message: String, throwable: Throwable? = null) {
        module.log(Log.INFO, "Fuck AD", message, throwable)
    }

    protected fun logD(message: String, throwable: Throwable? = null) {
        module.log(Log.DEBUG, "Fuck AD", message, throwable)
    }

    protected fun logE(message: String, throwable: Throwable? = null) {
        module.log(Log.ERROR, "Fuck AD", message, throwable)
    }

    protected fun logW(message: String, throwable: Throwable? = null) {
        module.log(Log.WARN, "Fuck AD", message, throwable)
    }

    protected inline fun runMain(crossinline function: () -> Unit) {
        Handler(Looper.getMainLooper()).post { function() }
    }

    protected inline fun runMainDelayed(delayMillis: Long, crossinline function: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({ function() }, delayMillis)
    }

    val HookCallback.instance: Any get() = chain.thisObject
    val HookCallback.args: List<Any?> get() = chain.args
    fun <T> HookCallback.instance(): T = chain.thisObject as T
}