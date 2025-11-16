package com.back.motionit.global.event

interface Broadcaster<T> {
    fun onCreated(event: T)
}
