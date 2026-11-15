/** Formato de Backend/data/scenarios/<id>/scenario.json (versão 1). */

export type Abundance = "BAIXA" | "MEDIA" | "ALTA";
export type Resource = "PEDRA" | "METAL" | "PETROLEO";

export type Deposit = {
  resource: Resource;
  abundance: Abundance;
};

export type Province = {
  id: string;
  name: string;
  state: string;
  mainCity: { name: string; lon: number; lat: number };
  terrain: string;
  coastal: boolean;
  deposits: Deposit[];
};

export type Nation = {
  id: string;
  name: string;
  capital: string;
  startingProvinces: string[];
};

export type ScenarioData = {
  formatVersion: number;
  id: string;
  name: string;
  provisional: boolean;
  geometryFile: string;
  states: Array<{ id: string; name: string }>;
  terrains: Array<{ id: string; name: string; description: string }>;
  provinces: Province[];
  connections: Array<[string, string]>;
  nations: Nation[];
};

export type Ring = Array<[number, number]>;
export type PolygonCoords = Ring[];

export type ProvinceGeometry =
  | { type: "Polygon"; coordinates: PolygonCoords }
  | { type: "MultiPolygon"; coordinates: PolygonCoords[] };

export type GeometryCollection = {
  type: "FeatureCollection";
  features: Array<{ type: "Feature"; properties: { id: string }; geometry: ProvinceGeometry }>;
};

/** Cenário pronto para a interface: dados, geometrias e consultas já indexadas. */
export type Scenario = {
  data: ScenarioData;
  geometries: Map<string, ProvinceGeometry>;
  provinces: Map<string, Province>;
  neighbours: Map<string, string[]>;
  stateNames: Map<string, string>;
  terrainNames: Map<string, string>;
  capitals: Set<string>;
};
