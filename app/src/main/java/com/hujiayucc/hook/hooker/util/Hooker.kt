package com.hujiayucc.hook.hooker.util

import android.app.Application
import android.content.Context
import android.util.Log
import com.hujiayucc.hook.ModuleMain.Companion.module
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
    companion object {
        lateinit var classLoader: ClassLoader
            private set
    }

    private object UnsetResult

    protected lateinit var appName: String
    protected lateinit var action: String

    abstract fun XposedModuleInterface.PackageReadyParam.onPackageReady()
    fun call(param: XposedModuleInterface.PackageReadyParam) {
        try {
            classLoader = param.classLoader
            appName = this::class.java.annotations.filterIsInstance<Run>().first().appName
            action = this::class.java.annotations.filterIsInstance<Run>().first().action
        } catch (_: NoSuchElementException) {
            runCatching {
                appName = this::class.java.annotations.filterIsInstance<RunJiaGu>().first().appName
                action = this::class.java.annotations.filterIsInstance<RunJiaGu>().first().action
                module.hook(Application::class.java.method("attachBaseContext"))
                    .intercept {
                        val context = it.args[0] as Context
                        classLoader = context.classLoader
                        module.log(Log.INFO, "Fuck AD", "Set $appName real classloader.")
                        it.proceed()
                    }
            }
        }

        param.onPackageReady()
        runCatching { module.log(Log.INFO, "Fuck AD", "$appName => $action") }
    }

    fun String.toClass(): Class<*> {
        return classLoader.loadClass(this)
    }

    fun String.toClassOrNull(): Class<*>? {
        return try {
            classLoader.loadClass(this)
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

    val HookCallback.instance: Any get() = chain.thisObject
    val HookCallback.args: List<Any?> get() = chain.args
    fun <T> HookCallback.instance(): T = chain.thisObject as T
}