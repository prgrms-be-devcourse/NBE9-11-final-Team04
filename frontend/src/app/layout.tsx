import type { Metadata } from 'next'
import './globals.css'
import { Providers } from '@/components/Providers'
import { AppShell } from '@/components/layout/AppShell'

export const metadata: Metadata = {
  title: 'SeedLink - 신뢰 기반 크라우드펀딩',
  description: '검증과 자금 투명성 위에서 아이디어를 실현하는 크라우드펀딩 플랫폼',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link
          rel="stylesheet"
          crossOrigin="anonymous"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css"
        />
      </head>
      <body>
        <Providers>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  )
}
