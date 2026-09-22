---
uis-id: UIS-MBR-005
name: member_signup
domain: member
domain-code: MBR
layer: ui
route: /member/signup
screens_role: 서버렌더
api_hints: []
access_control: []
anchors:
  - "modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupViewController.java:44"
revision_history:
  - "2026-09-13 골격 생성(spec_resync_check, zero-LLM)"
---

# UIS-MBR-005: member_signup

> 코드(라우트) 기준 재동기화 골격(zero-LLM) — 화면 구성·상태·검증 규칙은 보강 대상

## 1. 화면 개요

- 라우트: `/member/signup` (server-view, `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupViewController.java:44`)
- 목적: [TBD]

## 2. 화면 구성

[TBD]

## 3. 입력·검증 규칙

[TBD]

## 4. 호출 API

[TBD]

## 5. 표시 조건(상태)

[TBD] — 스토리북 스토리가 있으면 `[이름](story:ID)`로 연결

> 보강: `/sl-sync --apply --kind=uis` 또는 ddd-ui-agent
