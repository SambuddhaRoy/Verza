# ── Verza ProGuard / R8 rules ──────────────────────────────────────────────────

# Attributes Rhino + NewPipe + kotlinx-serialization rely on at runtime. Without
# Signature/Exceptions/EnclosingMethod, Rhino's generic-type and exception-class
# resolution breaks during base.js compilation and stream resolution silently fails.
-keepattributes Signature, *Annotation*, Exceptions, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, Deprecated

# NewPipeExtractor performs YouTube stream extraction via heavy reflection and an
# embedded Mozilla Rhino JS engine (for signature/n-parameter deciphering). Both
# must survive minification or stream resolution silently breaks in release builds.
-keep class org.schabi.newpipe.extractor.** { *; }
-keepclassmembers class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Mozilla Rhino (bundled by NewPipe for running base.js cipher functions). Rhino
# dynamically generates Java bytecode for each JS function it compiles and uses
# reflection to load them — needs everything kept, including private members and
# subclasses of ScriptableObject the runtime creates on the fly.
-keep class org.mozilla.javascript.** { *; }
-keepclassmembers class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class * extends org.mozilla.javascript.ScriptableObject { *; }
-keep class * implements org.mozilla.javascript.Scriptable { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**

# NewPipe transitive parsers
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**

# Rhino exposes an optional javax.script bridge we don't use.
-dontwarn javax.script.**

# Enum values() / valueOf reflection — NewPipe deserialises a lot of enums.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlinx serialization (InnerTube models are @Serializable)
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.verza.innertube.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Media3 / ExoPlayer ─────────────────────────────────────────────────────────
# Media3 bundles consumer rules, but R8 full-mode (AGP 8+) has been observed to strip / obfuscate
# parts of the session + notification machinery in release builds — which silently degraded the
# rich MediaStyle notification to a plain foreground one and stopped the OS from recognising an
# active media session (no lock-screen controls, no always-on-display media, no OEM popout).
# Media3 is core to the app, so keep it wholesale; the modest size cost buys reliable OS integration.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**
