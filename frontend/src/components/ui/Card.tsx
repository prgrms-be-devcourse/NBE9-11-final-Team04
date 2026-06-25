interface CardProps {
  children: React.ReactNode
  className?: string
  hover?: boolean
  onClick?: () => void
  style?: React.CSSProperties
}

export function Card({ children, className, hover, onClick, style }: CardProps) {
  return (
    <div
      onClick={onClick}
      className={className}
      style={{
        background: '#ffffff',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        transition: 'box-shadow 0.2s, border-color 0.2s',
        cursor: hover || onClick ? 'pointer' : undefined,
        ...style,
      }}
      onMouseEnter={hover || onClick ? e => {
        (e.currentTarget as HTMLDivElement).style.boxShadow = '0 4px 16px rgba(0,176,225,0.1)'
        ;(e.currentTarget as HTMLDivElement).style.borderColor = 'rgba(0,176,225,0.3)'
      } : undefined}
      onMouseLeave={hover || onClick ? e => {
        (e.currentTarget as HTMLDivElement).style.boxShadow = ''
        ;(e.currentTarget as HTMLDivElement).style.borderColor = 'var(--border)'
      } : undefined}
    >
      {children}
    </div>
  )
}
