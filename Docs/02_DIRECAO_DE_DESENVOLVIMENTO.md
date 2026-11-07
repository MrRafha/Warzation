# DIREÇÃO DE DESENVOLVIMENTO — GRAND STRATEGY RTS DE NAVEGADOR

**Versão:** 2.0  
**Última atualização:** 7 de novembro de 2026  
**Status:** Direção de execução consolidada.  
**Documento de referência:** `01_CONTEXTO_DO_PROJETO.md`

Este documento transforma a visão do jogo em um plano de execução. Ele define os marcos de entrega, o escopo do protótipo técnico, o escopo da primeira versão completa jogável, os critérios de aceitação e as prioridades para a equipe.

---

## 1. Objetivo do desenvolvimento

Construir um jogo de estratégia multiplayer para navegador no qual o ciclo principal seja:

**escolher uma nação → desenvolver províncias → produzir e distribuir recursos → pesquisar tecnologias → recrutar e movimentar exércitos → disputar território e abastecimento → negociar, cercar e vencer.**

O desenvolvimento será dividido em dois marcos distintos:

1. **Protótipo técnico inicial:** validar a fundação do mundo persistente usando um recorte de mapa, dois jogadores e conteúdo funcional das Eras Antiga e Medieval.
2. **Primeira versão completa jogável (MVP jogável):** entregar o cenário das Américas, 50–60 províncias, 3–6 jogadores, todas as mecânicas fundamentais e as quatro eras com conteúdo reduzido e balanceado para partidas de 7–14 dias.

Não se deve confundir o protótipo técnico com a versão completa do MVP.

### 1.1 Princípios de desenvolvimento

- **Servidor autoritativo:** o cliente envia ordens; o servidor valida e decide o estado oficial.
- **Tempo persistente:** eventos continuam avançando sem jogadores conectados.
- **Dados configuráveis:** custos, tempos, atributos, produção e limiares ficam em dados/configurações.
- **Sistemas desacoplados:** mapa, economia, pesquisa, movimento, combate, logística, diplomacia e mercado possuem responsabilidades claras.
- **Escopo progressivo:** validar sistemas essenciais antes de expandir o conteúdo.
- **Fidelidade cartográfica:** preservar fronteiras reais/reconhecíveis; balancear recursos e condições iniciais sem inventar fronteiras.
- **Rastreabilidade:** eventos importantes geram registros e relatórios compreensíveis.

---

## 2. Escopo da primeira versão completa jogável (MVP jogável)

Esta é a entrega de produto a ser considerada quando a equipe disser que o MVP está pronto para teste fechado.

| Sistema | Escopo obrigatório |
|---|---|
| **Cenário** | Américas: América do Norte, Central e do Sul. |
| **Mapa** | Aproximadamente 50–60 províncias; fronteiras realistas/reconhecíveis; geografia e conexões fixas. |
| **Nações** | 3–6 jogadores escolhem nações predefinidas. Cada nação começa com capital e duas províncias próximas. |
| **Força inicial** | Duas unidades de infantaria básica em cada uma das três províncias iniciais: seis unidades por jogador. Estoques e força inicial equilibrados. |
| **Neutros** | Províncias não escolhidas podem começar neutras, com pequenas guarnições e sem edifícios ou estradas. Sem IA econômica completa. |
| **Construção** | Três espaços por província comum e quatro por capital; todas começam vazias; sem níveis individuais de edifícios. |
| **Recursos** | Comida e pedra na Antiga; metal a partir da Medieval; dinheiro e petróleo a partir da Industrial. |
| **Economia** | Estoques por província, demanda alimentar fixa por província, consumo de comida por tropas e abastecimento automático de comida. |
| **Mercado** | Mercado global com pedidos de compra/venda, escambo e taxa de 15% enquanto a moeda não estiver desbloqueada. |
| **Construções** | Fazenda, Celeiro, Mina, Porto, Curral, Acampamento, Biblioteca e Banco, cada uma disponível na era apropriada; o uso específico do Porto no MVP precisa ser detalhado. |
| **Tecnologia** | Quatro eras com evolução individual, uma pesquisa ativa por jogador e marcos de construção que concedem bônus únicos de 50% em pesquisas relacionadas. |
| **Militar** | Corpo a corpo, ranged, cavalaria e cerco; recrutamento em Acampamentos; movimento em tempo real. |
| **Combate** | Automático, em rodadas, com composição, atributos, terreno, fortificações, tecnologias e abastecimento; reforços podem chegar. |
| **Estradas** | Construídas sobre conexões entre províncias; aumentam deslocamento e capacidade/velocidade de abastecimento; podem ser danificadas ou destruídas. |
| **Logística** | Lente de abastecimento com setas, excedentes, déficits, prioridades, múltiplos fornecedores e rotas alternativas. No MVP, transporte automático de recursos limita-se à comida. |
| **Pilhagem e cercos** | Saque, sabotagem, interrupção de rotas, consumo de reservas, desgaste e rendição gradual. |
| **Diplomacia** | Paz, pacto de não agressão, aliança, acesso militar e acordo comercial. |
| **Vitória** | Dominação com 60% de todas as províncias ou vitória diplomática compartilhada. |
| **Persistência** | Mundo continua ativo offline; construções, pesquisas, produção, viagens e ordens permanecem registradas. |
| **Duração** | Partidas planejadas para 7–14 dias. |
| **Interface** | Mapa, província, recursos, construções, pesquisa, exércitos, lente de abastecimento, mercado, diplomacia e relatórios. |

