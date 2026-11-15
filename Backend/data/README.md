# Dados do jogo

Dados configuráveis que o Backend carrega. Nada aqui contém lógica de regras:
os arquivos descrevem o mundo, e o código decide o que fazer com ele.

## Cenários

Cada cenário fica em `scenarios/<id>/` com dois arquivos:

| Arquivo | Conteúdo |
|---|---|
| `scenario.json` | Províncias, terrenos, depósitos, conexões e nações. |
| `provinces.geojson` | Uma geometria (`Polygon`, WGS84, lon/lat) por província, com `properties.id` igual ao `id` da província. |

Valide qualquer mudança com:

```bash
pip install -r tools/mapdata/requirements.txt
python tools/mapdata/validate_scenario.py Backend/data/scenarios/nordeste-teste
```

O CI roda essa validação em todo PR.

### Formato de `scenario.json` (versão 1)

```jsonc
{
  "formatVersion": 1,
  "id": "nordeste-teste",              // identificador estável do cenário
  "name": "...",
  "provisional": true,                 // valores de balanceamento são provisórios para teste
  "geometryFile": "provinces.geojson",
  "terrains": [
    { "id": "CAATINGA", "name": "Caatinga", "description": "..." }
  ],
  "provinces": [
    {
      "id": "piaui",                   // estável: minúsculas, sem acento, hífens
      "name": "Piauí",
      "mainCity": { "name": "Teresina", "lon": -42.8019, "lat": -5.0892 },
      "terrain": "CERRADO",            // id de "terrains"
      "coastal": true,                 // acesso ao mar; NÃO significa que existe Porto
      "deposits": [
        { "resource": "PEDRA", "abundance": "ALTA" }   // PEDRA | METAL | PETROLEO; BAIXA | MEDIA | ALTA
      ]
    }
  ],
  "connections": [["piaui", "ceara"]], // fronteira terrestre; cada par aparece uma vez e vale nos dois sentidos
  "nations": [
    {
      "id": "parnaiba",
      "name": "Nação do Parnaíba",
      "capital": "piaui",
      "startingProvinces": ["piaui", "maranhao", "ceara"]  // capital + duas províncias contíguas
    }
  ]
}
```

O que **não** entra no cenário, de propósito:

- **construções e estradas:** toda província começa vazia, e o validador rejeita
  esses campos;
- **espaços de construção:** são regra do jogo (3 por província comum, 4 por
  capital), não dado do mapa;
- **unidades, estoques e guarnições neutras:** são estado inicial da partida,
  definidos na issue de posição inicial.

## Cenário `nordeste-teste`

Mapa do protótipo técnico: os 9 estados do Nordeste, cada um como uma
província, com a capital estadual como cidade principal.

### Fronteiras e conexões

As 14 conexões foram **calculadas a partir da geometria** (estados que
compartilham fronteira terrestre), e o validador confere que a lista continua
igual à geometria. Nenhuma fronteira foi criada ou alterada para equilibrar
o mapa.

### Nações de teste

| Nação | Capital | Províncias iniciais | Perfil |
|---|---|---|---|
| Nação do Parnaíba | Piauí | Piauí, Maranhão, Ceará | Rica em pedra; comida no Maranhão |
| Nação do São Francisco | Bahia | Bahia, Sergipe, Alagoas | Rica em metal; comida em Alagoas e pastagem em Sergipe |

Pernambuco, Paraíba e Rio Grande do Norte começam neutros e formam a região
disputada entre as duas nações. Cada nação tem uma vantagem diferente (pedra
contra metal), o que dá motivo para negociar no mercado. Piauí e Bahia fazem
fronteira direta, então as capitais são vizinhas.

### Terrenos e depósitos

O terreno é o **predominante, simplificado** para o jogo. Os depósitos partem
de ocorrências reais conhecidas (por exemplo, petróleo no Rio Grande do Norte,
em Sergipe e na Bahia; metal na Bahia; rochas ornamentais no Ceará e no
Piauí), mas a abundância é **provisória para teste** e pode ser ajustada no
balanceamento sem mexer nas fronteiras. O petróleo só tem uso a partir da Era
Industrial, fora do recorte do protótipo.

O Maranhão foi classificado como planície fértil pela Baixada Maranhense e
pelos vales agrícolas do Mearim e do Pindaré. É a escolha mais discutível do
cenário e pode mudar no balanceamento.

### Geometria: fonte e processamento

- **Fonte:** limites estaduais da LAGEAMB/UFPR, derivados da malha do IBGE,
  distribuídos em GeoJSON por
  [giuliano-oliveira/geodata-br-states](https://github.com/giuliano-oliveira/geodata-br-states)
  (licença MIT), commit `32d7169859c5dad3bd7ae4f2710733b96b02d40f`, arquivo
  `geojson/br_states.json`. As áreas conferem com as oficiais do IBGE com
  diferença abaixo de 1%.
- **Processamento** (`tools/mapdata/build_geometry.py`):
  1. mantém só o polígono principal de cada estado: ilhas pequenas, como
     Fernando de Noronha, ficam de fora, mas a ilha de São Luís continua;
  2. simplifica com topologia compartilhada (`topojson`, tolerância de 0,03°),
     para que a fronteira entre dois estados seja a mesma linha nos dois lados;
  3. arredonda as coordenadas para 4 casas decimais (~11 m).

  O erro de área depois da simplificação ficou abaixo de 1% em todas as províncias.

Para regenerar a geometria:

```bash
git clone --depth 1 https://github.com/giuliano-oliveira/geodata-br-states /tmp/geodata
python tools/mapdata/build_geometry.py /tmp/geodata/geojson/br_states.json \
  Backend/data/scenarios/nordeste-teste/provinces.geojson 0.03
```
