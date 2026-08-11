import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Two backends, so two proxy targets.
//   order-service  :8080  the REST API, running from Class 1
//   support-agent  :8081  the chat endpoint, which exists from Class 7
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/orders': { target: 'http://localhost:8080', changeOrigin: true },
      '/api/chat': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/refund-email': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/events': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/confirmations': { target: 'http://localhost:8081', changeOrigin: true },
    },
  },
})
