// SR-310 — resolveSwipeTab 좌/우 스와이프·임계값 미만 무변화 단위테스트.
import { describe, expect, test } from '@jest/globals'
import { resolveSwipeTab } from './resolveSwipeTab'

describe('resolveSwipeTab', () => {
  test('왼쪽으로 임계값 이상 밀면 다음 탭 인덱스를 반환한다', () => {
    expect(resolveSwipeTab(200, 100, 40, 0, 3)).toBe(1)
  })

  test('오른쪽으로 임계값 이상 밀면 이전 탭 인덱스를 반환한다', () => {
    expect(resolveSwipeTab(100, 200, 40, 1, 3)).toBe(0)
  })

  test('임계값 미만 이동은 현재 인덱스를 그대로 반환한다', () => {
    expect(resolveSwipeTab(100, 110, 40, 1, 3)).toBe(1)
    expect(resolveSwipeTab(110, 100, 40, 1, 3)).toBe(1)
  })

  test('마지막 탭에서 왼쪽으로 밀어도 범위를 벗어나지 않는다', () => {
    expect(resolveSwipeTab(200, 100, 40, 2, 3)).toBe(2)
  })

  test('첫 탭에서 오른쪽으로 밀어도 범위를 벗어나지 않는다', () => {
    expect(resolveSwipeTab(100, 200, 40, 0, 3)).toBe(0)
  })
})
