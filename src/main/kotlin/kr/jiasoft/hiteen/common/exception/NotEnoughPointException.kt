package kr.jiasoft.hiteen.common.exception

class NotEnoughPointException(
    message: String = "포인트가 부족합니다."
) : RuntimeException(message)
