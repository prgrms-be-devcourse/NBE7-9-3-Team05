package com.back.motionit

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
class MotionitApplication

fun main(args: Array<String>) {
    SpringApplication.run(MotionitApplication::class.java, *args)
}