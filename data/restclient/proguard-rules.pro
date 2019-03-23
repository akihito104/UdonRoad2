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

-keepattributes Signature
-dontwarn java.lang.invoke.*

# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# twitter4j
-dontwarn twitter4j.**
-keep class twitter4j.conf.PropertyConfigurationFactory { *; }
-keep class twitter4j.TwitterImpl { *; }
-keep class twitter4j.AlternativeHttpClientImpl { *; }
-keep class twitter4j.DispatcherImpl { *; }
-keep class twitter4j.LoggerFactory { *; }
-keep class twitter4j.conf.ConfigurationFactory { *; }
-keep class twitter4j.conf.Configuration { *; }
