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

Quando o projeto Vite estiver criado, copie:

```powershell
Copy-Item Frontend\.env.example Frontend\.env
```

Ajuste `VITE_API_BASE_URL` e `VITE_WS_URL` somente se as portas locais forem
alteradas. O arquivo `.env` também é ignorado pelo Git.

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
parado ou as credenciais estiverem incorretas, a aplicação falhará no startup
com uma mensagem de conexão no log; corrija a configuração e execute novamente.

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

## Contrato inicial da API

### Saúde

```text
GET /actuator/health
GET /api/v1/health
```

`/api/v1/health` retorna `200` com `status`, `service` e `version`.

### Metadata de persistência

```text
POST /api/v1/metadata
GET  /api/v1/metadata/{key}
```

O POST exige `key` e `value`. Erros de validação retornam `400` com `code`,
`message`, `path`, `timestamp` e `fields`. Erros internos não expõem stack
traces ou credenciais.

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
