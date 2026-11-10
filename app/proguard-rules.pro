# NewPipeExtractor finds YouTube's streams through reflection and an embedded Rhino JS engine, and
# both break silently when minified: every lookup returns nothing in a release build.
-keepattributes Signature, *Annotation*, Exceptions, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**
-dontwarn javax.script.**
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# kotlinx.serialization: the generated serializers are looked up by name.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.verza.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.verza.** {
    *** Companion;
}

# R8's full mode stripped parts of Media3's session and notification code: the notification lost its
# media style and the lock screen its controls. Kept whole.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Soulseek: bridge.py calls the listener's onEvent by name through Chaquopy
-keep class com.verza.data.downloads.Soulseek$Listener { *; }
