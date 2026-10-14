package ru.it.novelforge.domain.model

data class UserAccount(
    val id: String,
    val email: String,
    val displayName: String = ""
)

data class RegistrationPending(
    val email: String,
    val resendAfterSeconds: Int = 60
)

class EmailVerificationRequiredException(
    val email: String
) : Exception("Email verification required")

class EmailAlreadyRegisteredException : Exception("Email already registered")
class InvalidVerificationCodeException : Exception("Invalid verification code")
class VerificationCodeExpiredException : Exception("Verification code expired")
class VerificationAttemptsExceededException : Exception("Verification attempts exceeded")
class VerificationResendTooSoonException(
    val retryAfterSeconds: Int? = null
) : Exception("Verification resend too soon")
class VerificationSendLimitException(
    val retryAfterSeconds: Int? = null
) : Exception("Verification send limit reached")
class EmailDeliveryException : Exception("Verification email delivery failed")
