package com.yach.kinetix.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabasePassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefName = "secure_db_prefs"
    private val keyPassphrase = "db_passphrase"

    /**
     * Obtiene o genera una frase de paso criptográfica aleatoria protegida por Android Keystore.
     */
    fun getPassphrase(): ByteArray {
        val prefs = getEncryptedPreferences()
        val existingPassphrase = prefs.getString(keyPassphrase, null)
        return if (existingPassphrase != null) {
            existingPassphrase.toByteArray(Charsets.UTF_8)
        } else {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            val newPassphrase = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            prefs.edit().putString(keyPassphrase, newPassphrase).apply()
            newPassphrase.toByteArray(Charsets.UTF_8)
        }
    }

    private fun getEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                prefName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback de contingencia en caso de entornos de testing o dispositivos sin Keystore hardware
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        }
    }
}
