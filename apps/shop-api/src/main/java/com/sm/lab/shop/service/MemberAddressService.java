// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberAddressDao;
import com.sm.lab.shop.domain.MemberAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 배송지 CRUD(SR-235, INF-MBR-008) — 목록/등록/수정/삭제/기본설정. 회원당 최대 10개·기본 1개·
 * 기본 삭제 시 최근 사용 순 다음 배송지 승계(STORY "구현 계획" + "사람 수정" 절, 사람 확정).
 *
 * <p><b>락 순서 고정(사례집 SR-231 r3 재발 방지)</b> — register/delete/setDefault 세 경로 모두
 * 쓰기 문장 이전에 {@link MemberAddressDao#selectByMemberIdForUpdate}를 먼저 호출하는 동일 순서를
 * 지킨다. 두 커넥션이 다른 순서로 같은 회원의 행을 잠그면 데드락이 난다.
 *
 * <p><b>존재+소유 분리 금지(사례집 SR-231 r5와 동일 계열)</b> — {@link MemberAddressDao#selectOwned}
 * 가 존재+소유를 한 SQL로 묶어 "없음"과 "남의 것"을 원천적으로 같은 결과(null→404 MBR-4041)로
 * 만든다. 두 판정을 분리하는 코드는 만들지 않는다.
 *
 * <p><b>self-invocation 함정 회피</b> — register/delete/setDefault 각각을 하나의
 * {@code @Transactional} public 메서드 안에 직선으로 적는다. 같은 빈 내부에서 별도
 * {@code @Transactional} 메서드를 다시 호출(this.내부헬퍼())하지 않는다 — Spring AOP 프록시는
 * self-invocation에 트랜잭션 어드바이스를 적용하지 못한다(STORY "프레임워크 실행 모델 함정" 절).
 *
 * <p><b>Dev 기록 — 계획과 다르게 간 지점</b>: STORY 계획은 "요청/응답 DTO는 컨트롤러 내부
 * record({@code CartController} 관례)"라 적었는데, 그 관례의 실체는 {@code CartController}가
 * 요청 record를 서비스에 그대로 넘기지 않고 필드를 풀어(primitive) 넘기는 것이다(예:
 * {@code cartService.addItem(req.memberId(), req.sku(), req.qty())}) — 이 코드베이스 어떤
 * {@code service} 클래스도 {@code controller} 패키지 타입을 import하지 않는다(역방향 의존
 * 없음). 배송지 요청은 필드가 8개라 그대로 풀면 파라미터 목록이 감당하기 어려워, 대신 이 서비스
 * 전용 내부 record({@link AddressInput})를 두고 컨트롤러가 자신의 {@code AddressRequest}를 그
 * 값으로 변환해 넘기게 했다 — 의존 방향은 여전히 controller→service만 존재한다(service가
 * controller를 import하지 않음).
 */
@Service
public class MemberAddressService {

    static final int MAX_ADDRESSES = 10;

    private static final String CODE_VALIDATION = "MBR-4200";
    private static final String CODE_NOT_FOUND = "MBR-4041";
    private static final String CODE_LIMIT_EXCEEDED = "MBR-4201";
    private static final String MESSAGE_NOT_FOUND = "배송지를 찾을 수 없습니다";

    private static final int RECIPIENT_MAX_LENGTH = 50;
    private static final int ADDRESS_MAX_LENGTH = 200;
    private static final int OPTIONAL_FIELD_MAX_LENGTH = 200;
    // 재작업 지시(3) — DDL의 phone VARCHAR(20) 상한과 동일. 정규화 전 원문 길이를 여기서 걸러
    // 두지 않으면, 하이픈을 길게 섞은 입력이 정규화 후 패턴은 통과하되 INSERT 시 원문이 컬럼
    // 폭을 넘겨 400 대신 500 MBR-5000이 된다.
    private static final int PHONE_RAW_MAX_LENGTH = 20;

    // 재작업 지시(4) — MemberRegistrationService(FUNC-member-003)와 동일한 휴대폰 형식 정규식을
    // 그대로 재사용한다(문자열 복제 제거, 가시성만 package-private로 열었다). 두 클래스 모두
    // com.sm.lab.shop.service 패키지라 새 의존을 추가하지 않는다.
    private static final Pattern ZIPCODE_PATTERN = Pattern.compile("^[0-9]{5}$");

    private final MemberAddressDao dao;
    private final Clock clock;

    @Autowired
    public MemberAddressService(MemberAddressDao dao) {
        this(dao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberRegistrationService와 동일 시임 패턴. */
    MemberAddressService(MemberAddressDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    /** 목록 — 최근 사용 순. 트랜잭션 불필요(단순 조회). */
    public List<MemberAddress> list(String memberId) {
        return dao.selectList(memberId);
    }

    /**
     * 등록 — 락(첫 문장) → 개수 제한(409, 락 이후) → 기본 강제/승계 판정 → 삽입.
     * 개수 제한 검사는 아직 아무것도 쓰기 전(롤백해도 잃을 것이 없는 사전 판정)이라 사례집
     * SR-231 r2("실패 경로에 남아야 하는 카운터를 트랜잭션에 넣지 말라")의 조건이 성립하지 않는다
     * — 그대로 옮기지 않는다.
     */
    @Transactional
    public MemberAddress register(String memberId, AddressInput input) {
        String phoneNorm = MemberRegistrationService.normalizePhone(input.phone());
        validateFields(input, phoneNorm);

        List<MemberAddress> existing = dao.selectByMemberIdForUpdate(memberId); // 락, 첫 문장
        if (existing.size() >= MAX_ADDRESSES) {
            throw limitExceeded();
        }

        boolean makeDefault = existing.isEmpty() || Boolean.TRUE.equals(input.isDefault());
        if (makeDefault && !existing.isEmpty()) {
            dao.clearDefaultForMember(memberId);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        MemberAddress address = new MemberAddress();
        address.setMemberId(memberId);
        address.setRecipient(input.recipient().trim());
        address.setPhone(input.phone());
        address.setPhoneNorm(phoneNorm);
        address.setZipcode(input.zipcode());
        address.setRoadAddress(input.roadAddress());
        address.setDetailAddress(input.detailAddress());
        address.setEntranceMethod(input.entranceMethod());
        address.setDeliveryMemo(input.deliveryMemo());
        address.setIsDefault(makeDefault ? "Y" : "N");
        // 사람 수정(1) — created_at과 같은 값으로 채운다(NULL 금지). 목록 정렬(last_used_at DESC,
        // created_at DESC)이 기본 배송지 승계 후보 선정과 같은 정렬을 쓰므로 NULL이 섞이면 순서가
        // 흔들린다. 주문에서의 갱신은 SR-255 몫(이 FUNC 범위 아님).
        address.setLastUsedAt(now);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        dao.insertAddress(address);
        return address;
    }

    /**
     * 수정(필드만, is_default 제외) — 존재+소유 확인만 하고 여러 행을 건드리지 않으므로
     * {@code selectByMemberIdForUpdate} 락은 불필요하다(STORY "데이터" 절).
     */
    @Transactional
    public MemberAddress update(String memberId, Long addressId, AddressInput input) {
        String phoneNorm = MemberRegistrationService.normalizePhone(input.phone());
        validateFields(input, phoneNorm);

        MemberAddress owned = dao.selectOwned(memberId, addressId);
        if (owned == null) {
            throw notFound();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        dao.updateAddress(memberId, addressId, input.recipient().trim(), input.phone(), phoneNorm, input.zipcode(),
                input.roadAddress(), input.detailAddress(), input.entranceMethod(), input.deliveryMemo(), now);
        return dao.selectOwned(memberId, addressId);
    }

    /**
     * 삭제 — 존재+소유(먼저, 싸게 실패) → 락(두 번째 문장, register/setDefault와 동일 지점) →
     * 소프트 삭제 → 삭제 대상이 기본이었으면 승계(후보가 없으면(마지막 남은 배송지) 승계 없음 —
     * NPE 방지).
     *
     * <p>재작업 지시(round 2, QA CONCERNS 권고#1) — 삭제 대상의 {@code is_default}와 승계 후보는
     * {@code selectByMemberIdForUpdate}(락, 최신 커밋본)가 반환한 목록에서 고른다. {@code
     * selectOwned}는 존재+소유 확인(먼저, 싸게 실패)에만 쓰고 그 결과의 {@code isDefault}는 승계
     * 판정에 쓰지 않는다 — MariaDB REPEATABLE READ에서 트랜잭션 스냅샷은 첫 일반 SELECT(그
     * {@code selectOwned})에서 고정되므로, 락 대기 중 커밋된 타 트랜잭션(동시 setDefault/register)의
     * 결과를 락 이전 스냅샷이나 비잠금 재조회({@link MemberAddressDao#selectNextDefaultCandidate})가
     * 보지 못해 "기본 1개" 불변식이 깨질 수 있었다(계획 위반이기도 했다).
     */
    @Transactional
    public void delete(String memberId, Long addressId) {
        MemberAddress owned = dao.selectOwned(memberId, addressId);
        if (owned == null) {
            throw notFound();
        }

        // 락, 두 번째 문장(register/setDefault와 동일 지점) — 이 목록(잠금 읽기 = 최신 커밋본)을
        // 그대로 써서 아래 승계 판정을 수행한다(반환값을 버리지 않는다).
        List<MemberAddress> locked = dao.selectByMemberIdForUpdate(memberId);

        boolean wasDefault = locked.stream()
                .filter(a -> addressId.equals(a.getAddressId()))
                .map(MemberAddress::getIsDefault)
                .anyMatch("Y"::equals);

        LocalDateTime now = LocalDateTime.now(clock);
        dao.softDelete(memberId, addressId, now);

        if (wasDefault) {
            locked.stream()
                    .filter(a -> !addressId.equals(a.getAddressId()))
                    .max(Comparator
                            .comparing(MemberAddress::getLastUsedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(MemberAddress::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .ifPresent(candidate -> dao.setDefault(memberId, candidate.getAddressId()));
        }
    }

    /** 기본설정 — 이미 기본이면 no-op 성공(멱등). */
    @Transactional
    public MemberAddress setDefault(String memberId, Long addressId) {
        MemberAddress owned = dao.selectOwned(memberId, addressId);
        if (owned == null) {
            throw notFound();
        }
        if ("Y".equals(owned.getIsDefault())) {
            return owned;
        }

        dao.selectByMemberIdForUpdate(memberId); // 락
        dao.clearDefaultForMember(memberId);
        dao.setDefault(memberId, addressId);
        return dao.selectOwned(memberId, addressId);
    }

    /**
     * 요청 검증(400 MBR-4200, DB 접근 전) — 위반 필드를 message에 명시(STORY "사람 수정" (3)).
     * phoneNorm은 호출부가 이미 {@link MemberRegistrationService#normalizePhone}으로 계산해
     * 넘긴다(중복 계산 방지 — insert/update 양쪽에서 그대로 재사용).
     */
    private static void validateFields(AddressInput input, String phoneNorm) {
        requireValidRecipient(input.recipient());
        requireValidPhone(input.phone(), phoneNorm);
        requireValidZipcode(input.zipcode());
        requireValidAddressField("roadAddress", input.roadAddress());
        requireValidAddressField("detailAddress", input.detailAddress());
        requireValidOptionalField("entranceMethod", input.entranceMethod());
        requireValidOptionalField("deliveryMemo", input.deliveryMemo());
    }

    private static void requireValidRecipient(String recipient) {
        if (recipient == null || recipient.trim().isEmpty() || recipient.trim().length() > RECIPIENT_MAX_LENGTH) {
            throw validationError("recipient", "받는사람 이름은 1~" + RECIPIENT_MAX_LENGTH + "자여야 합니다");
        }
    }

    private static void requireValidPhone(String phone, String phoneNorm) {
        if (phone == null || phone.isBlank()) {
            throw validationError("phone", "휴대폰번호를 입력하세요");
        }
        // 재작업 지시(3) — 정규화 전 원문 길이(1~20) 검증. requireValidPhone 진입 시점에 이미
        // isBlank가 아님을 확인했으므로 하한(1)은 항상 충족 — 여기서는 상한만 본다.
        if (phone.length() > PHONE_RAW_MAX_LENGTH) {
            throw validationError("phone", "휴대폰번호는 최대 " + PHONE_RAW_MAX_LENGTH + "자까지 입력할 수 있습니다");
        }
        if (phoneNorm == null || !MemberRegistrationService.PHONE_PATTERN.matcher(phoneNorm).matches()) {
            throw validationError("phone", "휴대폰번호 형식이 올바르지 않습니다");
        }
    }

    private static void requireValidZipcode(String zipcode) {
        if (zipcode == null || !ZIPCODE_PATTERN.matcher(zipcode).matches()) {
            throw validationError("zipcode", "우편번호는 숫자 5자리여야 합니다");
        }
    }

    private static void requireValidAddressField(String field, String value) {
        if (value == null || value.isBlank() || value.length() > ADDRESS_MAX_LENGTH) {
            throw validationError(field, "1~" + ADDRESS_MAX_LENGTH + "자여야 합니다");
        }
    }

    private static void requireValidOptionalField(String field, String value) {
        if (value != null && value.length() > OPTIONAL_FIELD_MAX_LENGTH) {
            throw validationError(field, "최대 " + OPTIONAL_FIELD_MAX_LENGTH + "자까지 입력할 수 있습니다");
        }
    }

    private static MemberAddressApiException validationError(String field, String message) {
        return new MemberAddressApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static MemberAddressApiException notFound() {
        return new MemberAddressApiException(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, MESSAGE_NOT_FOUND);
    }

    private static MemberAddressApiException limitExceeded() {
        return new MemberAddressApiException(HttpStatus.CONFLICT, CODE_LIMIT_EXCEEDED,
                "배송지는 최대 " + MAX_ADDRESSES + "개까지 등록할 수 있습니다");
    }

    /**
     * 이 서비스 전용 요청 전송 record(클래스 javadoc "Dev 기록" 참고) — 컨트롤러의
     * {@code MemberAddressController.AddressRequest}를 그대로 받지 않는다(역방향 의존 회피).
     * {@code isDefault}는 등록 요청에서만 의미가 있다(선택, null이면 false 취급).
     */
    public record AddressInput(String recipient, String phone, String zipcode, String roadAddress,
                                String detailAddress, String entranceMethod, String deliveryMemo,
                                Boolean isDefault) { }
}
