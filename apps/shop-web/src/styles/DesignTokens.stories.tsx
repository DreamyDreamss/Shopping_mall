// SR-309 — 디자인 토큰 문서 페이지. 값을 이 파일에 하드코딩하지 않고 실제 적용된 CSS 커스텀
// 프로퍼티 값을 getComputedStyle로 읽어 그린다(값의 정본은 tokens.css 하나뿐).
// 이 파일 자체가 *.stories.tsx라 story-per-component pair 규칙(exclude 대상) 의무가 없다 —
// 별도 짝 컴포넌트 .tsx를 새로 만들지 않는다.
import type { ReactNode } from 'react'
import type { Meta, StoryObj } from '@storybook/react-vite'
import { contrastRatio, meetsAA } from './contrast'
import {
  colorTokens,
  gradientTokens,
  typographyTokens,
  fontWeightTokens,
  spaceTokens,
  radiusTokens,
  shadowTokens,
  zIndexTokens,
  layoutTokens,
  type TokenCatalogEntry,
} from './tokenCatalog'

function tokenValue(cssVar: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(cssVar).trim()
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section style={{ marginBottom: 32 }}>
      <h2 style={{ fontSize: 18, marginBottom: 12 }}>{title}</h2>
      {children}
    </section>
  )
}

/** 색·그라데이션 견본 — 두 카탈로그 모두 CSS 값을 `var()`로 그대로 렌더한다. */
function TokenSwatch({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '6px 0' }}>
      <span
        style={{ width: 48, height: 32, borderRadius: 6, border: '1px solid #ddd', flexShrink: 0, background: `var(${entry.cssVar})` }}
      />
      <div>
        <div>
          <code>{entry.cssVar}</code> — {entry.label}
        </div>
        <div style={{ color: '#767676', fontSize: 13 }}>
          {value} · {entry.usage}
        </div>
      </div>
    </div>
  )
}

function TypeSample({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ marginBottom: 10 }}>
      <div style={{ fontSize: value, fontFamily: "'Pretendard', sans-serif" }}>{entry.label} — 가나다라 Pretendard 123</div>
      <div style={{ color: '#767676', fontSize: 12 }}>
        <code>{entry.cssVar}</code> = {value} · {entry.usage}
      </div>
    </div>
  )
}

function WeightSample({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ fontSize: 18, fontFamily: "'Pretendard', sans-serif", fontWeight: Number(value), marginBottom: 6 }}>
      {entry.label}({value}) — 가나다라 Pretendard
    </div>
  )
}

function SpaceSample({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
      <span style={{ width: value, height: 16, background: 'var(--color-primary)', display: 'inline-block' }} />
      <span>
        <code>{entry.cssVar}</code> = {value} · {entry.usage}
      </span>
    </div>
  )
}

function RadiusSample({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
      <span style={{ width: 48, height: 48, background: 'var(--color-primary-bg)', border: '1px solid var(--color-primary)', borderRadius: value }} />
      <span>
        <code>{entry.cssVar}</code> = {value} · {entry.usage}
      </span>
    </div>
  )
}

function ShadowSample({ entry }: { entry: TokenCatalogEntry }) {
  const value = tokenValue(entry.cssVar)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 12 }}>
      <span style={{ width: 64, height: 40, background: '#fff', boxShadow: value }} />
      <span>
        <code>{entry.cssVar}</code> = {value} · {entry.usage}
      </span>
    </div>
  )
}

// 대비표 배경 목록 — CSS 커스텀 프로퍼티는 `--`로 시작하는 문자열로, 그 외(예: '#ffffff')는
// 토큰으로 선언돼 있지 않은 리터럴 색상으로 취급한다(resolveBackground 참고).
// 흰 배경(#ffffff)은 상품 카드·본문의 실제 기본 배경이라 round 2 QA에서 대비 계산 기준으로 인용
// 됐음에도 표에 없어 문단-표 불일치가 났다(round 3 재작업 지시 1) — 표에도 추가해 일치시킨다.
const CONTRAST_BACKGROUNDS = ['#ffffff', '--color-surface-3', '--color-surface-2', '--color-surface-1', '--color-primary-bg']

/** 배경 식별자를 라벨(사람이 읽을 이름+실제 hex)과 계산에 쓸 hex 값으로 푼다.
 * `--`로 시작하면 CSS 커스텀 프로퍼티(tokens.css가 정본), 아니면 토큰화되지 않은 리터럴 색상. */
function resolveBackground(bg: string): { label: string; value: string } {
  if (bg.startsWith('--')) {
    const value = tokenValue(bg)
    return { label: `${bg}(${value})`, value }
  }
  return { label: `흰 배경(${bg})`, value: bg }
}

// tokenCatalog.colorTokens 중 "텍스트 용도"로 선언된 토큰 전부(배경·딤 계열 제외) — 표에서
// 하나라도 빠지면 미달 조합을 놓칠 수 있다(QA round1 권고 2). --color-price·--color-brand와
// 상태색 4종(성공/경고/오류/정보)을 포함한다.
const CONTRAST_TEXTS = [
  '--color-text',
  '--color-text-secondary',
  '--color-text-tertiary',
  '--color-link',
  '--color-primary',
  '--color-price',
  '--color-brand',
  '--color-success',
  '--color-warning',
  '--color-error',
  '--color-info',
]

