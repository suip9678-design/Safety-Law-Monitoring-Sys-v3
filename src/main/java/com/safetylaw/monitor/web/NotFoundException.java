package com.safetylaw.monitor.web;

/** 요청한 항목이 없을 때. 화면에는 404 로 전달된다. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
