import type { Abundance, Resource } from "./types";

export const resourceLabel: Record<Resource, string> = {
  PEDRA: "Pedra",
  METAL: "Metal",
  PETROLEO: "Petróleo",
};

export const abundanceLabel: Record<Abundance, string> = {
  BAIXA: "baixa",
  MEDIA: "média",
  ALTA: "alta",
};

/** Cores por terreno; terrenos novos caem na cor neutra. */
export const terrainColor: Record<string, string> = {
  PLANICIE_FERTIL: "#3f7f5a",
  PASTAGEM: "#7f8f45",
  CERRADO: "#9a8550",
  CAATINGA: "#8a6a4f",
  FLORESTA: "#2f5f45",
};

export const DEFAULT_TERRAIN_COLOR = "#4a5560";
