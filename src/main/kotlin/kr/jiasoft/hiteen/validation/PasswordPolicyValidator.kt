package kr.jiasoft.hiteen.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext


class PasswordPolicyValidator : ConstraintValidator<ValidPassword, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        // 값이 없으면(=수정 안함) → 무조건 true (통과)
        //회원가입시 NotBlank 추가적용하니 무관
        //회원정보 수정 시 값이 없으면 기존 값으로 대체하니 무관
        if (value.isNullOrBlank()) return true

        if (value.length < 8) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("비밀번호는 8자 이상이어야 합니다.").addConstraintViolation()
            return false
        }
        if (!value.any { it.isUpperCase() }) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("비밀번호에 대문자가 포함되어야 합니다.").addConstraintViolation()
            return false
        }
        if (!value.any { it.isLowerCase() }) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("비밀번호에 소문자가 포함되어야 합니다.").addConstraintViolation()
            return false
        }
        if (!value.any { it.isDigit() }) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("비밀번호에 숫자가 포함되어야 합니다.").addConstraintViolation()
            return false
        }
        if (!value.any { "!@#\$%^&*()_+-=[]{}|;:',.<>?/".contains(it) }) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("비밀번호에 특수문자가 포함되어야 합니다.").addConstraintViolation()
            return false
        }
        if (Regex("(\\d)\\1{2,}").containsMatchIn(value)) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("연속된 숫자가 3개 이상 포함될 수 없습니다.").addConstraintViolation()
            return false
        }
        if (Regex("([a-zA-Z])\\1{2,}").containsMatchIn(value)) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("연속된 문자가 3개 이상 포함될 수 없습니다.").addConstraintViolation()
            return false
        }
        return true
    }
}
