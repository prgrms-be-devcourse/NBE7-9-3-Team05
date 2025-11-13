package com.back.motionit.global.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity protected constructor(
    @JvmField
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
) {
    @CreatedDate
    @Column(name = "create_date", updatable = false)
    var createDate: LocalDateTime? = null

    @LastModifiedDate
    @Column(name = "modify_date")
    var modifyDate: LocalDateTime? = null

    // === Java 호환 Getter ===
    fun getId(): Long? = id
}
