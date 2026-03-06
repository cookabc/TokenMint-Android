# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Model classes
-keep,includedescriptorclasses class com.chuangcius.tokenmint.data.model.** { *; }
-keepclassmembers class com.chuangcius.tokenmint.data.model.** {
    *** Companion;
    *** serializer(...);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
