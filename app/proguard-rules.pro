-keep class com.hujiayucc.hook.ModuleMain { *; }
-keep @interface com.hujiayucc.hook.annotation.Run
-keep @interface com.hujiayucc.hook.annotation.RunJiaGu
-keep @com.hujiayucc.hook.annotation.Run class * { *; }
-keep @com.hujiayucc.hook.annotation.RunJiaGu class * { *; }
-dontwarn **

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*