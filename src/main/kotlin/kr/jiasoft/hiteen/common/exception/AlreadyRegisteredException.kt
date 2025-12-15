package kr.jiasoft.hiteen.common.exception

class AlreadyRegisteredException(
    message: String = "이미 가입된 번호야~"
) : RuntimeException(message)
