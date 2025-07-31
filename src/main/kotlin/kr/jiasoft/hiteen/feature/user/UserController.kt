package kr.jiasoft.hiteen.feature.user

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/users/{id}")
    suspend fun getUser(@PathVariable id: Long): UserEntity? =
        userService.getUserById(id)

    @PostMapping
    suspend fun createUser(@RequestBody user: UserEntity): UserEntity =
        userService.createUser(user)

    @GetMapping("/auth/hi")
    suspend fun getUser1(): String =
        "hi"
}