package com.ssafy.dib

import com.ssafy.dib.feature.auth.SignupForm
import com.ssafy.dib.feature.auth.SignupValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupValidatorTest {
    private val validForm = SignupForm(
        phoneNumber = "01012345678",
        email = "member@example.com",
        password = "Dibpass1!0",
        name = "김디브",
        nickname = "디브러버",
        gender = "FEMALE",
        birthDate = "2000-01-31"
    )

    @Test
    fun acceptsCompleteSignupForm() {
        assertTrue(SignupValidator.isFormValid(validForm))
    }

    @Test
    fun rejectsPasswordWithoutLettersAndDigits() {
        assertFalse(SignupValidator.isFormValid(validForm.copy(password = "12345678")))
        assertFalse(SignupValidator.isFormValid(validForm.copy(password = "password")))
    }

    @Test
    fun rejectsPasswordMissingRequiredCharacterClass() {
        assertFalse(SignupValidator.isPasswordValid("dibpassword1!"))
        assertFalse(SignupValidator.isPasswordValid("DIBPASSWORD1!"))
        assertFalse(SignupValidator.isPasswordValid("Dibpassword!"))
        assertFalse(SignupValidator.isPasswordValid("Dibpassword1"))
    }

    @Test
    fun reportsEachPasswordRequirementWhileTyping() {
        val requirements = SignupValidator.passwordRequirements("Dibpassword1")

        assertEquals(listOf("10~64자", "영문 대문자", "영문 소문자", "숫자", "특수문자"), requirements.map { it.label })
        assertTrue(requirements.take(4).all { it.satisfied })
        assertFalse(requirements.last().satisfied)
    }

    @Test
    fun rejectsMalformedPhoneAndBirthDate() {
        assertFalse(SignupValidator.isFormValid(validForm.copy(phoneNumber = "010-1234-5678")))
        assertFalse(SignupValidator.isFormValid(validForm.copy(birthDate = "20000131")))
        assertFalse(SignupValidator.isFormValid(validForm.copy(birthDate = "2025-02-31")))
    }

    @Test
    fun kakaoSignupDoesNotRequirePassword() {
        assertTrue(SignupValidator.isKakaoFormValid(validForm.copy(password = "")))
    }
}
