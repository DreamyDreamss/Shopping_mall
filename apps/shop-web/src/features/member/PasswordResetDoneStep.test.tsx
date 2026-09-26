/** @jest-environment jsdom */
// QUICK-20260926-1.1 사람 수정 — 완료 단계 렌더 시 "로그인하러 가기" 링크가 #/login을 가리키는지 단언.
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { describe, expect, test } from '@jest/globals'
import { PasswordResetDoneStep } from './PasswordResetDoneStep'

describe('PasswordResetDoneStep', () => {
  test('"로그인하러 가기" 링크가 #/login을 가리킨다', () => {
    render(<PasswordResetDoneStep />)
    const link = screen.getByRole('link', { name: '로그인하러 가기' })
    expect(link).toHaveAttribute('href', '#/login')
  })
})
