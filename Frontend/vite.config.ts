import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Os testes usam os cenários reais de Backend/data como fixture; a aplicação busca o mapa na API.
const scenariosDir = fileURLToPath(new URL("../Backend/data/scenarios", import.meta.url));

export default defineConfig({
  plugins: [react()],
  server: {
    // Necessário para o Vitest ler as fixtures fora da pasta do Frontend.
    fs: { allow: [".", scenariosDir] },
  },
  test: {
    alias: { "@scenarios": scenariosDir },
    environment: "jsdom",
    setupFiles: "./src/testSetup.ts",
    pool: "threads",
    fileParallelism: false,
  },
});