type ContrastLevel = '통과' | '큰 글씨 전용' | '본문 사용 금지' | '계산 불가'

/** 일반 텍스트 AA(4.5:1)를 충족하면 '통과', 못 미치더라도 큰 글씨 AA(3:1, 24px 이상 또는
 * 18.66px 이상 굵게)를 충족하면 '큰 글씨 전용', 둘 다 못 미치면 '본문 사용 금지'로 판정한다. */
function contrastLevel(ratio: number): ContrastLevel {
  if (meetsAA(ratio)) return '통과'
  if (meetsAA(ratio, true)) return '큰 글씨 전용'
  return '본문 사용 금지'
}

const LEVEL_COLOR: Record<ContrastLevel, string> = {
  통과: '#136b2f',
  '큰 글씨 전용': '#8a5a00',
  '본문 사용 금지': '#b42318',
  '계산 불가': '#767676',
}

/** 배경·본문 hex 한 쌍의 대비를 계산한다. 토큰 값이 유효한 hex가 아니면(오타 토큰·rgba 계열·
 * tokens.css 미로드로 빈 문자열 등) `contrastRatio`가 예외를 던지는데, 그 조합 하나만
 * "계산 불가"로 표시하고 나머지 표·문단은 계속 그린다(round 3 QA 권고 3 — 행 단위 격리, 토큰
 * 하나의 계산 오류가 Design Tokens 스토리 전체를 무너뜨리지 않게 한다). */
function evaluateContrast(bgValue: string, textValue: string): { ratio: number | null; level: ContrastLevel } {
  try {
    const ratio = contrastRatio(bgValue, textValue)
    return { ratio, level: contrastLevel(ratio) }
  } catch {
    return { ratio: null, level: '계산 불가' }
  }
}

type ContrastRow = {
  bgLabel: string
  bgValue: string
  textVar: string
  textValue: string
  ratio: number | null
  level: ContrastLevel
}

function buildContrastRows(): ContrastRow[] {
  return CONTRAST_BACKGROUNDS.flatMap(bg => {
    const { label: bgLabel, value: bgValue } = resolveBackground(bg)
    return CONTRAST_TEXTS.map(textVar => {
      const textValue = tokenValue(textVar)
      const { ratio, level } = evaluateContrast(bgValue, textValue)
      return { bgLabel, bgValue, textVar, textValue, ratio, level }
    })
  })
}

/** 특정 텍스트 토큰(가격/브랜드 등)이 어느 배경 위에서 쓸 수 있는지를, 하드코딩한 수치가 아니라
 * 이 표와 똑같은 계산(`evaluateContrast`)으로 매 렌더마다 도출해 문장으로 만든다. 이렇게 하면
 * 문단과 표가 서로 다른 값을 말하는 일이 구조적으로 불가능하다(round 2 QA 지적 — 흰 배경 실측을
 * 인용한 문단이 흰 배경 없는 표와 모순됐던 문제의 재발 방지, round 3 재작업 지시 1). */
function describeUsage(cssVar: string): string {
  const textValue = tokenValue(cssVar)
  const perBackground = CONTRAST_BACKGROUNDS.map(bg => {
    const { label, value: bgValue } = resolveBackground(bg)
    return { label, level: evaluateContrast(bgValue, textValue).level }
  })
  const usable = perBackground.filter(b => b.level === '통과' || b.level === '큰 글씨 전용').map(b => b.label)
  // '본문 사용 금지'는 실측 대비율이 3:1에 못 미친 것이고, '계산 불가'는 대비율 자체를 못 구한 것이다
  // (토큰 값이 유효한 색상이 아님) — round 3 QA 권고 2: 이 둘을 한 문구로 합치면 측정한 적 없는
  // "3:1 미달"을 단정하게 된다. 사유별로 문장을 따로 만든다.
  const rejected = perBackground.filter(b => b.level === '본문 사용 금지').map(b => b.label)
  const uncalculable = perBackground.filter(b => b.level === '계산 불가').map(b => b.label)

  const usablePart =
    usable.length > 0
      ? `${usable.join('·')} 위의 굵은 큰 글씨(18.66px 이상 굵게, 또는 24px 이상)에만 쓴다.`
      : '이 표의 배경 어디에서도 큰 글씨 기준(3:1)을 충족하지 않아 텍스트로 쓰지 않는다.'
  const rejectedPart =
    rejected.length > 0
      ? ` ${rejected.join('·')} 위에서는 큰 글씨도 3:1 미달이므로 쓰지 않는다(그 위의 텍스트는 본문색 ${tokenValue('--color-text')}을 쓰고, 강조는 별도 배지로 대신한다).`
      : ''
  const uncalculablePart =
    uncalculable.length > 0
      ? ` ${uncalculable.join('·')}는 계산 불가 — 토큰 값 확인(유효한 색상 값이 아니어서 이 조합의 대비를 측정하지 못했다. 미달을 단정하지 않는다).`
      : ''
  return usablePart + rejectedPart + uncalculablePart
}

