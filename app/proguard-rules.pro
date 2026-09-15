# Reglas ProGuard para SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Reglas ProGuard para Room y BCrypt
-keep class androidx.room.** { *; }
-dontwarn at.favre.lib.**
