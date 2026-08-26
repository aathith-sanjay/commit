import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  // VITE_BASE_PATH is set to '/commit/' in CI for GitHub Pages deployment.
  // Defaults to '/' for local development.
  base: process.env.VITE_BASE_PATH ?? '/',
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
