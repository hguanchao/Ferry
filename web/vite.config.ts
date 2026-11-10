import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      strictPort: false,
      proxy: {
        // /api 转发到网关，网关按 /ferry-auth、/ferry-system 前缀 StripPrefix 后路由到具体服务
        '/api': {
          target: env.VITE_GATEWAY || 'http://127.0.0.1:9100',
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/api/, ''),
        },
      },
    },
    build: {
      target: 'es2022',
      chunkSizeWarningLimit: 1200,
    },
  }
})
