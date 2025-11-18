package com.back.motionit

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@Disabled("CI에서 불필요한 전체 컨텍스트 부팅 방지")
internal class MotionitApplicationTests {
    @Test
    fun contextLoads() {
    }
}
