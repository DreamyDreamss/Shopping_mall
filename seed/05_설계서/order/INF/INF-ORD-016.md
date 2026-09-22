---
inf-id: INF-ORD-016
name: api zipcodes
layer: api
method: GET
path: /api/zipcodes
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables: []
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/ZipcodeController.java:38
---

# INF-ORD-016: GET /api/zipcodes

> 코드(핸들러) 기준 재동기화 골격(zero-LLM) — 요청·응답·오류 계약은 보강 대상

## 개요

[TBD]

## 요청

[TBD]

## 응답

[TBD]

## 오류

[TBD]

> 보강: `/sl-sync --apply --kind=inf` 또는 ddd-api-agent

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-235 | #3 | GET /api/zipcodes 우편번호(도로명) 검색 API 구현 — 200 {items:[{zipcode,roadAddress,sido,sigungu}]}(0건 포함), 400 MBR-4202(검색어 정규화 후 2~50자 범위 밖), 숫자 검색어는 zipcode 전방일치도 포함(OR) | shop-api@1c82e8c, shop-web@8e1e3e3 |