### 2.1 Eras na versão completa

A primeira versão completa jogável deve incluir as quatro eras:

1. Antiga;
2. Medieval;
3. Industrial;
4. Moderna.

O conteúdo de cada era pode ser reduzido para caber em uma partida de 7–14 dias. A estrutura tecnológica deve suportar as quatro eras, mas não é necessário produzir uma grande quantidade de unidades, edifícios e pesquisas por era.

O Banco, dinheiro e petróleo pertencem à fase Industrial. Metalurgia pertence à Medieval. A infraestrutura de unidades aéreas da Era Moderna ainda precisa ser detalhada; não presumir que Aeroporto esteja aprovado como construção.

### 2.2 Conteúdo explicitamente fora do MVP

Não implementar na primeira versão completa, salvo decisão futura:

- transporte comercial físico com comboios terrestres ou embarcações;
- saque/interceptação de comboios comerciais;
- simulação de navios mercantes e rotas comerciais físicas;
- guerra naval complexa e combate naval próprio;
- IA que administre uma civilização completa;
- população simulada individualmente;
- deterioração automática de recursos;
- níveis ou melhorias individuais de edifícios;
- controle tático individual dos soldados durante o combate;
- editor de mapas ou geração procedural do cenário;
- aplicativo móvel nativo;
- vitória econômica separada;
- sistemas políticos/governamentais extensos e eventos aleatórios complexos.

O mercado global e a diplomacia comercial simplificada substituem o transporte físico durante o MVP.

---

## 3. Protótipo técnico inicial

### 3.1 Objetivo

Provar que o servidor consegue manter um mundo persistente e que economia, tecnologia, movimento, combate e abastecimento funcionam juntos antes da produção completa do conteúdo.

### 3.2 Escopo mínimo

- mapa de teste com aproximadamente 8–12 províncias;
- dois jogadores de teste;
- recorte funcional das Eras Antiga e Medieval;
- dados de terreno, depósitos e adjacências;
- províncias com espaços de construção e estoques locais;
- produção de comida, pedra e metal conforme os requisitos da era;
- Fazenda, Mina, Celeiro, Acampamento e Biblioteca em versão inicial;
- uma pesquisa ativa por jogador;
- infantaria básica e ao menos uma categoria adicional para validar o sistema de unidade;
- movimento em tempo real pelo grafo;
- combate automático simplificado;
- abastecimento alimentar básico;
- persistência offline e sincronização de dois clientes.

O protótipo poderá usar dados de mapa reduzidos ou simplificados para validar as regras. Ele não substitui o trabalho cartográfico necessário ao cenário definitivo das Américas.

### 3.3 Critérios de saída do protótipo

- Dois clientes veem o mesmo estado territorial.
- Uma ordem enviada por um cliente é validada pelo servidor e refletida no outro.
- Construções e pesquisas concluem com base no tempo do servidor.
- Produção e consumo são atualizados sem o navegador aberto.
- Exércitos percorrem conexões válidas e chegam ao destino.
- Um combate básico é resolvido pelo servidor.
- O estado sobrevive ao reinício do servidor sem duplicação de recursos, ordens ou resultados.

---

## 4. Regras operacionais que orientam a implementação

### 4.1 Mapa e fronteiras

Cada província deve ter identificador estável, geometria, cidade principal, terreno, depósitos naturais, adjacências e acesso costeiro quando aplicável.

