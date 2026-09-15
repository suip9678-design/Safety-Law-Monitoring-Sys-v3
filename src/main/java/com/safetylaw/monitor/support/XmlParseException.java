package com.safetylaw.monitor.support;

/** XML 을 해석하지 못했을 때. */
public class XmlParseException extends RuntimeException {

    public XmlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
