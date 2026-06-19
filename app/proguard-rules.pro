# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Echo keep rules
# ---------------------------------------------------------------------------

# Keep readable crash stack traces (Crashlytics deobfuscates with the mapping
# file once uploaded). Also keep annotations/signatures Firestore relies on.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Firestore deserializes documents into these entities via reflection
# (DocumentSnapshot.toObject). R8 must NOT rename/strip their no-arg
# constructor, fields, or accessors, or deserialization returns empty objects.
# (-keep class { *; } already retains all members, so a separate
# -keepclassmembers rule would be redundant.)
-keep class com.example.echo.data.entity.** { *; }