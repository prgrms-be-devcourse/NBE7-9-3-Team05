package com.back.motionit.security.jwt

import com.back.motionit.domain.user.entity.User
import com.back.motionit.standard.ut.JwtUtil
import com.back.motionit.standard.ut.JwtUtil.Jwt.isExpired
import com.back.motionit.standard.ut.JwtUtil.Jwt.payloadOrNull
import com.back.motionit.standard.ut.JwtUtil.Jwt.toString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import javax.crypto.SecretKey

@ExtendWith(MockitoExtension::class)
class JwtTokenProviderTest {

    private lateinit var provider: JwtTokenProvider
    private lateinit var secretKey: SecretKey

    private val secret = "c2VjdXJlLXRlc3Qta2V5LXNlY3VyZS10ZXN0LWtleS0xMjM0NTY="
    private val accessExp = 3600L
    private val refreshExp = 7200L

    @BeforeEach
    fun setup() {
        provider = JwtTokenProvider(secret, accessExp, refreshExp)

        secretKey = provider.javaClass.getDeclaredField("secretKey")
            .apply { isAccessible = true }
            .get(provider) as SecretKey
    }

    private fun <T> mockJwt(block: (MockedStatic<JwtUtil.Jwt>) -> T): T =
        mockStatic(JwtUtil.Jwt::class.java).use(block)

    @Test
    @DisplayName("Access 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
    fun generateAccessToken_Success() {
        val user = User(1L, "testUser")

        mockJwt { mocked ->
            mocked.`when`<Any?> {
                toString(
                    secretKey,
                    accessExp,
                    mapOf("id" to 1L, "nickname" to "testUser")
                )
            }.thenReturn("mock-access-token")

            val token = provider.generateAccessToken(user)
            assertThat(token).isEqualTo("mock-access-token")
        }
    }

    @Test
    @DisplayName("Refresh 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
    fun generateRefreshToken_Success() {
        val user = User(1L, "testUser")

        mockJwt { mocked ->
            mocked.`when`<Any?> {
                toString(
                    secretKey,
                    refreshExp,
                    mapOf("id" to 1L, "nickname" to "testUser")
                )
            }.thenReturn("mock-refresh-token")

            val token = provider.generateRefreshToken(user)
            assertThat(token).isEqualTo("mock-refresh-token")
        }
    }

    @Test
    @DisplayName("payloadOrNull - JwtUtil에서 payload 반환 시 정상 변환")
    fun payloadOrNull_Success() {
        val fakePayload = mapOf("id" to 1, "nickname" to "testUser")

        mockJwt { mocked ->
            mocked.`when`<Any?> { payloadOrNull("validToken", secretKey) }
                .thenReturn(fakePayload)

            val result = provider.payloadOrNull("validToken")

            assertThat(result!!["id"]).isEqualTo(1L)
            assertThat(result["nickname"]).isEqualTo("testUser")
        }
    }

    @Test
    @DisplayName("payloadOrNull - null 반환 시 null 반환")
    fun payloadOrNull_ReturnsNull() {
        mockJwt { mocked ->
            mocked.`when`<Any?> { payloadOrNull("invalidToken", secretKey) }
                .thenReturn(null)

            val result = provider.payloadOrNull("invalidToken")
            assertThat(result).isNull()
        }
    }

    @Test
    @DisplayName("isExpired - JwtUtil의 isExpired 결과를 그대로 반환")
    fun isExpired_Success() {
        mockJwt { mocked ->
            mocked.`when`<Any?> { isExpired("expiredToken", secretKey) }
                .thenReturn(true)

            val result = provider.isExpired("expiredToken")
            assertThat(result).isTrue()
        }
    }
}
