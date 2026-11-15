# Warzation — Arquitetura e stack

## Stack aprovada

| Área | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot + Maven instalado localmente |
| Frontend | React + TypeScript + Vite |
| Banco de dados | PostgreSQL |
| Comunicação em tempo real | WebSocket |
| Editor | VS Code |
| Versionamento | Git + GitHub |
| Ambiente | Instalação local, sem Docker |

## Responsabilidades

### Backend

O backend será o servidor autoritativo do jogo. Ele deverá:

- validar todas as ordens recebidas do cliente;
- aplicar regras de mapa, economia, construções, pesquisa, militar,
  logística, diplomacia e vitória;
- persistir o estado no PostgreSQL;
- processar eventos temporizados mesmo sem jogadores conectados;
- publicar atualizações e eventos pelo WebSocket;
- garantir idempotência e registrar operações críticas.

### Frontend

O frontend será responsável pela apresentação e interação:

- renderizar o mapa e as lentes de informação;
- apresentar estado recebido do servidor;
- enviar ordens e exibir validações e resultados;
- manter componentes organizados por funcionalidade;
- não decidir saldo, propriedade, conclusão, combate ou vencedor.

### Banco de dados

O PostgreSQL será a fonte persistente do estado do mundo. O modelo deve
preservar timestamps de ações, eventos importantes e dados suficientes para
retomar o processamento após reinicializações.

### Comunicação

O WebSocket será usado para sincronização bidirecional entre servidor e
clientes, para que a interface receba atualizações sem recarregar a página. Mensagens devem ter contratos explícitos, validação no servidor e
identificação suficiente para evitar processamento duplicado.

### Mapa

O mapa é servido pelo backend, persistido no PostgreSQL e consumido pelo frontend. Províncias são nós de um grafo e fronteiras terrestres navegáveis são arestas. Geometrias e conteúdo cartográfico ficam em dados estruturados, nunca embutidos nos componentes da interface.

## Arquitetura lógica

Separar responsabilidades mantém o projeto testável.

### Módulos de domínio sugeridos

| Módulo | Responsabilidade |
|---|---|
| **World/Map** | Províncias, geometrias, cidades, terrenos, depósitos, fronteiras e adjacências. |
| **Players/Nations** | Jogadores, nações, capitais, propriedade inicial e eliminação. |
| **Economy** | Recursos, produção, estoques, consumo, custos e mercado. |
| **Buildings** | Requisitos, espaços, construção, conclusão e efeitos. |
| **Research** | Árvore tecnológica, pré-requisitos, fila única, eras, Biblioteca e marcos. |
| **Military** | Unidades, exércitos, recrutamento, movimento e combate. |
| **Logistics** | Estradas, capacidade, rotas alimentares, prioridades, reservas e abastecimento. |
| **Siege/Pillage** | Isolamento, saque, dano de infraestrutura, desgaste e rendição. |
| **Diplomacy** | Tratados, propostas, aceite, expiração, alianças e acesso. |
| **Match/Lifecycle** | Relógio, persistência, eventos agendados, vitória e eliminação. |
| **Realtime Gateway** | Sincronização de estado e notificações para os clientes. |

Os módulos são uma divisão conceitual. A equipe pode adaptá-los à arquitetura selecionada, mantendo as responsabilidades separadas.

### Modelo orientado a dados

Manter definições separadas do código de execução. Entidades/configurações sugeridas:

- `nations`: nomes, capital, províncias iniciais e configuração de equilíbrio;
- `provinces`: geometria, cidade principal, fronteiras, terreno, depósitos, costa e espaços;
- `resources`: tipos, era de disponibilidade e regras de estoque;
- `buildings`: custo, tempo, tecnologia, terreno, efeito e espaço consumido;
- `technologies`: era, pré-requisitos, desbloqueios, bônus e marco associado;
- `units`: categoria, era, atributos, custo, treinamento e consumo;
- `roads`: par de províncias conectadas, condição, capacidade, velocidade e estado;
- `supply_rules`: demanda fixa, prioridades, capacidade e política de rota;
- `market_rules`: tipos de oferta, taxa e forma de liquidação;
- `diplomacy_rules`: tratados, requisitos, permissões e duração;
- `victory_rules`: 60% territorial e estabilidade diplomática;
- `match_rules`: relógio, ciclo de produção e timers.

Os nomes são sugestões de organização, não exigência de banco ou formato.

### Tempo, eventos e idempotência

Evitar depender de um processo contínuo por jogador conectado.

