package com.back.motionit.support

import com.back.motionit.global.service.AwsCdnSignService
import com.back.motionit.global.service.AwsS3Service
import com.theokanning.openai.service.OpenAiService
import org.springframework.test.context.bean.override.mockito.MockitoBean

@IntegrationTest
class BaseIntegrationTest {
    @MockitoBean
    var awsCdnSignService: AwsCdnSignService? = null

    @MockitoBean
    var openAiService: OpenAiService? = null
}
