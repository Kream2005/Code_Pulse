import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const api = {
  target: 'http://localhost:8080',
  changeOrigin: true,
  secure: false,
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 4200,
    strictPort: true,
    proxy: {
      '/auth': api,
      '/feedbacks': api,
      '/notifications': api,
      '/coding-challenges': api,
      '/utilisateurs': api,
      '/demandes-reinit': api,
      '/integration-logs': api,
      '/analytics': api,
      '/questions-feedback': api,
      '/reponses-feedback': api,
      '/api': api,
    },
  },
})
