package com.team04.domain.payment.exception;

/** 지급대행 재시도 대상 실패를 표현하는 내부 예외입니다. */
public class PayoutRetryException extends RuntimeException {

    public PayoutRetryException(String message) {
        super(message);
    }

    public PayoutRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
