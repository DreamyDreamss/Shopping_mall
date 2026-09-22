// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ZipcodeDao;
import com.sm.lab.shop.domain.Zipcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 우편번호(도로명) 검색(SR-235, INF-MBR-009) — 검색어 정규화(trim + 연속 공백 한 칸, 사람 수정
 * (1)) + 길이 검증(정규화 후 2~50자, 벗어나면 400 {@code MBR-4202}) + 숫자 판별(정규화 후 값이
 * 숫자로만 구성되면 zipcode 전방일치 분기, 사람 수정 (2)) 후 DAO 위임.
 *
 * <p>트랜잭션 불필요 — 단일 {@code SELECT} 조회뿐이라 {@code @Transactional}을 붙이지 않는다
 * ({@code MemberAddressService.list()}와 동일 판단, STORY "데이터" 절).
 */
@Service
public class ZipcodeService {

    static final String CODE_VALIDATION = "MBR-4202";
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;

    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");
    private static final Pattern NUMERIC_ONLY = Pattern.compile("^[0-9]+$");

    private final ZipcodeDao dao;

    @Autowired
    public ZipcodeService(ZipcodeDao dao) {
        this.dao = dao;
    }

    /**
     * 검색 — 검증 순서(STORY "순서·보안" 절): 정규화 → 길이 검증(400, DB 접근 전) → 숫자 판별 →
     * 조회. 결과 0건도 예외 없이 빈 리스트 그대로 반환(존재 판정이 없는 전역 참조데이터 검색).
     */
    public List<Zipcode> search(String q) {
        String normalized = normalize(q);
        if (normalized.length() < MIN_LENGTH) {
            throw validationError("검색어는 최소 " + MIN_LENGTH + "자 이상이어야 합니다");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw validationError("검색어는 최대 " + MAX_LENGTH + "자까지 입력할 수 있습니다");
        }

        boolean numeric = NUMERIC_ONLY.matcher(normalized).matches();
        return dao.search(normalized, numeric);
    }

    /**
     * trim 후 연속 공백을 한 칸으로 정규화(사람 수정 (1)). {@code q}가 {@code null}이면 빈
     * 문자열로 취급해 아래 길이 검증(2자 미만)에서 걸러지게 한다.
     */
    private static String normalize(String q) {
        if (q == null) {
            return "";
        }
        return WHITESPACE_RUN.matcher(q.trim()).replaceAll(" ");
    }

    private static ZipcodeApiException validationError(String message) {
        return new ZipcodeApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, message);
    }
}
