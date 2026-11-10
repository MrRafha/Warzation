# Stack técnica

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
clientes. Mensagens devem ter contratos explícitos, validação no servidor e
identificação suficiente para evitar processamento duplicado.

## Ambiente local

O projeto será executado localmente, sem Docker. A instalação de Java 21,
Node.js, npm e PostgreSQL faz parte da configuração de desenvolvimento. As
variáveis de conexão e demais configurações locais não devem ser commitadas;
um arquivo de exemplo deverá documentar os nomes esperados.

## Organização inicial

- `Backend/`: aplicação Spring Boot, domínio, persistência e WebSocket.
- `Frontend/`: aplicação React/Vite, componentes, telas e cliente WebSocket.
- `Backend/data/`: dados configuráveis do mapa e regras do jogo.
- `Docs/`: decisões de produto, regras de domínio e contratos técnicos.

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
