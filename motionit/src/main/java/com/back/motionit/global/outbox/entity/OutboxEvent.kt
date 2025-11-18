package com.back.motionit.global.outbox.entity

import com.back.motionit.global.enums.OutboxEventType
import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val eventType: OutboxEventType,

    @Column(nullable = false, length = 100)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: Long,

    @Lob
    @Column(nullable = false)
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OutboxStatus = OutboxStatus.PENDING,

    var lastTriedAt: LocalDateTime? = null,

    var attemptCount: Int = 0,

    @Column(length = 500)
    var lastErrorMessage: String? = null,
): BaseEntity()
