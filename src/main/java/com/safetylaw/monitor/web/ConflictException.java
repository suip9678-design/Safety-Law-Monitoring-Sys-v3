package com.safetylaw.monitor.web;

/** 지금 상태에서는 할 수 없는 요청일 때. 화면에는 409 로 전달된다. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
