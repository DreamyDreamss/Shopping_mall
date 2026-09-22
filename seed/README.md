# seed/ — 시연 초기 데이터

setup 스크립트가 이 디렉토리를 워크스페이스로 복사한다. 직접 수정하지 말고,
워크스페이스에서 시연·개발을 진행한 뒤 갱신본을 여기로 되올린다.

| 경로 | 워크스페이스에서의 위치 | 내용 |
|---|---|---|
| `변경관리/` | `docs/변경관리/` | SR 원장(`_sr_ledger.jsonl` 2,796줄) · 요구사항 · 문답 — SR 디렉토리 150개 |
| `09_납품/` | `docs/09_납품/` | 납품 묶음 21건 |
| `_lab/catalog/` | `_lab/catalog/` | 설계서 139건 |
| `.speclinker/` | `.speclinker/` | 스펙 인덱스 · 승인 · 기준선 · 프로파일 (파일 206개) |

## 담지 않는 것

파생물은 읽는 시점에 재생성되므로 제외한다.

- `.speclinker/job_logs` — AIDD 잡 로그
- `.speclinker/model.sqlite` — 모델 색인 (정본에서 재계산)
- `.speclinker/agentchat.db` — 대화 기록
- `.speclinker/story_shots/{current,diff}` — 스토리북 캡처 비교본
- `.speclinker/backup` · `*.tmp`

## DB와의 관계

이 디렉토리는 **speclinker 산출물**(스펙·SR·납품물)이고, 애플리케이션 데이터는
`db/dump.sql`(16테이블 · 124행)에 따로 있다. 둘은 같은 시점(2026-09-22)의 짝이다.
한쪽만 갈아끼우면 스펙이 가리키는 데이터와 실제 DB가 어긋난다.
