package com.back.motionit.global.outbox.repository

import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.outbox.entity.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {
    fun findTop100ByStatusOrderById(status: OutboxStatus): List<OutboxEvent>
}
