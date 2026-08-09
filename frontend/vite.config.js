import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: "globalThis",
  },
  server: {
    // Listen on LAN so phones/other PCs on the same Wi‑Fi can open the app.
    host: true,
    port: 5173,
  },
});