**A prioridade é a fidelidade das fronteiras reais ou reconhecíveis.** A distribuição e produtividade de recursos podem ser ajustadas para balanceamento; não se deve criar fronteiras fictícias apenas para dar recursos equivalentes a cada nação.

O cenário completo terá 50–60 províncias nas Américas. Cada província corresponde a uma região geográfica/cidade-estado com cidade principal.

### 4.2 Nações e posição inicial

- Nação jogável predefinida.
- Uma capital e duas províncias próximas.
- Três províncias iniciais no total.
- Duas unidades de infantaria básica em cada província inicial.
- Seis unidades por jogador.
- Estoques iniciais e potencial econômico equilibrados.
- Territórios neutros com pequenas guarnições, sem construções e sem estradas.

A lista final de nações e a atribuição de províncias iniciais devem ser aprovadas no conteúdo do mapa. A seleção de nações deve evitar que fronteiras, recursos ou posição produzam vantagem inicial excessiva.

### 4.3 Terrenos e construções

Nenhum terreno ou cidade começa com construções prontas. Terreno e depósito definem elegibilidade e modificadores, não infraestrutura instalada.

| Tipo de província | Espaços |
|---|---:|
| Comum | 3 |
| Capital | 4 |

Todas as províncias começam vazias. Não implementar níveis ou melhorias individuais de edifícios. Estradas são infraestrutura de conexão e não ocupam espaços.

Construções aprovadas:

1. Fazenda — comida;
2. Celeiro — capacidade de armazenamento de comida;
3. Mina — extração de pedra ou metal em depósito compatível;
4. Porto — infraestrutura costeira e conexão marítima;
5. Curral — comida e suporte à cavalaria;
6. Acampamento — recrutamento de unidades terrestres;
7. Biblioteca — velocidade de pesquisa;
8. Banco — economia monetária da Era Industrial.

Aeroporto não está aprovado na lista atual. A equipe deve registrar a dependência de infraestrutura aérea ao detalhar as unidades modernas, sem adicionar a construção silenciosamente. Da mesma forma, o Porto está na lista aprovada, mas seu efeito exato no MVP precisa ser especificado antes de implementar conexões marítimas.

### 4.4 Economia

Recursos por fase:

- Era Antiga: comida e pedra;
- Era Medieval: comida, pedra e metal;
- Era Industrial: dinheiro e petróleo passam a fazer parte da economia, mantendo os recursos anteriores;
- Era Moderna: todos os recursos anteriores permanecem utilizáveis.

Depósitos podem estar cadastrados no mapa desde o início, mas metal só entra na economia utilizável após o avanço tecnológico medieval. Dinheiro e petróleo são recursos de late game.

Regras:

- estoques ficam associados a províncias;
- Celeiro amplia a capacidade local de comida;
- cada província tem demanda alimentar fixa;
- tropas consomem comida periodicamente;
- não há simulação de população individual;
- não há deterioração automática;
- produção é calculada por ciclos, inicialmente horários;
- transferências automáticas da lente logística movimentam comida, não pedra ou metal.

### 4.5 Mercado global

A interface de mercado deve permitir publicar pedidos de compra e venda e negociar com os demais jogadores.

- Antes de desbloquear dinheiro, as operações utilizam escambo entre recursos.
- A taxa do mercado é de 15% enquanto a moeda não estiver disponível.
- A taxa é liquidada em bens no escambo, não exige uma moeda inexistente.
- A regra exata de contabilização deve estar centralizada e configurável no servidor; na configuração inicial de teste, a taxa é deduzida da quantidade de mercadoria oferecida.
- Depois do desbloqueio da moeda, a cobrança monetária e as demais regras avançadas serão definidas junto com a economia Industrial.
- A troca do MVP é resolvida pelo mercado; não há navio ou comboio transportando fisicamente o bem.

Acordo/aliança comercial faz parte da diplomacia simplificada. Rotas de comércio físico por estrada ou embarcação e saque de comboios ficam para versões futuras.

### 4.6 Tecnologia e eras

- Progressão individual por jogador.
- Árvore dividida em quatro eras na versão completa.
- Tecnologias possuem pré-requisitos e desbloqueiam construções, unidades ou bônus.
- Uma pesquisa ativa por jogador.
- Biblioteca aumenta a velocidade geral da pesquisa.
- A primeira construção concluída de uma categoria concede um marco único de 50% de bônus no progresso/tempo de uma pesquisa relacionada.
- O marco não pode ser repetido construindo novamente a mesma categoria.
- Tecnologias podem conceder bônus globais, substituindo melhorias individuais de edifícios.
- Metalurgia é Medieval; tropas antigas não exigem metal.

