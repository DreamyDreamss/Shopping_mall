// SR-310 — pushToast 1개/3개/4번째 삽입 시 가장 오래된 것 제거 단위테스트.
import { describe, expect, test } from '@jest/globals'
import { capToasts, pushToast, type ToastItem } from './toastQueue'

function toast(id: string): ToastItem {
  return { id, message: `메시지-${id}` }
}

describe('pushToast', () => {
  test('빈 배열에 1개 추가하면 그 1개만 남는다', () => {
    expect(pushToast([], toast('1'))).toEqual([toast('1')])
  })

  test('3개까지는 전부 유지한다(기본 max=3)', () => {
    const result = pushToast([toast('1'), toast('2')], toast('3'))
    expect(result).toEqual([toast('1'), toast('2'), toast('3')])
  })

  test('4번째 삽입은 가장 오래된(첫) 항목을 제거한다', () => {
    const result = pushToast([toast('1'), toast('2'), toast('3')], toast('4'))
    expect(result).toEqual([toast('2'), toast('3'), toast('4')])
  })

  test('max를 다르게 지정하면 그 상한을 따른다', () => {
    expect(pushToast([toast('1')], toast('2'), 1)).toEqual([toast('2')])
  })
})

describe('capToasts', () => {
  test('max 이하이면 그대로 유지한다(기본 max=3)', () => {
    const toasts = [toast('1'), toast('2')]
    expect(capToasts(toasts)).toEqual(toasts)
  })

  test('max를 초과하면 가장 오래된(앞쪽) 것부터 잘라 최신 max개만 남긴다', () => {
    const result = capToasts([toast('1'), toast('2'), toast('3'), toast('4'), toast('5')])
    expect(result).toEqual([toast('3'), toast('4'), toast('5')])
  })

  test('max를 다르게 지정하면 그 상한을 따른다', () => {
    expect(capToasts([toast('1'), toast('2'), toast('3')], 1)).toEqual([toast('3')])
  })

  test('max<=0이면 아무것도 남기지 않는다', () => {
    expect(capToasts([toast('1')], 0)).toEqual([])
  })
})
