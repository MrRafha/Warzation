# Warzation

[![CI](https://github.com/MrRafha/Warzation/actions/workflows/ci.yml/badge.svg)](https://github.com/MrRafha/Warzation/actions/workflows/ci.yml)

Jogo de estratégia multiplayer para navegador, persistente e em tempo real. O
projeto combina desenvolvimento civilizacional, economia, guerra, logística e
diplomacia em um mundo compartilhado.

> **Princípio central:** não basta conquistar o mundo; é preciso conseguir
> governá-lo.

## Visão do produto

Cada jogador escolhe uma nação das Américas, desenvolve suas províncias,
pesquisa tecnologias, produz recursos, recruta exércitos e disputa território.
As ações são processadas por um servidor autoritativo e continuam evoluindo
quando os jogadores estão desconectados.

O ciclo principal é:

1. escolher uma nação;
2. desenvolver províncias e especializar sua economia;
3. produzir, armazenar e distribuir recursos;
4. pesquisar tecnologias e avançar pelas eras;
5. recrutar e movimentar exércitos;
6. manter linhas de abastecimento, atacar, cercar e conquistar;
7. negociar, formar alianças e buscar uma condição de vitória.

## Escopo

### Protótipo técnico

O primeiro marco valida a fundação do jogo com:

- mapa de teste do Nordeste com 24 províncias (cada estado dividido em 2–3 regiões);
- dois jogadores de teste;
- recorte funcional das Eras Antiga e Medieval;
- economia de comida, pedra e metal;
- construções, pesquisa, movimento, combate e abastecimento simplificados;
- persistência offline e sincronização entre clientes.

### MVP jogável

O MVP terá:

- cenário das Américas com aproximadamente 50–60 províncias;
- 3–6 jogadores e nações predefinidas;
- quatro eras: Antiga, Medieval, Industrial e Moderna;
- mapa, economia, construções, tecnologia, exércitos, combate e logística;
- estradas, pilhagem, cercos, diplomacia e mercado global;
- vitória por dominação territorial ou por coalizão diplomática;
- partidas planejadas para durar de 7 a 14 dias.

Transporte físico de comércio, guerra naval complexa, IA de civilizações
completas, população individual e aplicativo móvel ficam fora do MVP.

## Princípios técnicos

- **Servidor autoritativo:** o cliente envia ordens; o servidor valida e
  decide o estado oficial.
- **Mundo persistente:** construções, pesquisas, produção, movimentos e
  diplomacia continuam avançando sem jogadores conectados.
- **Dados configuráveis:** mapa, custos, tempos, atributos, tecnologias e
  regras devem ficar separados da lógica de execução.
- **Sistemas desacoplados:** mapa, economia, construções, pesquisa, militar,
  logística, diplomacia e ciclo da partida possuem responsabilidades claras.
- **Tempo e eventos idempotentes:** reinícios e reprocessamentos não podem
  duplicar custos, produção, movimento, combate ou recompensas.
- **Fidelidade cartográfica:** fronteiras reais ou reconhecíveis têm prioridade;
  o balanceamento deve ser feito por recursos e condições iniciais.

## Estrutura do repositório

```text
Backend/
  pom.xml            Projeto Maven com Java 21/Spring Boot
  data/              Dados configuráveis do mundo e das regras
  src/
    application/     Casos de uso e orquestração
    config/          Configuração da aplicação
    domain/          Regras e entidades do jogo
    infrastructure/ Persistência, relógio e integrações externas
    main/resources/  Configuração e dados de execução
    realtime/        Gateway de sincronização em tempo real
  tests/             Testes do servidor e do domínio

Frontend/
  package.json       Projeto npm com React/TypeScript/Vite
  public/            Arquivos públicos e assets
  src/
    components/      Componentes visuais compartilhados
    features/        Funcionalidades organizadas por domínio
    lib/              Clientes, estado e utilitários
    styles/           Estilos globais e tokens visuais
  tests/             Testes da interface e dos fluxos

tools/
  mapdata/          Scripts que geram e validam os dados do mapa

Docs/
  01_CONTEXTO_DO_PROJETO.md         Regras do jogo e pendências
  02_DIRECAO_DE_DESENVOLVIMENTO.md  Escopo, fases, aceitação e testes
  03_ARQUITETURA_E_STACK.md         Stack, módulos e modelo de dados
  04_GUIA_DESENVOLVIMENTO_LOCAL.md  Instalação, execução e API inicial
```

## Estado atual

As regras fundamentais e a sequência de desenvolvimento estão documentadas.
A stack técnica foi definida: Java 21 com Spring Boot no backend, React com
TypeScript e Vite no frontend, PostgreSQL como banco de dados e WebSocket para
comunicação em tempo real. O desenvolvimento será feito no VS Code, com Git e
GitHub para versionamento, em instalação local sem Docker.

Os detalhes e responsabilidades da stack estão em
[03_ARQUITETURA_E_STACK.md](Docs/03_ARQUITETURA_E_STACK.md).

A fundação técnica da Sprint 1 está implementada: persistência no PostgreSQL
com Flyway, API HTTP com erros padronizados e CORS restrito, WebSocket em
`/ws` com salas, e comandos validados pelo servidor e idempotentes, que
sobrevivem a reinicializações.

A Sprint 2 entregou o mapa de teste do Nordeste (24 províncias) servido pela
API, jogadores de teste identificados, partidas com escolha de nação, posição
inicial, sincronização da partida em tempo real e histórico de eventos.

## Documentação

- [Contexto do projeto](Docs/01_CONTEXTO_DO_PROJETO.md) — fonte única das regras do jogo
- [Direção de desenvolvimento](Docs/02_DIRECAO_DE_DESENVOLVIMENTO.md) — escopo, fases e critérios de aceitação
- [Arquitetura e stack](Docs/03_ARQUITETURA_E_STACK.md)
- [Guia de desenvolvimento local](Docs/04_GUIA_DESENVOLVIMENTO_LOCAL.md)

## Próximo passo

Fase 2: economia, armazenamento e construções sobre o estado da partida
(Docs/02_DIRECAO_DE_DESENVOLVIMENTO.md).
