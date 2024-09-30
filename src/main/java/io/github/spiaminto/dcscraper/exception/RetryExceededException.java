package io.github.spiaminto.dcscraper.exception;


public class RetryExceededException extends RuntimeException {

    public RetryExceededException(String message) {
        super(message);
    }

    public RetryExceededException(String message, Throwable cause) {
        super(message, cause);
    }

}