### 4.7 Unidades, recrutamento e movimento

Categorias:

- corpo a corpo;
- ranged;
- cavalaria;
- cerco.

Cada tropa é uma unidade militar completa. Recrutamento ocorre em Acampamentos; cada unidade possui custo e tempo. Unidades antigas não exigem metal; unidades medievais mais avançadas podem exigir.

Os exércitos são grupos. O servidor calcula rota e tempo. A velocidade do grupo parte da unidade mais lenta e recebe modificadores de terreno, estrada e tecnologia. O movimento continua offline.

**Limite de temporizador:** construções e treinamento de tropas devem levar no máximo 24 horas por ordem. Esse teto não deve ser aplicado automaticamente a pesquisa ou deslocamento.

### 4.8 Combate

Combate automático, resolvido no servidor em rodadas. Reforços podem entrar em uma batalha em andamento conforme regras de chegada.

Fatores:

- composição e quantidade;
- ataque e defesa;
- vantagens entre categorias;
- terreno;
- fortificações;
- tecnologia;
- abastecimento.

O cliente exibe resultados e relatórios; não calcula o vencedor.

### 4.9 Estradas e abastecimento

Estradas são construídas nas conexões entre províncias. Tropas atravessam conexões naturais sem estrada, mas estradas aumentam velocidade e capacidade/velocidade do abastecimento. Elas podem ser danificadas ou destruídas e precisam de reparo/reconstrução.

No MVP, apenas comida é transferida automaticamente pela rede logística.

O sistema deve:

1. identificar excedentes e déficits de comida;
2. calcular rotas válidas entre fontes e destinos;
3. considerar capacidade e velocidade das estradas;
4. permitir prioridades configuráveis;
5. permitir mais de uma fonte abastecedora para um destino;
6. recalcular e usar alternativas quando uma rota for interrompida;
7. considerar províncias próprias e, quando houver tratado apropriado, trânsito autorizado por províncias aliadas.

A lente de abastecimento mostra setas de fluxo, fontes, destinos, excedentes, déficits, prioridades e rotas comprometidas. O jogador configura prioridades; o servidor calcula os caminhos.

A demanda de comida da população é uma quantidade fixa por província. Tropas têm consumo periódico separado. Reservas locais e Celeiros influenciam a resistência durante cercos.

### 4.10 Pilhagem, sabotagem e cerco

Pilhagem pode roubar estoques e danificar ou destruir estradas/conexões logísticas. Ações específicas poderão danificar construções ou reduzir temporariamente produtividade. Limites, custos e tempos são parâmetros configuráveis.

Uma província/cidade pode entrar em cerco quando sua rede de abastecimento é efetivamente interrompida. A existência de uma rota alternativa impede que a destruição de uma única estrada seja suficiente para isolar a cidade.

Reservas locais são consumidas. A falta de comida reduz gradualmente a resistência da guarnição e da população. A rendição exige condições e tempo; não é instantânea. O defensor pode enviar reforços, romper o cerco, reabrir rotas ou negociar.

### 4.11 Diplomacia e vitória

Tratados do MVP:

- paz;
- pacto de não agressão;
- aliança;
- acesso militar;
- acordo/aliança comercial.

**Dominação:** controlar 60% de todas as províncias do mapa, incluindo neutras. O cálculo é por quantidade de províncias; frações são arredondadas para cima.

**Vitória diplomática:** todos os jogadores não eliminados precisam integrar e aceitar formalmente a mesma coalizão; ela deve permanecer estável por um período configurável. Os membros elegíveis vencem juntos. Jogador offline, mas ainda não eliminado, continua sendo participante para essa regra.

**Eliminação:** ocorre quando o jogador perde todas as províncias. Perder a capital não elimina. Eliminados podem assistir à partida.

### 4.12 Tempo e persistência

- alvo de duração: 7–14 dias;
- atualizações/eventos internos em intervalos de até um minuto, conforme sistema;
- produção econômica inicialmente horária;
- construções e recrutamento com timers reais de até 24 horas;
- servidor processa o estado mesmo sem clientes conectados;
- armazenar timestamps e último processamento;
- usar idempotência para impedir execução duplicada;
- reinício do servidor não pode apagar nem duplicar o estado.

---

## 5. Arquitetura lógica recomendada

