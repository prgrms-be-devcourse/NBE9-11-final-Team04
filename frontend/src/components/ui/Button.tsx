'use client'

import { cn } from '@/utils/cn'
import { type ButtonHTMLAttributes, forwardRef } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'outline' | 'ghost' | 'dark' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
}

const variantStyles: Record<string, React.CSSProperties> = {
  primary: { background: 'var(--brand)', color: '#fff' },
  outline: { border: '1.5px solid var(--brand)', color: 'var(--brand-dark)', background: 'transparent' },
  ghost: { border: '1px solid var(--border)', color: 'var(--fg-2)', background: 'transparent' },
  dark: { background: 'var(--fg)', color: '#fff' },
  danger: { background: '#f65c5c', color: '#fff' },
}

const sizeStyles: Record<string, React.CSSProperties> = {
  sm: { padding: '6px 14px', fontSize: '13px' },
  md: { padding: '10px 20px', fontSize: '15px' },
  lg: { padding: '14px 32px', fontSize: '17px', fontWeight: 700 },
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, disabled, children, style, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
        fontWeight: 500,
        borderRadius: 'var(--radius-sm)',
        border: 'none',
        cursor: disabled || loading ? 'not-allowed' : 'pointer',
        opacity: disabled || loading ? 0.5 : 1,
        transition: 'all 0.2s',
        fontFamily: 'inherit',
        ...variantStyles[variant],
        ...sizeStyles[size],
        ...style,
      }}
      {...props}
    >
      {loading && (
        <span style={{ width: '14px', height: '14px', borderRadius: '50%', border: '2px solid currentColor', borderTopColor: 'transparent', animation: 'spin 0.8s linear infinite', display: 'inline-block' }} />
      )}
      {children}
    </button>
  ),
)
Button.displayName = 'Button'
