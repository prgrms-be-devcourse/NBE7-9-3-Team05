package com.back.motionit.security.jwt

import com.back.motionit.domain.user.entity.User
import com.back.motionit.standard.ut.JwtUtil
import com.back.motionit.standard.ut.JwtUtil.Jwt.isExpired
import com.back.motionit.standard.ut.JwtUtil.Jwt.payloadOrNull
import com.back.motionit.standard.ut.JwtUtil.Jwt.toString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils.setField

@ExtendWith(MockitoExtension::class)
internal class JwtTokenProviderTest {
    private fun testProvider(): JwtTokenProvider {
        return JwtTokenProvider(
            "test-secret",
            3600L,
            1209600000L
        )
    }

    @Test
    @DisplayName("Access 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
    fun generateAccessToken_Success() {

        val provider = testProvider()
        setField(provider, "secret", "test-secret")
        setField(provider, "accessTokenExpiration", 3600L)

        val user = User(1L, "testUser")

        mockStatic(JwtUtil.Jwt::class.java).use { mockedJwt ->
            mockedJwt.`when`<Any?> {
                toString(
                    "test-secret",
                    3600L,
                    mapOf("id" to 1L, "nickname" to "testUser")
                )
            }
                .thenReturn("mock-access-token")

            val token = provider.generateAccessToken(user)

            assertThat(token).isEqualTo("mock-access-token")
        }
    }

    @Test
    @DisplayName("Refresh 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
    fun generateRefreshToken_Success() {

        val provider = testProvider()
        setField(provider, "secret", "test-secret")
        setField(provider, "refreshTokenExpiration", 7200L)

        val user = User(1L, "testUser")

        mockStatic(JwtUtil.Jwt::class.java).use { mockedJwt ->
            mockedJwt.`when`<Any?> {
                toString(
                    "test-secret",
                    7200L,
                    mapOf("id" to 1L, "nickname" to "testUser")
                )
            }
                .thenReturn("mock-refresh-token")

            val token = provider.generateRefreshToken(user)

            assertThat(token).isEqualTo("mock-refresh-token")
        }
    }

    @Test
    @DisplayName("payloadOrNull - JwtUtil에서 payload 반환 시 정상 변환")
    fun payloadOrNull_Success() {
        val provider = testProvider()
        setField(provider, "secret", "test-secret")

        val fakePayload = mapOf(
            "id" to 1,
            "nickname" to "testUser"
        )

        mockStatic(JwtUtil.Jwt::class.java).use { mockedJwt ->
            mockedJwt.`when`<Any?> { payloadOrNull("validToken", "test-secret") }
                .thenReturn(fakePayload)

            val result = provider.payloadOrNull("validToken")

            assertThat(result!!["id"]).isEqualTo(1L)
            assertThat(result["nickname"]).isEqualTo("testUser")
        }
    }

    @Test
    @DisplayName("payloadOrNull - null 반환 시 null 반환")
    fun payloadOrNull_ReturnsNull() {
        val provider = testProvider()
        setField(provider, "secret", "test-secret")

        mockStatic(JwtUtil.Jwt::class.java).use { mockedJwt ->
            mockedJwt.`when`<Any?> { payloadOrNull("invalidToken", "test-secret") }
                .thenReturn(null)

            val result = provider.payloadOrNull("invalidToken")

            assertThat(result).isNull()
        }
    }

    @DisplayName("isExpired - JwtUtil의 isExpired 결과를 그대로 반환")
    @Test
    fun isExpired_Success() {
        val provider = testProvider()
        setField(provider, "secret", "test-secret")

        mockStatic(JwtUtil.Jwt::class.java).use { mockedJwt ->
            mockedJwt.`when`<Any?> { isExpired("expiredToken", "test-secret") }
                .thenReturn(true)

            val result = provider.isExpired("expiredToken")

            assertThat(result).isTrue()
        }
    }
}
