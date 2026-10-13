# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room entities are also serialized by Gson in backups.
-keep class com.meettrack.app.data.model.Client { *; }
-keep class com.meettrack.app.data.model.Meeting { *; }
-keep enum com.meettrack.app.data.model.MeetingStatus { *; }
-keep class com.meettrack.app.data.backup.BackupPayload { *; }

-keepattributes Signature, *Annotation*
