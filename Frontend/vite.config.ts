import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Enquanto a API do mapa (#15) não existe, o Frontend lê os cenários direto de Backend/data.
const scenariosDir = fileURLToPath(new URL("../Backend/data/scenarios", import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@scenarios": scenariosDir },
  },
  server: {
    fs: { allow: [".", scenariosDir] },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/testSetup.ts",
    pool: "threads",
    fileParallelism: false,
  },
});
