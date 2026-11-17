package lostfound.privacy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PiiMaskerTest {

    @Test void 휴대폰번호를_가린다() {
        assertEquals("연락처 [MASKED]로 주세요", PiiMasker.mask("연락처 010-1234-5678로 주세요"));
        assertEquals("[MASKED]", PiiMasker.mask("01012345678"));
    }

    @Test void 카드번호를_가린다() {
        assertEquals("카드 [MASKED] 분실", PiiMasker.mask("카드 1234-5678-9012-3456 분실"));
    }

    @Test void 학번을_가린다() {
        assertEquals("학번 [MASKED] 학생증", PiiMasker.mask("학번 20231234 학생증"));
    }

    @Test void 이메일을_가린다() {
        assertEquals("[MASKED]로 연락", PiiMasker.mask("student@sunmoon.ac.kr로 연락"));
    }

    @Test void 문맥이_있는_이름을_가린다() {
        assertEquals("이름은 [MASKED]이고 검정 지갑", PiiMasker.mask("이름은 홍길동이고 검정 지갑"));
        assertEquals("[MASKED]의 학생증이 든 지갑", PiiMasker.mask("홍길동의 학생증이 든 지갑"));
    }

    @Test void 개인정보가_없으면_그대로_둔다() {
        String text = "어제 공학관 3층에서 연분홍 버즈 케이스를 잃어버렸어요";
        assertEquals(text, PiiMasker.mask(text));
    }

    @Test void 날짜와_층수는_가리지_않는다() {
        String text = "2026-11-05 오후 3층 강의실";
        assertEquals(text, PiiMasker.mask(text));
    }

    @Test void null과_빈문자열을_처리한다() {
        assertNull(PiiMasker.mask(null));
        assertEquals("", PiiMasker.mask(""));
    }

    @Test void 마스킹_개수를_센다() {
        assertEquals(2, PiiMasker.countMasked(PiiMasker.mask("010-1111-2222, 20231234")));
    }
}
