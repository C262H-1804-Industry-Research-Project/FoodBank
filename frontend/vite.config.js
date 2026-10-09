import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.API_PROXY_TARGET || 'http://localhost:8080'
  const proxy = {
    '/api': { target: apiTarget, changeOrigin: true },
    '/uploads': { target: apiTarget, changeOrigin: true },
  }
  return {
    plugins: [vue()],
    server: { proxy },
    preview: { proxy },
    test: {
      environment: 'jsdom',
      include: ['src/**/*.test.js'],
      // Ignore any developer token in .env.local so tests never call Mapbox; tests opt in with vi.stubEnv
      env: { VITE_MAPBOX_TOKEN: '' },
      restoreMocks: true,
      unstubEnvs: true,
      unstubGlobals: true,
    },
  }
})
