package kr.jiasoft.hiteen.eloquent.dto

data class O2MInfo(
    val childClass: Class<*>,
    val fkField: String,
    val parentIdField: String,
    val parentIdOf: (Any) -> Any?,
    val childParentIdOf: (Any) -> Any?
)
