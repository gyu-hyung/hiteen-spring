package kr.jiasoft.hiteen.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder

class PasswordPolicyValidatorTest {

    private lateinit var validator: PasswordPolicyValidator
    private lateinit var context: ConstraintValidatorContext
    private lateinit var builder: ConstraintViolationBuilder

    @BeforeEach
    fun setUp() {
        validator = PasswordPolicyValidator()
        context = mock(ConstraintValidatorContext::class.java)
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder::class.java)
        `when`(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder)
        `when`(builder.addConstraintViolation()).thenReturn(context)
    }

    @Test
    fun `올바른 비밀번호는 true`() {
        val pw = "Abcdef1!"
        assertTrue(validator.isValid(pw, context))
        verify(context, never()).buildConstraintViolationWithTemplate(anyString())
    }

    @Test
    fun `너무 짧으면 false`() {
        val pw = "Abc1!"
        assertFalse(validator.isValid(pw, context))
        verify(context).buildConstraintViolationWithTemplate("비밀번호는 8자 이상이어야 합니다.")
        verify(builder).addConstraintViolation()
    }

    @Test
    fun `대문자 없으면 false`() {
        val pw = "abcdef1!"
        assertFalse(validator.isValid(pw, context))
        verify(context).buildConstraintViolationWithTemplate("비밀번호에 대문자가 포함되어야 합니다.")
        verify(builder).addConstraintViolation()
    }

    @Test
    fun `숫자 없으면 false`() {
        val pw = "Abcdefg!"
        assertFalse(validator.isValid(pw, context))
    }

    @Test
    fun `특수문자 없으면 false`() {
        val pw = "Abcdef12"
        assertFalse(validator.isValid(pw, context))
    }

    @Test
    fun `연속 숫자 3개 이상이면 false`() {
        val pw = "Abc111!"
        assertFalse(validator.isValid(pw, context))
    }

    @Test
    fun `연속 문자 3개 이상이면 false`() {
        val pw = "AAAbc1!"
        assertFalse(validator.isValid(pw, context))
    }
}
