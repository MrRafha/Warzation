# Guia de desenvolvimento local

Este guia prepara o ambiente para o desenvolvimento do Warzation sem Docker.

## Pré-requisitos

- Java 21 (JDK);
- Node.js 20 LTS ou uma versão LTS mais recente compatível com Vite, e npm;
- PostgreSQL 15 ou superior;
- Git;
- VS Code.

Confirme as versões instaladas:

```powershell
java -version
node --version
npm --version
psql --version
git --version
```

O Backend usará Java 21 com Spring Boot. O Frontend usará React, TypeScript e
Vite. O PostgreSQL e os serviços da aplicação serão executados localmente.

## Configuração do PostgreSQL

Crie um banco e um usuário dedicados ao desenvolvimento:

```sql
CREATE USER warzation_dev WITH PASSWORD 'defina-uma-senha-local';
CREATE DATABASE warzation OWNER warzation_dev;
```

Não reutilize senhas de produção nem publique a senha em arquivos rastreados.
O serviço do PostgreSQL deve estar iniciado antes do Backend.

## Configuração local

### Backend

Configure o Maven localmente e confirme:

```powershell
mvn -version
```

Depois copie:

```powershell
Copy-Item Backend\src\main\resources\application-local.example.yml `
  Backend\src\main\resources\application-local.yml
```

Edite `application-local.yml` com o usuário e a senha do PostgreSQL local.
Esse arquivo é ignorado pelo Git.

### Frontend

Copie o arquivo de exemplo:

```powershell
Copy-Item Frontend\.env.example Frontend\.env
```

`VITE_API_BASE_URL` e `VITE_WS_URL` são obrigatórias: sem elas a tela inicial
avisa qual variável falta. Ajuste os valores somente se as portas locais forem
alteradas. O arquivo `.env` também é ignorado pelo Git.

Se o Frontend rodar em outra origem (outra porta ou `127.0.0.1`), libere-a no
Backend com a variável `CORS_ALLOWED_ORIGINS`, separando várias origens por
vírgula. Coringas (`*`) não são aceitos.

## Executar as aplicações

```powershell
# Backend
Set-Location Backend
mvn spring-boot:run

# Frontend, em outro terminal
Set-Location Frontend
npm install
npm run dev
```

O Backend aplica as migrations Flyway ao iniciar. Se o PostgreSQL estiver
parado, o banco não existir ou as credenciais estiverem incorretas, o startup
para com um bloco `APPLICATION FAILED TO START` que explica a causa e lista o
que conferir; corrija a configuração e execute novamente.

## Executar testes

Backend:

```powershell
Set-Location Backend
mvn test
```

Os testes usam o perfil `test` e um banco H2 em memória. Para validar a conexão
real com PostgreSQL, inicie o serviço, configure `DB_URL`, `DB_USERNAME` e
`DB_PASSWORD` e execute:

```powershell
Set-Location Backend
$env:SPRING_PROFILES_ACTIVE = "local"
mvn test
```

Frontend:

```powershell
Set-Location Frontend
npm test
```

O teste de integração da API grava e lê metadata usando a camada de
persistência. A migration PostgreSQL é aplicada automaticamente no perfil
local.

## Integração contínua

Todo push na `main` e todo pull request rodam `.github/workflows/ci.yml`:

- **Backend (testes):** `mvn verify`, com o H2 do perfil `test`;
- **Backend (smoke com PostgreSQL):** sobe a aplicação contra um PostgreSQL 16,
  aplica um comando, reinicia e confere persistência e idempotência
  (`.github/scripts/postgres-smoke.sh`);
- **Dados do mapa:** valida os cenários de `Backend/data/scenarios`
  (`tools/mapdata/validate_scenario.py`);
- **Frontend (testes e build):** `npm ci`, `npm test` e `npm run build`.

Um PR só deve ser mergeado com todos os jobs verdes.

## Contratos da API e do WebSocket

Rotas, formato de erro, comandos e mensagens do WebSocket estão em
[03_ARQUITETURA_E_STACK.md](./03_ARQUITETURA_E_STACK.md).

## Roteiros de verificação

### Jogadores de teste

O protótipo não tem contas. Defina os tokens dos dois jogadores de teste antes
de subir o Backend: pelo menos 16 caracteres cada, diferentes entre si, e nunca
versionados.

```powershell
$env:PLAYER_1_TOKEN = "um-token-local-do-jogador-1"
$env:PLAYER_2_TOKEN = "um-token-local-do-jogador-2"
mvn spring-boot:run
```

Ou coloque os valores em `application-local.yml`, que está no `.gitignore`
(veja `application-local.example.yml`). Sem token, o jogador fica desativado e
as ordens são recusadas com `401`.

No Frontend, digite o token no campo **Token do jogador de teste**. Cada aba
guarda o próprio jogador, então duas abas podem ser os dois jogadores.

### Dois clientes em tempo real

1. Inicie o PostgreSQL, o Backend e o Frontend.
2. Abra `http://localhost:5173` em duas abas (ou dois navegadores).
3. Entre como Jogador 1 em uma aba e como Jogador 2 na outra. Abra
   "Ferramentas de desenvolvimento" e confirme "Tempo real conectado".
