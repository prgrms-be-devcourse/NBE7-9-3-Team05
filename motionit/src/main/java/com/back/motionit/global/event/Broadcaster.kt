package com.back.motionit.global.event;

public interface Broadcaster<T> {
	void onCreated(T event);
}
