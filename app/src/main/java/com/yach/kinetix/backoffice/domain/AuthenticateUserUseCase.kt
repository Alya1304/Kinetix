package com.yach.kinetix.backoffice.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.UserDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.backoffice.data.entity.UserEntity
import com.yach.kinetix.core.security.SecurityManager
import javax.inject.Inject

class AuthenticateUserUseCase @Inject constructor(
    private val userDao: UserDao,
    private val securityManager: SecurityManager,
    private val auditLogDao: AuditLogDao
) {

    /**
     * Valida el NIP del usuario contra el hash BCrypt almacenado y retorna el usuario autenticado.
     */
    suspend operator fun invoke(username: String, pin: String): Result<UserEntity> = runCatching {
        val user = userDao.getUserByUsername(username)
            ?: throw IllegalArgumentException("Usuario o NIP incorrecto")

        val isValid = securityManager.verifyPin(pin, user.pinHash)
        if (!isValid) {
            throw IllegalArgumentException("Usuario o NIP incorrecto")
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = user.id,
                action = "USER_LOGIN_SUCCESS",
                entityName = "User",
                entityId = user.id.toString(),
                details = "Inicio de sesión exitoso para ${user.username} con rol ${user.role}"
            )
        )

        user
    }
}
