package io.realworld.domain.spi

import io.realworld.domain.api.dto.UserDto

data class UserModel(
  val email: String,
  val token: String,
  val username: String,
  val password: String,
  val bio: String? = null,
  val image: String? = null
) {
  fun toDto() = UserDto(email = email, token = token, username = username, bio = bio, image = image)
}

interface UserRepository {
  fun save(user: UserModel): UserModel
  fun findByEmail(email: String): UserModel?
}