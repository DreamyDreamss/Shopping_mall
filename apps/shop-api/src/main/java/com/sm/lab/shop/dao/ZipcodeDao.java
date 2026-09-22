// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Zipcode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 우편번호(도로명) 검색(SR-235, INF-MBR-009) — 로컬 샘플 테이블 {@code ZIPCODES} 대상 단일 조회.
 * 이 테이블은 어떤 회원에도 속하지 않는 전역 참조 데이터라 존재/소유 판정이 없다(STORY "순서·보안"
 * 절 — 404 개념 자체가 없음, 결과 0건은 200 빈 배열).
 */
@Mapper
public interface ZipcodeDao {

    /**
     * 검색 — {@code numeric}이 true면(검색어가 숫자로만 구성, 사람 수정 (2)) {@code zipcode}
     * 전방일치({@code LIKE 'q%'})로, 그 외에는 {@code road_address} 부분일치({@code LIKE '%q%'})로
     * 조회한다. {@code road_address} 오름차순, {@code LIMIT 50}(응답 크기 캡 — SR에 없는 Dev
     * 판단, 안전장치).
     */
    List<Zipcode> search(@Param("q") String q, @Param("numeric") boolean numeric);
}
