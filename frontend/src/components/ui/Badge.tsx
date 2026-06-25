interface BadgeProps {
  children: React.ReactNode
  variant?: 'blue' | 'green' | 'orange' | 'red' | 'gray' | 'brand'
  className?: string
}

const variantStyles: Record<string, React.CSSProperties> = {
  blue:   { background: 'var(--brand-tint)', color: 'var(--brand-deeper)' },
  green:  { background: '#e8f8ef', color: '#1a7a3f' },
  orange: { background: '#fff4e8', color: '#a05c00' },
  red:    { background: '#ffeaea', color: '#c0392b' },
  gray:   { background: 'var(--bg-alt)', color: 'var(--fg-muted)' },
  brand:  { background: 'var(--brand)', color: '#ffffff' },
}

export function Badge({ children, variant = 'blue', className }: BadgeProps) {
  return (
    <span
      className={className}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        fontSize: '12px',
        fontWeight: 700,
        padding: '3px 8px',
        borderRadius: 'var(--radius-lg)',
        ...variantStyles[variant],
      }}
    >
      {children}
    </span>
  )
}
