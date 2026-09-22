---
uis-id: UIS-ORD-007
name: orders_상세
domain: order
domain-code: ORD
layer: ui
route: /orders/{*}
screens_role: 주화면
api_hints: []
access_control: []
anchors:
  - "modules/shop-web/src/App.tsx:50"
revision_history:
  - "2026-09-13 골격 생성(spec_resync_check, zero-LLM)"
---

# UIS-ORD-007: orders_상세

> 코드(라우트) 기준 재동기화 골격(zero-LLM) — 화면 구성·상태·검증 규칙은 보강 대상

## 1. 화면 개요

- 라우트: `/orders/{*}` (spa-route, `modules/shop-web/src/App.tsx:50`)
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
