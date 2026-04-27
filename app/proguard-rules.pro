# kotlinx.serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.matto.mcell.**$$serializer { *; }
-keepclassmembers class dev.matto.mcell.** {
    *** Companion;
}
-keepclasseswithmembers class dev.matto.mcell.** {
    kotlinx.serialization.KSerializer serializer(...);
}
