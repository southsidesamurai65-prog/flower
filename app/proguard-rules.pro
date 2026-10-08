# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.example.flowerid.**$$serializer { *; }
-keepclassmembers class com.example.flowerid.** { *** Companion; }
-keepclasseswithmembers class com.example.flowerid.** { kotlinx.serialization.KSerializer serializer(...); }
