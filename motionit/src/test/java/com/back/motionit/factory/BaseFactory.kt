package com.back.motionit.factory

import net.datafaker.Faker

open class BaseFactory {
    protected val faker: Faker = Faker()
}