- Persistir timestamps de início e conclusão de ações.
- Persistir o último instante de produção/consumo aplicado.
- Ao processar uma entidade, calcular o tempo transcorrido e aplicar somente os efeitos ainda não processados.
- Usar tarefas agendadas para ações que precisam ser resolvidas no horário.
- Registrar eventos críticos de forma transacional sempre que possível.
- Garantir idempotência: executar novamente um evento não pode duplicar produção, cobrança, movimento, combate ou recompensa.

### Segurança e autoridade

- Cliente nunca define saldo, propriedade, conclusão ou vencedor.
- Validar toda ordem no servidor: propriedade, recursos, tecnologia, espaço, conexão, tratado e estado da partida.
- Proteger operações simultâneas de estoque para impedir gastos duplicados.
- Registrar histórico de operações econômicas, militares e diplomáticas.
- Não confiar em timers ou relógios enviados pelo navegador.

## Contrato da API HTTP

Todas as rotas da aplicação ficam sob `/api/v1`. O corpo é sempre JSON.

| Método e rota | Uso | Sucesso |
|---|---|---|
| `GET /api/v1/health` | Verifica se o Backend está ativo. | `200` com `status`, `service`, `version` |
| `POST /api/v1/metadata` | Grava um par chave/valor de teste. | `201` |
| `GET /api/v1/metadata/{key}` | Lê um par chave/valor. | `200` |
| `GET /api/v1/rooms/{roomId}` | Estado oficial de uma sala de teste. | `200` |
| `POST /api/v1/rooms/{roomId}/commands` | Envia um comando para a sala (exige identificação). | `201` aplicado, `200` reenvio |
| `GET /api/v1/rooms/{roomId}/commands/{commandId}` | Consulta o resultado de um comando. | `200` |
| `GET /api/v1/players/me` | Jogador do token enviado (exige identificação). | `200` com `id`, `name` |
| `GET /api/v1/scenarios` | Lista os cenários carregados. | `200` |
| `GET /api/v1/scenarios/{id}` | Cenário completo: `scenario` (conteúdo de `scenario.json`), `buildingSlots` e `geometry` (GeoJSON). Envia `ETag`; com `If-None-Match` igual responde `304`. | `200` |
| `GET /api/v1/scenarios/{id}/provinces/{provinceId}/neighbours` | Vizinhos de uma província. | `200` |
| `GET /api/v1/scenarios/{id}/adjacency?from=&to=` | Se duas províncias fazem fronteira. | `200` com `adjacent` |
| `GET /api/v1/scenarios/{id}/routes?from=&to=` | Menor rota terrestre, em número de fronteiras. | `200` com `provinces` e `borders` |

Erros seguem sempre o mesmo formato e nunca expõem stack traces:

```json
{
  "timestamp": "2026-11-15T16:00:00Z",
  "status": 400,
  "code": "INVALID_COMMAND",
  "message": "Comando inválido",
  "path": "/api/v1/rooms/dev/commands",
  "fields": { "payload.amount": "deve estar entre 1 e 100" }
}
```

| Status | `code` | Quando |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_COMMAND`, `MALFORMED_REQUEST` | Dados inválidos ou JSON ilegível. `fields` detalha cada campo. |
| 401 | `UNAUTHENTICATED` | Ordem sem token de jogador ou com token inválido. Vem com `WWW-Authenticate: Bearer`. |
| 404 | `NOT_FOUND` | Rota, sala, comando ou chave inexistente. |
| 405 | `METHOD_NOT_ALLOWED` | Método HTTP não suportado na rota. |
| 422 | `NO_ROUTE` | Não existe rota terrestre entre as províncias pedidas. |
| 409 | `COMMAND_CONFLICT`, `CONFLICT` | `commandId` reutilizado com outra intenção ou conflito de gravação. |
| 500 | `INTERNAL_ERROR` | Erro inesperado; o detalhe fica só no log do servidor. |

O CORS libera apenas as origens de `CORS_ALLOWED_ORIGINS` (padrão
`http://localhost:5173`), nos métodos `GET` e `POST`. Coringas (`*`) são
recusados no startup.

## Cenários (mapa)

No startup, `ScenarioCatalog` lê todos os `data/scenarios/*/scenario.json` do
classpath (o Maven copia `Backend/data` para dentro do jar), valida cada um e
grava a definição nas tabelas `scenario_*` (migration `V3`).