A tecnologia específica será escolhida em uma etapa própria. Independentemente da stack, separar responsabilidades ajuda a manter o projeto testável.

### 5.1 Módulos de domínio sugeridos

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

### 5.2 Modelo orientado a dados

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

### 5.3 Tempo, eventos e idempotência

Evitar depender de um processo contínuo por jogador conectado.

- Persistir timestamps de início e conclusão de ações.
- Persistir o último instante de produção/consumo aplicado.
- Ao processar uma entidade, calcular o tempo transcorrido e aplicar somente os efeitos ainda não processados.
- Usar tarefas agendadas para ações que precisam ser resolvidas no horário.
- Registrar eventos críticos de forma transacional sempre que possível.
- Garantir idempotência: executar novamente um evento não pode duplicar produção, cobrança, movimento, combate ou recompensa.

### 5.4 Segurança e autoridade

- Cliente nunca define saldo, propriedade, conclusão ou vencedor.
- Validar toda ordem no servidor: propriedade, recursos, tecnologia, espaço, conexão, tratado e estado da partida.
- Proteger operações simultâneas de estoque para impedir gastos duplicados.
- Registrar histórico de operações econômicas, militares e diplomáticas.
- Não confiar em timers ou relógios enviados pelo navegador.

### 5.5 Comunicação

A interface deve receber atualizações do servidor sem recarregar a página. Uma solução de comunicação bidirecional ou baseada em eventos em tempo real será escolhida na etapa de arquitetura técnica.

---

## 6. Sequência recomendada de desenvolvimento

As fases abaixo representam entregas verificáveis. A equipe pode trabalhar em paralelo quando as dependências estiverem claras, mas deve manter os critérios de saída.

### Fase 0 — Especificação e mapa de teste

**Entregas:**
- mapa de teste com 8–12 províncias;
- cidades, terrenos, depósitos e adjacências;
- modelo inicial de província e nação;
- protótipo visual para selecionar uma província;
- conjunto inicial de dados configuráveis.

**Saída:** equipe valida como o mapa é representado e como conexões são consultadas.

### Fase 1 — Mundo persistente e multiplayer

**Entregas:**
- criação e entrada em partida;
- dois jogadores de teste;
- nações e propriedade inicial;
- servidor autoritativo;
- armazenamento persistente;
- sincronização e registro de eventos.

**Saída:** dois clientes veem o mesmo mundo e mudanças persistem após desconexão/reinício.

### Fase 2 — Economia, armazenamento e construções

**Entregas:**
- comida, pedra e depósitos de metal;
- produção horária e consumo;
- estoque local por província;
- demanda alimentar fixa;
- Celeiro e capacidade local;
- espaços 3/4 e validação de terreno/depósito;
- construção temporizada;
- Fazenda, Celeiro, Mina, Curral, Acampamento e Biblioteca.

**Saída:** uma província pode ser desenvolvida e seu saldo permanece correto offline. Nenhum edifício começa instalado.

### Fase 3 — Pesquisa e progressão tecnológica

**Entregas:**
- árvore do recorte Antigo/Medieval para o protótipo;
- uma pesquisa ativa por jogador;
- desbloqueios;
- Biblioteca acelerando pesquisa;
- Metalurgia na Era Medieval;
- marcos únicos de 50% associados a construções;
- persistência de conclusão offline.

**Saída:** jogadores progridem individualmente, sem repetir bônus de marco.

### Fase 4 — Exércitos, movimentação e combate

**Entregas:**
- unidades de corpo a corpo, ranged, cavalaria e cerco, gradualmente;
- Acampamento e recrutamento;
- duas infantarias básicas por província inicial;
- exércitos compostos;
- movimento pelo grafo e tempos reais;
- combate automático em rodadas;
- chegada de reforços e relatórios.

**Saída:** uma guerra básica funciona sem controle tático individual.

### Fase 5 — Estradas, abastecimento e cercos

**Entregas:**
- estradas em conexões;
- velocidade e capacidade de estrada;
- distribuição automática de comida;
- cálculo de excedentes, déficits e rotas alternativas;
- prioridades de abastecimento;
- lente de rotas com setas e estados;
- dano, destruição e reparo de estradas;
- pilhagem;
- reservas, isolamento, desgaste e rendição.

**Saída:** é possível interromper o abastecimento, visualizar por quê, criar alternativas e vencer uma disputa por logística.

### Fase 6 — Diplomacia, mercado e condições de vitória

