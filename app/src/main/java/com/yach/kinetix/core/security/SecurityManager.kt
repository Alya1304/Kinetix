package com.yach.kinetix.core.security

import at.favre.lib.crypto.bcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    private val cost = 12

    /**
     * Hashea un NIP o contraseña con BCrypt utilizando un Salt seguro generado dinámicamente.
     */
    fun hashPin(pin: String): String {
        return BCrypt.withDefaults().hashToString(cost, pin.toCharArray())
    }

    /**
     * Valida si el NIP ingresado coincide con el hash almacenado.
     */
    fun verifyPin(pin: String, hash: String): Boolean {
        val result = BCrypt.verifyer().verify(pin.toCharArray(), hash.toCharArray())
        return result.verified
    }
}
