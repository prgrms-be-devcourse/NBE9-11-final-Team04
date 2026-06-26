import { cn } from '@/utils/cn'

interface ProgressBarProps {
  value: number
  max?: number
  showLabel?: boolean
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export function ProgressBar({
  value,
  max = 100,
  showLabel = true,
  size = 'md',
  className,
}: ProgressBarProps) {
  const actualPercent = Math.round((value / max) * 100)
  const barWidth = Math.min(100, actualPercent)
  const heights = { sm: 'h-1.5', md: 'h-2.5', lg: 'h-4' }

  return (
    <div className={cn('w-full', className)}>
      {showLabel && (
        <div className="mb-1.5 flex justify-between text-sm">
          <span className="font-semibold text-primary-600">{actualPercent}%</span>
          <span className="text-slate-500">달성률</span>
        </div>
      )}
      <div className={cn('w-full overflow-hidden rounded-full bg-slate-200', heights[size])}>
        <div
          className={cn('rounded-full bg-gradient-to-r from-primary-500 to-accent-500 transition-all duration-500', heights[size])}
          style={{ width: `${barWidth}%` }}
        />
      </div>
    </div>
  )
}
