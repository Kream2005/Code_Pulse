import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const spring = {
  target: 'http://localhost:8080',
  changeOrigin: true,
  secure: false,
}

const search = {
  target: 'http://localhost:8090',
  changeOrigin: true,
  secure: false,
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 4200,
    strictPort: true,
    proxy: {
      '/auth': spring,
      '/feedbacks': spring,
      '/notifications': spring,
      '/coding-challenges': spring,
      '/utilisateurs': spring,
      '/demandes-reinit': spring,
      '/integration-logs': spring,
      '/analytics': spring,
      '/questions-feedback': spring,
      '/reponses-feedback': spring,
      '/dev': spring,
      '/api': spring,
      // codepulse-search (semantic search / KPI / assistant)
      '/search': search,
      '/kpi': search,
      '/assistant': search,
      '/knowledge': search,
      '/ingestion': search,
      '/health': search,
    },
  },
})
