# Pleos Connect SDK: AIDL, 리플렉션, gson 직렬화를 쓰므로 이름을 바꾸지 않는다
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-keep class ai.umos.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.sun.jna.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontwarn ai.pleos.**
-dontwarn com.sun.jna.**
-dontwarn java.awt.**