/** 표와 같은 계산으로 사용 규칙 문단을 만드는 대상 — 벤치마크 실측상 AA 본문 기준(4.5:1)에
 * 못 미쳐 사용 제약이 필요한 토큰 2종(round 1·2 QA가 지적한 `--color-price`·`--color-brand`). */
const SPECIAL_TEXT_RULES: TokenCatalogEntry[] = colorTokens.filter(t => t.cssVar === '--color-price' || t.cssVar === '--color-brand')

/** 배경×본문 명도 대비 표 — WCAG AA(4.5:1) 미달 조합을 "본문 사용 금지"로, 큰 글씨 기준(3:1)만
 * 충족하는 조합은 "큰 글씨 전용"으로, 계산 자체가 실패한 조합은 "계산 불가"로 구분 표시한다
 * (행 단위 격리 — 한 행의 오류가 표 전체 렌더를 막지 않는다). */
function ContrastTable() {
  const rows = buildContrastRows()
  return (
    <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: 13 }}>
      <thead>
        <tr>
          <th style={{ textAlign: 'left', padding: 6 }}>배경</th>
          <th style={{ textAlign: 'left', padding: 6 }}>본문색</th>
          <th style={{ textAlign: 'left', padding: 6 }}>대비율</th>
          <th style={{ textAlign: 'left', padding: 6 }}>판정(AA 4.5:1 / 큰 글씨 3:1)</th>
        </tr>
      </thead>
      <tbody>
        {rows.map(r => (
          <tr key={`${r.bgLabel}__${r.textVar}`} style={{ background: r.bgValue, color: r.textValue }}>
            <td style={{ padding: 6 }}>{r.bgLabel}</td>
            <td style={{ padding: 6 }}>
              {r.textVar} ({r.textValue})
            </td>
            <td style={{ padding: 6 }}>{r.ratio !== null ? `${r.ratio.toFixed(2)}:1` : '—'}</td>
            <td style={{ padding: 6, fontWeight: 700, color: LEVEL_COLOR[r.level] }}>{r.level}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function DesignTokensDoc() {
  return (
    <div style={{ maxWidth: 900 }}>
      <Section title="색">
        {colorTokens.map(entry => (
          <TokenSwatch key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="그라데이션">
        {gradientTokens.map(entry => (
          <TokenSwatch key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="배경×본문 명도 대비 표">
        <p style={{ fontSize: 13, color: '#767676', marginBottom: 12 }}>
          판정은 4단계다 — <b>통과</b>(일반 본문 AA 4.5:1 이상), <b>큰 글씨 전용</b>(일반 본문 기준은 못 미치지만 WCAG 큰 글씨
          기준인 3:1 이상 — 18.66px 이상 굵게, 또는 24px 이상 일반 굵기로만 사용), <b>본문 사용 금지</b>(3:1도 못 미침),{' '}
          <b>계산 불가</b>(토큰 값이 유효한 색상이 아니어서 그 조합만 계산을 건너뜀 — 나머지 표는 그대로 렌더됨).
          <br />
          아래 문장은 이 표와 같은 계산(<code>contrast.ts</code>)으로 매 렌더마다 도출한다 — 표와 문장이 서로 다른 값을
          말할 수 없다.
          {SPECIAL_TEXT_RULES.map(rule => (
            <span key={rule.cssVar}>
              <br />
              <b>
                <code>{rule.cssVar}</code>({rule.label})
              </b>
              는 {describeUsage(rule.cssVar)}
            </span>
          ))}
        </p>
        <ContrastTable />
      </Section>
      <Section title="타이포 — 크기 스케일">
        {typographyTokens.map(entry => (
          <TypeSample key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="타이포 — 굵기">
        {fontWeightTokens.map(entry => (
          <WeightSample key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="간격(4px 배수)">
        {spaceTokens.map(entry => (
          <SpaceSample key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="radius">
        {radiusTokens.map(entry => (
          <RadiusSample key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="그림자">
        {shadowTokens.map(entry => (
          <ShadowSample key={entry.cssVar} entry={entry} />
        ))}
      </Section>
      <Section title="z-index 층">
        <ul>
          {zIndexTokens.map(entry => (
            <li key={entry.cssVar}>
              <code>{entry.cssVar}</code> = {tokenValue(entry.cssVar)} · {entry.usage}
            </li>
          ))}
        </ul>
      </Section>
      <Section title="레이아웃">
        <ul>
          {layoutTokens.map(entry => (
            <li key={entry.cssVar}>
              <code>{entry.cssVar}</code> = {tokenValue(entry.cssVar)} · {entry.usage}
            </li>
          ))}
        </ul>
      </Section>
    </div>
  )
}

const meta = {
  title: 'Design Tokens',
  tags: ['UIS-CMN-001'],
} satisfies Meta

export default meta

type Story = StoryObj<typeof meta>

export const Tokens: Story = { render: () => <DesignTokensDoc /> }