**Entregas:**
- paz, pacto de não agressão, aliança, acesso militar e acordo comercial;
- mercado global de compra/venda;
- escambo com taxa de 15% antes do desbloqueio da moeda;
- pedidos e aceite de negociações;
- vitória por 60% das províncias;
- vitória diplomática compartilhada;
- eliminação e modo espectador.

**Saída:** uma partida pode terminar por dominação ou coalizão diplomática estável.

### Fase 7 — Conteúdo completo das Américas e quatro eras

**Entregas:**
- mapa definitivo com 50–60 províncias;
- fronteiras, cidades, terrenos, depósitos e conexões validados;
- nações predefinidas e posições iniciais equilibradas;
- conteúdo reduzido das quatro eras;
- Banco, dinheiro e petróleo na Era Industrial;
- conteúdo necessário da Era Moderna, após definição da infraestrutura aérea;
- Porto e conexões marítimas conforme sua função aprovada;
- configuração para 3–6 jogadores.

**Saída:** o jogo contém todo o cenário e a progressão previstos para a primeira versão completa.

### Fase 8 — Balanceamento e teste fechado

**Entregas:**
- custos, produção, consumo, velocidades, durações e limiares ajustados;
- partidas completas de 7–14 dias;
- tutorial/ajuda mínima;
- relatórios e tratamento de desconexão;
- testes de estabilidade, exploração e abuso;
- revisão de documentação e implantação.

**Saída:** grupo de 3–6 jogadores consegue iniciar, jogar e concluir uma partida persistente sem intervenção manual da equipe.

---

## 7. Critérios de aceitação por sistema

### 7.1 Mapa e nações

- O cliente seleciona uma província e exibe nome, cidade principal, dono, terreno, depósitos, espaços, construções, estoque e conexões.
- Fronteiras e adjacências são idênticas para todos os jogadores.
- Movimento para uma conexão inexistente é rejeitado.
- Nenhuma província começa com edifícios ou estradas.
- Uma nação começa com capital e duas províncias próximas.
- Cada província inicial contém duas infantarias básicas.
- O total e a força inicial são iguais entre nações.

### 7.2 Economia e construções

- Produção/consumo são calculados com jogador offline.
- Estoques são associados a províncias.
- Demanda alimentar fixa por província é aplicada corretamente.
- Celeiro aumenta capacidade local de comida.
- O servidor impede gastos acima do estoque.
- O servidor valida terreno, depósito, tecnologia e espaço.
- Construções comuns ocupam um dos três espaços; capitais possuem quatro.
- Não existem níveis de edifício no MVP.
- Uma construção não termina antes ou depois do prazo persistido por divergência do relógio do cliente.

### 7.3 Mercado

- Jogadores conseguem publicar pedidos/ofertas de compra e venda.
- Escambo funciona sem recurso monetário.
- A taxa de 15% é aplicada de forma consistente e exibida antes da confirmação.
- A cobrança em bens é registrada e não pode ocorrer duas vezes.
- Nenhuma mercadoria exige navio ou comboio no MVP.

### 7.4 Tecnologia

- O jogador conhece os pré-requisitos e efeitos de cada pesquisa.
- Apenas uma pesquisa fica ativa por jogador.
- Biblioteca altera a velocidade conforme regra configurada.
- Primeira construção de uma categoria concede o marco uma única vez.
- O marco concede 50% de bônus na pesquisa associada.
- Metalurgia não aparece antes da Era Medieval.
- Jogadores podem estar em eras diferentes.

### 7.5 Movimento e combate

- Exército não atravessa província sem conexão válida.
- A velocidade considera a unidade mais lenta e os modificadores aprovados.
- Movimento continua offline.
- Combate é calculado no servidor em rodadas.
- Reforços podem chegar e entrar conforme regra definida.
- O relatório explica perdas e modificadores relevantes.

### 7.6 Logística e cerco

- O sistema identifica excedente e déficit alimentar.
- O jogador configura prioridades.
- A lente mostra setas, fontes, destinos e rotas interrompidas.
- Mais de uma fonte pode abastecer um destino.
- O sistema encontra rota alternativa quando possível.
- Estrada aumenta velocidade/capacidade e pode ser danificada, destruída e reparada.
- Danificar uma estrada não isola automaticamente uma cidade se existir rota alternativa válida.
- Reservas locais e Celeiros influenciam a resistência.
- Rendição exige condições e tempo configuráveis.

### 7.7 Diplomacia e vitória

