export function LoadingSpinner({ className = 'h-8 w-8' }: { className?: string }) {
  return (
    <div className="flex items-center justify-center py-12">
      <div className={`animate-spin rounded-full border-4 border-primary-200 border-t-primary-600 ${className}`} />
    </div>
  )
}
