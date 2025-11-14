package com.back.motionit.domain.challenge.room.builder

import net.datafaker.Faker
import org.springframework.util.StringUtils.truncate
import java.util.*

class CreateRoomRequestBuilder(videoUrl: String?) {
    companion object {
        private val faker = Faker(Locale("ko"))
    }

    private var title: String = truncate(faker.lorem().characters(8, 30, true), 30)
    private var description: String = truncate(faker.lorem().characters(20, 100, true), 100)
    private var capacity: Int = faker.number().numberBetween(2, 100)
    private var duration: Int = faker.number().numberBetween(3, 30)
    private var videoUrl: String =
        videoUrl ?: "https://youtube.com/watch?v=${faker.regexify("[A-Za-z0-9_-]{11}")}"

    private var imageFileName: String = faker.file().fileName(null, null, "png", null)

    private var contentType = "image/png"

    fun toParamMap(): Map<String, String> =
        linkedMapOf(
            "title" to title,
            "description" to description,
            "capacity" to capacity.toString(),
            "duration" to duration.toString(),
            "videoUrl" to videoUrl,
            "imageFileName" to imageFileName,
            "contentType" to contentType
        )
}