- Tratados exigem proposta/aceite conforme tipo.
- Estados, duração e histórico dos tratados são visíveis.
- Mercado e negociação funcionam com taxa de 15% no escambo.
- Dominação exige 60% de todas as províncias, com arredondamento para cima.
- Vitória diplomática exige todos os jogadores não eliminados na mesma coalizão, aceite formal e estabilidade pelo período configurado.
- Perder a capital não elimina.
- Perder todas as províncias elimina e habilita espectador.

### 7.8 Persistência

- Desconectar e reconectar não duplica nem perde recursos.
- Reiniciar o servidor não apaga a partida.
- Construções, pesquisas e movimentos continuam offline.
- Um evento processado novamente não duplica seus efeitos.
- O estado final de batalhas e negociações é consistente entre clientes.

---

## 8. Estratégia de testes

### 8.1 Testes funcionais

- **Mapa:** geometrias, fronteiras, adjacência, terrenos, depósitos e acesso costeiro.
- **Nações:** capital, duas províncias próximas, estoques e seis unidades iniciais.
- **Economia:** produção, demanda fixa, consumo militar, capacidade, custos e concorrência.
- **Construções:** espaços 3/4, terreno, depósito, tecnologia, conclusão e ausência de níveis.
- **Tecnologia:** pré-requisitos, eras individuais, Biblioteca e marcos únicos.
- **Mercado:** pedidos concorrentes, escambo, taxa, cancelamento, aceite e duplicidade.
- **Movimento:** rotas, velocidade, chegada offline e mudança de propriedade.
- **Combate:** composição, modificadores, rodadas, reforços e relatório.
- **Logística:** fornecedores, prioridades, capacidade, rotas alternativas e interrupções.
- **Cerco:** reservas, desgaste, contrajogo, rendição e reabertura de rota.
- **Diplomacia:** proposta, aceite, expiração, dissolução e vitória compartilhada.
- **Persistência:** queda/reinício do servidor, reconexão, idempotência e concorrência.

### 8.2 Testes de abuso

- tentar construir sem tecnologia, terreno ou espaço;
- enviar ordens simultâneas que gastem o mesmo estoque;
- recrutar unidade antiga exigindo metal indevidamente;
- aplicar o mesmo marco tecnológico mais de uma vez;
- publicar ordens de mercado com estoque insuficiente;
- explorar taxas ou cancelar ordens para duplicar bens;
- tentar movimentar exército por conexão inexistente;
- danificar repetidamente estrada para contornar custos/limites;
- interromper rota durante deslocamento ou cerco;
- tentar render uma cidade enquanto ainda existe abastecimento válido;
- formar/dissolver coalizões repetidamente para explorar a vitória diplomática;
- desconectar durante batalha, cerco ou conclusão de construção;
- abrir múltiplas abas e enviar ordens concorrentes.

---

## 9. Parâmetros que devem permanecer configuráveis

Não fixar diretamente no código:

- produção por recurso, terreno e edifício;
- demanda alimentar por província;
- capacidade de armazenamento e bônus de Celeiro;
- estoques iniciais por nação;
- custo e duração de construção;
- custo, consumo e duração de treinamento de unidade;
- limite máximo de 24 horas para construção e treinamento;
- velocidade por unidade, terreno e estrada;
- capacidade e condição de estradas;
- prioridade, capacidade e velocidade das rotas alimentares;
- taxa do mercado (inicialmente 15%) e forma de cobrança;
- tempo de pesquisa e bônus de marcos;
- atributos e fórmulas de combate;
- reservas, desgaste e limiar de capitulação;
- percentual de dominação (60% aprovado);
- período de estabilidade diplomática;
- ciclos de produção e atualização;
- duração e ritmo de partida (alvo de 7–14 dias).

Os parâmetros que já foram aprovados como regra — por exemplo, 60% de dominação, 15% de taxa, três/quatro espaços e limite de 24 horas para construções/treinamento — devem ter esses valores como padrão. Configurável não significa que a equipe pode alterá-los sem decisão de design.

---

## 10. Riscos e medidas preventivas

