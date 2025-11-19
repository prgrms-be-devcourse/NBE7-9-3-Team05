package com.back.motionit.domain.user.repository

import com.back.motionit.domain.user.dto.QUserLoginProjection
import com.back.motionit.domain.user.dto.UserLoginProjection
import com.back.motionit.domain.user.entity.QUser.user
import com.querydsl.jpa.impl.JPAQueryFactory


class UserRepositoryImpl(
    private val query: JPAQueryFactory
) : UserRepositoryCustom {

    override fun findLoginUserByEmail(email: String): UserLoginProjection? {
        return query
            .select(
                QUserLoginProjection(
                    user.id,
                    user.email,
                    user.password,
                    user.nickname
                )
            )
            .from(user)
            .where(user.email.eq(email))
            .fetchOne()
    }
}