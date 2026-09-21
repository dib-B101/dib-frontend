import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  // ALB 한 대를 백엔드와 나눠 쓴다. /admin 아래에서 서비스되므로 에셋 경로도 /admin/ 으로 나가야 한다
  base: '/admin/',
  plugins: [react()],
  server: {
    port: 5173,
  },
})