| Risco | Consequência | Medida preventiva |
|---|---|---|
| Escopo excessivo | A versão jogável não termina. | Separar protótipo técnico e MVP completo; limitar conteúdo por era. |
| Mapa desbalanceado | Algumas nações começam superiores. | Preservar fronteiras e ajustar recursos, estoques e parâmetros iniciais. |
| Fronteiras pouco fiéis | O mapa perde a identidade geográfica desejada. | Validar cartografia antes do balanceamento final. |
| Economia sem moeda nas eras iniciais | Comércio fica bloqueado. | Escambo global com taxa em bens. |
| Logística opaca | Jogador não entende por que uma cidade está sem comida. | Lente de rotas, setas, prioridades e relatório de interrupção. |
| Rotas frágeis demais | Uma única sabotagem decide uma guerra. | Rotas alternativas, mais de um fornecedor e reservas. |
| Cercos sem contrajogo | Jogador perde sem oportunidade de reagir. | Reservas, reforços, rompimento de cerco e rendição gradual. |
| Simulação temporal inconsistente | Recursos/ordens duplicados ou perdidos. | Timestamps, idempotência, eventos e testes de reinício. |
| Comércio físico no MVP | Complexidade de transporte, navios e saque atrasa entrega. | Manter mercado global simplificado; comboios são expansão futura. |
| Edifícios com muitos níveis | Microgerenciamento e excesso de entidades. | Não implementar níveis; usar expansão e bônus tecnológicos globais. |
| Vitória diplomática explorável | Aliança momentânea encerra a partida. | Aceite formal e período de estabilidade configurável. |

---

## 11. Entregáveis de documentação e trabalho em equipe

Manter no repositório, no mínimo:

- `01_CONTEXTO_DO_PROJETO.md` — visão, mecânicas e regras de referência;
- `02_DIRECAO_DE_DESENVOLVIMENTO.md` — escopo, fases, critérios de aceitação e testes;
- `README.md` — como configurar, executar e testar o projeto;
- `docs/MAPA_E_DADOS.md` — cartografia, províncias, nações, depósitos e adjacências;
- `docs/ECONOMIA_E_CONSTRUCOES.md` — recursos, produção, armazenamento, espaços e edifícios;
- `docs/TECNOLOGIAS_E_ERAS.md` — árvore tecnológica, desbloqueios e marcos;
- `docs/MILITAR_E_COMBATE.md` — unidades, recrutamento, movimento e combate;
- `docs/LOGISTICA_E_CERCOS.md` — estradas, lente, abastecimento, pilhagem e rendição;
- `docs/DIPLOMACIA_MERCADO_E_VITORIA.md` — tratados, mercado, taxas e condições de vitória.

Toda mudança que altere regra confirmada deve atualizar primeiro o contexto e, em seguida, o documento específico do sistema.

---

## 12. Decisões pendentes para detalhamento

As regras estruturais estão consolidadas. As pendências abaixo são produção de conteúdo, parâmetros ou decisões de implementação, não motivos para reabrir os pilares já aprovados:

1. nome e identidade visual;
2. lista definitiva das nações jogáveis, capitais e províncias iniciais;
3. cartografia final das 50–60 províncias, geometrias e fronteiras;
4. documentação das fontes geográficas usadas;
5. depósitos e produtividade de recursos por província;
6. custos e duração de construções, pesquisas e unidades;
7. árvore tecnológica detalhada e lista de conteúdo por era;
8. atributos de unidades, fórmulas de combate e duração das rodadas;
9. parâmetros de estradas, transferência alimentar, prioridades e capacidade;
10. origem e destino provincial das mercadorias negociadas no mercado global;
11. função específica do Porto no MVP;
12. contabilização exata da taxa do mercado depois do desbloqueio da moeda;
13. período necessário para vitória diplomática;
14. condições numéricas de cerco, desgaste e rendição;
15. infraestrutura necessária às unidades aéreas da Era Moderna;
16. hospedagem e detalhes de bibliotecas complementares da stack aprovada.

Sempre que a implementação exigir um desses valores, usar parâmetro configurável e identificá-lo como **provisório para teste**. Não alterar uma decisão aprovada por conveniência técnica sem registrar a proposta e obter validação de design.

---

## 13. Orientação final à equipe

A ordem de prioridade é:

1. provar o mundo persistente e autoritativo;
2. validar economia, construções e pesquisa;
3. validar exércitos, movimento e combate;
4. validar abastecimento, estradas e cercos;
5. validar mercado, diplomacia e vitória;
6. expandir para o mapa completo das Américas e o conteúdo reduzido das quatro eras;
7. balancear partidas completas de 7–14 dias.

**Direção central:** construir primeiro um mundo que funcione e persista; depois, a interface pela qual os jogadores o governam. A logística deve afetar de verdade o resultado das guerras, e a diplomacia deve ser uma condição de vitória viável — não apenas um chat ou uma lista de alianças.
