package lostfound.exception;

/** 프로젝트 공통 예외의 부모. */
public class LostFoundException extends RuntimeException {
    public LostFoundException(String message) { super(message); }
    public LostFoundException(String message, Throwable cause) { super(message, cause); }
}

