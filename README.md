# Warzation

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

- mapa de 8–12 províncias;
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
  pom.xml            Projeto Java 21/Spring Boot
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
  package.json       Projeto React/TypeScript/Vite
  public/            Arquivos públicos e assets
  src/
    components/      Componentes visuais compartilhados
    features/        Funcionalidades organizadas por domínio
    lib/              Clientes, estado e utilitários
    styles/           Estilos globais e tokens visuais
  tests/             Testes da interface e dos fluxos

Docs/
  01_CONTEXTO_DO_PROJETO.md
  02_DIRECAO_DE_DESENVOLVIMENTO.md
  MAPA_E_DADOS.md
  ECONOMIA_E_CONSTRUCOES.md
  TECNOLOGIAS_E_ERAS.md
  MILITAR_E_COMBATE.md
  LOGISTICA_E_CERCOS.md
  DIPLOMACIA_MERCADO_E_VITORIA.md
```

## Estado atual

As regras fundamentais e a sequência de desenvolvimento estão documentadas.
A stack técnica foi definida: Java 21 com Spring Boot no backend, React com
TypeScript e Vite no frontend, PostgreSQL como banco de dados e WebSocket para
comunicação em tempo real. O desenvolvimento será feito no VS Code, com Git e
GitHub para versionamento, em instalação local sem Docker.

Os detalhes e responsabilidades da stack estão em
[STACK_TECNICA.md](Docs/STACK_TECNICA.md).

## Documentação

- [Contexto do projeto](Docs/01_CONTEXTO_DO_PROJETO.md)
- [Direção de desenvolvimento](Docs/02_DIRECAO_DE_DESENVOLVIMENTO.md)
- [Mapa e dados](Docs/MAPA_E_DADOS.md)
- [Economia e construções](Docs/ECONOMIA_E_CONSTRUCOES.md)
- [Tecnologias e eras](Docs/TECNOLOGIAS_E_ERAS.md)
- [Militar e combate](Docs/MILITAR_E_COMBATE.md)
- [Logística e cercos](Docs/LOGISTICA_E_CERCOS.md)
- [Diplomacia, mercado e vitória](Docs/DIPLOMACIA_MERCADO_E_VITORIA.md)
- [Stack técnica](Docs/STACK_TECNICA.md)
- [Guia de desenvolvimento local](Docs/GUIA_DESENVOLVIMENTO_LOCAL.md)

## Próximo passo

Configurar os projetos Spring Boot e React/Vite e iniciar a Fase 0: mapa de
teste com 8–12 províncias, modelo inicial de província e nação, dados
configuráveis e protótipo visual para seleção de províncias.
