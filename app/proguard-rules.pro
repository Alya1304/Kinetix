# Restringir únicamente a modelos de Room o entidades que lo requieran explícitamente
-keep class com.yach.kinetix.**.data.entity.** { *; }
-keep class * implements androidx.room.RoomDatabase { *; }