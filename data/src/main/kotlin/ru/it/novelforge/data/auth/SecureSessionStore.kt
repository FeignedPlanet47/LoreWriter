package ru.it.novelforge.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.it.novelforge.domain.model.UserAccount

@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadUser())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    fun accessToken(): String? = decrypt(preferences.getString(KEY_ACCESS, null))
    fun refreshToken(): String? = decrypt(preferences.getString(KEY_REFRESH, null))

    @Synchronized
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        user: UserAccount
    ) {
        preferences.edit()
            .putString(KEY_ACCESS, encrypt(accessToken))
            .putString(KEY_REFRESH, encrypt(refreshToken))
            .putString(KEY_USER_ID, encrypt(user.id))
            .putString(KEY_EMAIL, encrypt(user.email))
            .putString(KEY_DISPLAY_NAME, encrypt(user.displayName))
            .apply()

        _currentUser.value = user
    }

    @Synchronized
    fun clear() {
        preferences.edit().clear().apply()
        _currentUser.value = null
    }

    private fun loadUser(): UserAccount? {
        val id = decrypt(preferences.getString(KEY_USER_ID, null)) ?: return null
        val email = decrypt(preferences.getString(KEY_EMAIL, null)) ?: return null
        val displayName = decrypt(preferences.getString(KEY_DISPLAY_NAME, null)).orEmpty()
        return UserAccount(id = id, email = email, displayName = displayName)
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size in 1..255) { "Некорректная длина GCM IV" }

        val payload = ByteArray(1 + iv.size + encrypted.size)
        payload[0] = iv.size.toByte()
        System.arraycopy(iv, 0, payload, 1, iv.size)
        System.arraycopy(encrypted, 0, payload, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null

        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            if (payload.size < 3) return null

            val ivSize = payload[0].toInt() and 0xFF
            if (ivSize <= 0 || payload.size <= 1 + ivSize) return null

            val iv = payload.copyOfRange(1, 1 + ivSize)
            val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "novelforge_secure_session"
        const val KEY_ALIAS = "novelforge_session_aes_v1"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}
