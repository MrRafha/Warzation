# Mapa e dados

Documento de trabalho para a Fase 0. As regras consolidadas estão em
[01_CONTEXTO_DO_PROJETO.md](./01_CONTEXTO_DO_PROJETO.md).

O mapa será servido pelo backend Java/Spring Boot, persistido no PostgreSQL e
consumido pelo frontend React/TypeScript. Geometrias e regras cartográficas
devem continuar em dados estruturados, sem serem embutidas nos componentes da
interface.

## Responsabilidades

- representar províncias como nós de um grafo;
- representar fronteiras terrestres navegáveis como arestas;
- registrar cidade principal, terreno, depósitos, costa e espaços;
- separar geometria e conteúdo cartográfico das regras de execução;
- manter nações, capitais e posições iniciais como dados configuráveis.

## Dados provisórios do protótipo

O protótipo deverá conter 8–12 províncias, dois jogadores de teste, terrenos,
depósitos, cidades e adjacências. Nenhum edifício deve começar instalado.

## Pendências

- definir a lista final de províncias das Américas;
- registrar fontes geográficas;
- definir geometrias e fronteiras;
- definir nações, capitais e províncias iniciais;
- calibrar depósitos e produtividade.
