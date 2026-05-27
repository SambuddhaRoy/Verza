# ── LSTN ProGuard / R8 rules ──────────────────────────────────────────────────

# NewPipeExtractor performs YouTube stream extraction via heavy reflection and an
# embedded Mozilla Rhino JS engine (for signature/n-parameter deciphering). Both must
# survive minification or stream resolution silently breaks in release builds.
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Mozilla Rhino (bundled by NewPipe for running base.js cipher functions)
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**

# NewPipe transitive parsers
-dontwarn org.jsoup.**
-keep class com.grack.nanojson.** { *; }

# Rhino exposes an optional javax.script bridge we don't use.
-dontwarn javax.script.**

# Kotlinx serialization (InnerTube models are @Serializable)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.lstn.innertube.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3 / ExoPlayer keeps its own consumer rules; nothing extra needed here.
