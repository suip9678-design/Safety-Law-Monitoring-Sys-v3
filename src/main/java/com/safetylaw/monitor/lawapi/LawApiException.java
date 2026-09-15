package com.safetylaw.monitor.lawapi;

/** 법령 API 호출 또는 응답 해석에 실패했을 때. */
public class LawApiException extends RuntimeException {

    public LawApiException(String message) {
        super(message);
    }

    public LawApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
