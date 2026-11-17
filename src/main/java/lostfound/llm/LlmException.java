package lostfound.llm;

import lostfound.exception.LostFoundException;

/** API 키 없음, 타임아웃, 잘못된 응답 등. 호출하는 쪽은 잡아서 대체 경로로 진행한다. */
public class LlmException extends LostFoundException {
    public LlmException(String message) { super(message); }
    public LlmException(String message, Throwable cause) { super(message, cause); }
}