4. Clique em **+1** em uma aba: o contador oficial muda nas duas.
5. Clique em **Reenviar último comando**: a aba mostra "duplicado, não
   reaplicado" e o contador não muda.
6. Pare o Backend: as abas mostram "Reconectando...". Suba de novo: elas
   voltam sozinhas com o mesmo contador.

O teste automatizado equivalente é `RoomWebSocketTests`.

### Partida com dois jogadores

1. Com os tokens configurados, abra o Frontend em duas abas.
2. Entre como Jogador 1 em uma e como Jogador 2 na outra.
3. Na aba do Jogador 1, crie uma partida em **Partidas abertas** e clique em
   **Entrar** ao lado da Nação do Parnaíba.
4. Na aba do Jogador 2, clique em **Atualizar** e entre com a Nação do São
   Francisco. Tentar a Nação do Parnaíba mostra que ela já foi escolhida.
5. Nas duas abas, clique em **Abrir partida**. Os dois veem "ao vivo".
6. Na aba do Jogador 1, que criou a partida, clique em **Iniciar partida**. As
   duas abas passam a mostrar o mapa colorido pelos donos, sem recarregar:
   Piauí e Bahia com 2 infantarias por província e o restante neutro.
7. Clique numa província para ver dono, unidades e estoque, e confira o
   **Histórico**.
8. Reinicie o Backend: as abas mostram "reconectando..." e voltam sozinhas
   com o mesmo estado.

A lista de partidas se atualiza sozinha a cada 10 s. Os testes automatizados
equivalentes são `MatchControllerTests`, `MatchStartTests`,
`MatchRealtimeTests` (dois clientes WebSocket), `MatchEventsTests` e
`MatchPersistenceAfterRestartTests`.

### Persistência após reinicialização

Com o Backend rodando (PowerShell):

```powershell
$id = [guid]::NewGuid().ToString()
$cmd = @{ commandId = $id; type = "INCREMENT_COUNTER"; payload = @{ amount = 1 } } | ConvertTo-Json
$auth = @{ Authorization = "Bearer $env:PLAYER_1_TOKEN" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/rooms/dev/commands `
  -ContentType "application/json" -Headers $auth -Body $cmd
Invoke-RestMethod http://localhost:8080/api/v1/rooms/dev
```

1. Anote `counter` e `version`.
2. Pare o Backend (`Ctrl+C`) e inicie de novo com `mvn spring-boot:run`.
3. Rode `Invoke-RestMethod http://localhost:8080/api/v1/rooms/dev`: os valores
   são os mesmos.
4. Reenvie o mesmo `$cmd` (com `-Headers $auth`): a resposta vem com `status: DUPLICATE` e o
   contador não muda.
5. Consulte `http://localhost:8080/api/v1/rooms/dev/commands/$id` para ver o
   resultado gravado.

O teste automatizado equivalente é `PersistenceAfterRestartTests`, que sobe a
aplicação três vezes sobre o mesmo banco H2 em arquivo.

## Problemas comuns

### Porta ocupada

Verifique qual processo usa uma porta:

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
```

Altere a porta do serviço responsável e atualize as URLs do Frontend.

### Falha de conexão com PostgreSQL

Confirme que:

1. o serviço do PostgreSQL está em execução;
2. o banco `warzation` existe;
3. usuário, senha, host e porta estão corretos;
4. `application-local.yml` foi criado a partir do exemplo;
5. nenhuma variável de ambiente está sobrescrevendo a configuração.

### Dependências não encontradas

Verifique Java, Node.js, npm e PostgreSQL com os comandos da seção de
pré-requisitos. Depois execute novamente o comando da aplicação correspondente.

## Segurança local

- Nunca versione `.env`, `application-local.yml` ou senhas.
- Use somente credenciais locais nos exemplos.
- Não coloque tokens ou credenciais em issues, README ou logs.