- **Validação:** campos desconhecidos (inclusive `buildings` e `roads`), ids
  repetidos, conexões inválidas, grafo desconexo, estados com menos de 2 ou mais
  de 3 províncias, regras de nação e falta de geometria. Um cenário inválido
  **impede o startup**, com a lista de problemas e o arquivo de origem.
- **Idempotência:** o cenário é gravado com o hash SHA-256 do seu conteúdo. Se
  o hash não mudou, o banco não é tocado. Se mudou, a definição é substituída.
- **Espaços de construção:** são regra do jogo (`BuildingSlots`: 3 por
  província, 4 nas capitais), calculados pelo servidor e gravados junto da província.
- **Geometria:** não vai para o banco, porque é apresentação. A API serve a
  geometria a partir do arquivo.
- **Consultas:** `ProvinceGraph` responde vizinhos, adjacência e menor rota.
  É a base para movimento e abastecimento.

Quando partidas passarem a referenciar províncias (#18), substituir um cenário
usado por partidas exigirá um novo `id` de cenário.

## Identificação de jogadores (provisória)

O protótipo **não tem contas**. Existem jogadores de teste configurados no
Backend (`application.players.test-players`), cada um com um token definido
por variável de ambiente (`PLAYER_1_TOKEN`, `PLAYER_2_TOKEN`) ou em
`application-local.yml`. Nenhum token real é versionado; só o perfil `test` tem
tokens fixos, usados pelos testes automatizados.

- **HTTP:** `Authorization: Bearer <token>`. Rotas de ordem usam `@CurrentPlayer`;
  sem token válido, a resposta é `401 UNAUTHENTICATED`. Leituras públicas
  (health, mapa) não exigem identificação.
- **WebSocket:** navegadores não enviam cabeçalhos no handshake, então a
  identificação é a mensagem `AUTHENTICATE`. Sem ela, `JOIN_ROOM`, `LEAVE_ROOM`
  e `COMMAND` são recusados. Três tokens inválidos seguidos fecham a conexão,
  e uma conexão não pode trocar de jogador.
- **O jogador sempre vem do token**, nunca do corpo da mensagem: um campo
  `playerId` enviado pelo cliente é ignorado. Cada comando registra o
  `player_id` de quem o enviou, e reutilizar o `commandId` de outro jogador
  gera `409`.
- Tokens precisam ter ao menos 16 caracteres e ser diferentes entre si. Ficam
  em memória só como hash SHA-256 e nunca vão para o log. Jogador com token
  vazio fica desativado.
- **Frontend:** o token é digitado na tela e guardado por aba
  (`sessionStorage`), então duas abas podem ser dois jogadores.

Quando houver contas de verdade, `PlayerDirectory` e `CurrentPlayerArgumentResolver`
são os pontos a substituir.

## Comandos e idempotência

O cliente nunca grava estado: ele envia **comandos** (intenções) e o servidor
decide o resultado. Não existe rota que altere o estado de uma sala
diretamente, e campos extras no comando (por exemplo `counter`) são ignorados.

```json
{
  "commandId": "8d0c7a2e-5b8e-4c3f-9f0a-2a7d6c1e9b44",
  "type": "INCREMENT_COUNTER",
  "payload": { "amount": 1 }
}
```

- `commandId` é um UUID gerado pelo cliente. Reenviar o mesmo `commandId` (por
  queda de rede, clique duplo ou reconexão) devolve o resultado já gravado com
  `status: "DUPLICATE"` e não reaplica o efeito.
- Reutilizar um `commandId` com outra sala, tipo ou conteúdo gera `409`.
- A sala é bloqueada (`SELECT ... FOR UPDATE`) durante o comando; o efeito e o
  registro em `processed_commands` são gravados na mesma transação.
- `processed_commands` persiste `processed_at` e o estado resultante, então a
  proteção contra reprocessamento continua valendo após reinicializações.
- A sala `dev` é criada pela migration `V2` e serve como sala de teste da
  Sprint 1. O único comando por enquanto é `INCREMENT_COUNTER`, com `amount`
  inteiro entre 1 e 100.

## Contrato do WebSocket

Endpoint: `ws://<host>:8080/ws` (`VITE_WS_URL` no Frontend). O handshake só é
aceito das origens de `CORS_ALLOWED_ORIGINS`. Mensagens são JSON com no máximo
8 KB.

Cliente → servidor:

```json
{ "type": "JOIN_ROOM", "id": "<id do cliente>", "payload": { "roomId": "dev" } }
```

| `type` | `payload` | Efeito |
|---|---|---|
| `AUTHENTICATE` | `{ "token" }` | Identifica a conexão e recebe `AUTHENTICATED` com `{ "id", "name" }`. Precisa vir antes das mensagens abaixo. |
| `JOIN_ROOM` | `{ "roomId" }` | Entra na sala (sai da anterior) e recebe `ROOM_STATE`. |
| `LEAVE_ROOM` | — | Sai da sala e recebe `LEFT_ROOM`. |
| `COMMAND` | comando da seção anterior | Executa na sala atual e recebe `COMMAND_RESULT`. |
| `PING` | — | Recebe `PONG`. |

Servidor → cliente:

```json
{ "type": "ROOM_STATE", "id": "<uuid>", "replyTo": "<id do cliente>", "roomId": "dev",
  "payload": { "roomId": "dev", "counter": 3, "version": 3, "updatedAt": "..." } }
```

| `type` | Quando | `payload` |
|---|---|---|
| `CONNECTED` | Logo após a conexão. | `{ "sessionId" }` |
| `AUTHENTICATED` | Resposta a `AUTHENTICATE` com token válido. | `{ "id", "name" }` do jogador |
| `ROOM_STATE` | Resposta a `JOIN_ROOM` e a **todos** da sala após cada mudança confirmada no banco. | estado da sala |
| `COMMAND_RESULT` | Resposta a `COMMAND`, só para quem enviou. | mesmo corpo do resultado HTTP |
| `LEFT_ROOM` | Resposta a `LEAVE_ROOM`. | — |
| `PONG` | Resposta a `PING`. | — |
| `ERROR` | Mensagem rejeitada. | `{ "code", "message", "fields"? }` |

Códigos de `ERROR`: `UNAUTHENTICATED` (sem `AUTHENTICATE` ou token inválido),
`ALREADY_AUTHENTICATED` (tentativa de trocar o jogador da conexão),
`INVALID_MESSAGE` (JSON inválido ou sem `type`),
`UNKNOWN_TYPE`, `NOT_IN_ROOM`, `ROOM_NOT_FOUND`, `INVALID_COMMAND`,
`COMMAND_CONFLICT`, `INTERNAL_ERROR`. Toda rejeição é registrada no log com o
id da sessão.

`id` identifica cada mensagem; `replyTo` liga uma resposta à mensagem do
cliente. Comandos enviados por HTTP também disparam `ROOM_STATE` para a sala.

**Reconexão:** o cliente reconecta com espera crescente (1 s, 2 s, 4 s... até
10 s), envia `AUTHENTICATE` e `JOIN_ROOM` de novo e substitui o que estiver na tela pelo
`ROOM_STATE` recebido. O servidor não guarda estado de jogo na sessão; ela
serve apenas para saber quem recebe as atualizações.

## Ambiente local

O projeto será executado localmente, sem Docker. A instalação de Java 21,
Node.js, npm e PostgreSQL faz parte da configuração de desenvolvimento. As
variáveis de conexão e demais configurações locais não devem ser commitadas;
um arquivo de exemplo deverá documentar os nomes esperados.

## Organização inicial

- `Backend/`: aplicação Spring Boot, domínio, persistência e WebSocket.
- `Frontend/`: aplicação React/Vite, componentes, telas e cliente WebSocket.
- `Backend/data/`: dados configuráveis do mapa e regras do jogo; formato e fontes em `Backend/data/README.md`.
- `tools/mapdata/`: scripts para gerar e validar os dados do mapa.
- `Docs/`: regras do jogo, direção de desenvolvimento, arquitetura e guia local.

O Maven deve estar instalado localmente e disponível no `PATH`. Os comandos
do Backend usam `mvn` diretamente.

O PostgreSQL é o banco de produção/desenvolvimento local. O perfil `test` usa
um banco H2 em memória apenas para testes automatizados rápidos; a migration
do PostgreSQL continua versionada separadamente em `src/main/resources`.

As migrations do schema são gerenciadas pelo Flyway. O Backend não deve criar
ou alterar tabelas automaticamente via Hibernate.

## Critérios para a primeira implementação

1. O backend inicia e conecta ao PostgreSQL local.
2. O frontend inicia com Vite e consegue acessar o backend.
3. Dois clientes recebem o mesmo estado de uma partida pelo WebSocket.
4. Uma ordem inválida é rejeitada pelo backend com erro explícito.
5. O estado persistido sobrevive à reinicialização do backend.
