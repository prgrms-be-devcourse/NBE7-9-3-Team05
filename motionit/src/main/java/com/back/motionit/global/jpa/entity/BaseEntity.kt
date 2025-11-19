package com.back.motionit.global.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    @CreatedDate
    @Column(name = "create_date", updatable = false)
    lateinit var createDate: LocalDateTime

    @LastModifiedDate
    @Column(name = "modify_date")
    lateinit var modifyDate: LocalDateTime
}