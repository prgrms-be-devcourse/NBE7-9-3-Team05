package com.back.motionit.global.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null   // ← 반드시 var + open

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    open var createDate: LocalDateTime? = null   // ← 반드시 var + open

    @LastModifiedDate
    @Column(name = "modify_date")
    open var modifyDate: LocalDateTime? = null   // ← 반드시 var + open

    // Hibernate가 호출할 no-arg constructor 필요
    protected constructor()

    // Kotlin(or Java)에서 id 설정하는 생성자도 허용
    protected constructor(id: Long?) {
        this.id = id
    }
}