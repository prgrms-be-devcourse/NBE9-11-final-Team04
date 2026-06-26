interface TrustScoreGaugeProps {
  score: number
  size?: number
  showLabel?: boolean
}

export function TrustScoreGauge({ score, size = 120, showLabel = true }: TrustScoreGaugeProps) {
  const radius = (size - 12) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (score / 100) * circumference

  const getColor = () => {
    if (score >= 80) return '#059669'
    if (score >= 60) return '#2563eb'
    if (score >= 40) return '#d97706'
    return '#dc2626'
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="#e2e8f0" strokeWidth={8} />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={getColor()}
            strokeWidth={8}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            className="transition-all duration-700"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-bold text-slate-900">{score}</span>
          <span className="text-xs text-slate-500">/ 100</span>
        </div>
      </div>
      {showLabel && (
        <div className="text-center">
          <p className="text-sm font-medium text-slate-700">신뢰도 점수</p>
          {score >= 80 && (
            <span className="mt-1 inline-block rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
              CERTIFIED
            </span>
          )}
        </div>
      )}
    </div>
  )
}
