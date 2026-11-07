# CONTEXTO DO PROJETO — GRAND STRATEGY RTS DE NAVEGADOR

**Versão:** 2.0  
**Última atualização:** 7 de novembro de 2026  
**Status:** Regras fundamentais consolidadas; parâmetros de balanceamento e conteúdo detalhado ainda serão definidos.  
**Finalidade:** Ser a referência comum para todos os membros da equipe — pessoas e ferramentas de desenvolvimento — sobre a visão, as regras e o escopo do jogo.

> O projeto ainda não possui nome oficial. “Grand Strategy RTS” é uma identificação provisória, não o título definitivo.

---

## 1. Visão geral

O projeto é um jogo de estratégia multiplayer para navegador que combina elementos de **Civilization** e **Supremacy**. Os jogadores comandam civilizações em um mundo persistente, dividido em províncias geográficas com cidades, terrenos, fronteiras e potenciais de recursos previamente definidos.

A experiência é organizada em quatro pilares:

1. **Desenvolvimento:** construir infraestrutura, especializar províncias, administrar recursos e pesquisar tecnologias.
2. **Guerra:** recrutar e movimentar exércitos em tempo real, conquistar territórios, proteger ou interromper linhas de abastecimento e conduzir cercos.
3. **Diplomacia:** negociar, estabelecer tratados e alianças, declarar guerras e buscar uma vitória diplomática compartilhada.
4. **Logística:** administrar o fluxo de alimentos, conectar territórios, criar rotas alternativas e sustentar exércitos e cidades.

A partida continua evoluindo quando o jogador está desconectado. Produção, construções, pesquisas, deslocamentos, abastecimento e outras ordens são calculados pelo servidor com base no tempo transcorrido.

### 1.1 Fantasia central

O jogador deve sentir que está governando uma civilização em um mundo compartilhado: expandir exige planejamento; conquistar exige logística; desenvolver exige escolhas; e vencer exige mais do que acumular tropas.

**Princípio central: não basta conquistar o mundo; é preciso conseguir governá-lo.**

### 1.2 Referências de inspiração

- **Civilization:** desenvolvimento civilizacional, especialização, pesquisa tecnológica, progressão histórica e decisões de longo prazo.
- **Supremacy:** mapa estratégico persistente, ordens em tempo real, deslocamento militar, controle territorial e diplomacia multiplayer.

As referências orientam o gênero e os sistemas. O objetivo é criar uma identidade própria, sem reproduzir interfaces, conteúdo protegido ou regras exatas desses jogos.

---

## 2. Cenário e mapa

### 2.1 Primeiro cenário: Américas

O primeiro cenário jogável será o continente americano, abrangendo:

- América do Norte;
- América Central;
- América do Sul.

O mapa inicial deverá conter aproximadamente **50–60 províncias**. A quantidade final será definida durante a produção cartográfica e os testes de jogabilidade.

A geografia-base será fixa durante a partida. A propriedade política, as construções, as estradas, os exércitos e as rotas poderão mudar, mas as fronteiras e conexões geográficas do cenário não serão geradas aleatoriamente.

### 2.2 Prioridade de fidelidade geográfica

A fidelidade das fronteiras é mais importante do que reproduzir perfeitamente a distribuição de recursos naturais.

Diretrizes:

- As províncias devem seguir fronteiras administrativas, políticas ou geográficas reconhecíveis do mundo real sempre que possível.
- Não criar fronteiras fictícias apenas para equilibrar o jogo.
- A cartografia deve preservar a identidade e a localização das regiões representadas.
- Os depósitos e a abundância de recursos devem partir de referências geográficas e análises do mundo real.
- A distribuição, a quantidade ou a produtividade dos recursos poderá ser ajustada quando necessário para balanceamento, sem distorcer as fronteiras estabelecidas.

Em caso de conflito entre equilíbrio econômico perfeito e fidelidade geográfica, **preservar as fronteiras reais e ajustar os recursos ou as condições iniciais**.

### 2.3 Províncias, cidades e capitais

Uma **província** é uma unidade territorial controlável que representa uma cidade-estado ou região geográfica do mapa. Ela possui uma cidade principal, que funciona como seu centro administrativo e referência geográfica.

Exemplo conceitual:

| Propriedade | Exemplo |
|---|---|
| Província | Piauí |
| Cidade principal | Teresina |
| Terreno predominante | Cerrado |
| Recursos naturais | Pedra e metal (exemplo ilustrativo) |
| Conexões terrestres | Províncias vizinhas |
| Acesso marítimo | Costa do Atlântico; depende de infraestrutura para uso logístico ou comercial marítimo |

Termos utilizados:

- **Província:** unidade territorial controlável, desenvolvível e conquistável.
- **Cidade principal:** cidade real ou referência urbana associada à província.
- **Capital:** província/cidade designada como centro político de uma nação na partida.
- **Fronteira terrestre:** limite compartilhado que permite conexão entre duas províncias, conforme as regras do mapa.
- **Acesso costeiro:** atributo geográfico que indica contato com o mar; não significa que exista um porto construído.
- **Construção:** infraestrutura instalada pelo jogador dentro de um espaço de construção da província.

O mapa deverá ser representado também como um grafo: províncias são nós; fronteiras terrestres navegáveis são arestas. O acesso ao mar é um atributo geográfico e as conexões marítimas dependem de regras e infraestrutura próprias. Uma costa não é, por si só, uma conexão marítima utilizável.

### 2.4 Nações e posições iniciais

Os jogadores escolhem uma entre as nações jogáveis predefinidas das Américas.

Cada nação começa com:

- uma capital;
- duas províncias próximas à capital;
- três províncias iniciais no total;
- duas unidades de infantaria básica em cada província inicial, totalizando seis unidades por jogador;
- estoque inicial de recursos;
- potencial econômico inicial equivalente ao dos adversários.

As nações devem começar com a mesma quantidade e força militar. As diferenças geográficas e de recursos naturais não devem produzir uma vantagem inicial injusta. O equilíbrio pode ser alcançado por meio dos estoques iniciais, da seleção das posições e de parâmetros de produção, sem alterar artificialmente as fronteiras reais.

A lista definitiva de nações, capitais e províncias iniciais ainda será cadastrada no conteúdo do mapa.

### 2.5 Territórios neutros

Províncias que não fizerem parte das posições iniciais dos jogadores começam neutras ou, conforme a configuração da partida, sob controle de outro jogador.

Territórios neutros:

- possuem pequenas guarnições defensivas;
- podem ser conquistados;
- não possuem construções instaladas;
- não possuem estradas ou infraestrutura construída previamente;
- não são administrados por uma IA econômica ou diplomática no MVP.

A guarnição neutra é uma defesa inicial, não uma civilização completa controlada por computador.

---

## 3. Terrenos, recursos naturais e especialização territorial

Cada província possui características geográficas predefinidas que determinam oportunidades, restrições e modificadores de produção.

Exemplos de especialização:

- **Planícies férteis:** favorecem fazendas e produção de comida.
- **Pastagens:** favorecem currais e atividades ligadas à criação de animais e cavalaria.
- **Montanhas e regiões com depósitos:** favorecem minas de pedra ou metal.
- **Litoral:** permite construir um porto após cumprir os requisitos tecnológicos.
- **Regiões com petróleo:** guardam um recurso estratégico que só poderá ser explorado na fase tecnológica apropriada.
- **Florestas, desertos e outros terrenos:** podem oferecer modificadores ou restrições específicos.

A especialização resulta da combinação entre terreno, depósito natural, disponibilidade de espaços e construções escolhidas pelo jogador.

### 3.1 Nenhuma infraestrutura predefinida

**Nenhuma província ou cidade começa com construções estratégicas instaladas.**

- Litoral significa elegibilidade para construir um Porto, não um Porto gratuito.
- Depósito mineral significa potencial de extração, não uma Mina pronta.
- Planície fértil significa bônus potencial para uma Fazenda, não uma Fazenda pronta.
- Cidade ou capital é uma referência geográfica e política, não um pacote automático de edifícios.
- Territórios neutros também começam sem construções e sem estradas.

Os depósitos naturais e as condições do terreno são dados do cenário. A construção que os explora é sempre uma decisão do jogador.

---

## 4. Construções e espaços

### 4.1 Espaços disponíveis

- Província comum: **3 espaços de construção**.
- Capital: **4 espaços de construção**.
- Todas as províncias começam com os espaços vazios.
- Cada edifício ocupa um espaço.
- Estradas são infraestrutura das conexões entre províncias e não ocupam espaços de construção.
- Não haverá melhorias ou níveis individuais de edifícios no escopo aprovado.

A ausência de níveis é intencional: evita microgerenciamento excessivo e permite que o crescimento de late game venha de expansão territorial, novas construções em outras províncias e bônus tecnológicos globais.

### 4.2 Lista de construções aprovada

| Construção | Função principal | Regras e observações |
|---|---|---|
| **Fazenda** | Produção de comida. | Produção favorecida por planícies férteis; requer tecnologia agrícola. |
| **Celeiro** | Aumenta a capacidade de armazenamento de comida da província. | Ajuda a manter reservas durante interrupções de abastecimento e cercos. |
| **Mina** | Extrai pedra ou metal. | Exige depósito compatível e tecnologia; o depósito define o recurso extraído. |
| **Porto** | Habilita infraestrutura e um nó portuário para conexões marítimas. | Só pode ser construído em província costeira elegível e após o desbloqueio tecnológico. Nenhuma costa começa com porto. A função específica desse nó no MVP precisa ser detalhada antes da implementação. |
| **Curral** | Produção de alimentos e suporte à criação/manutenção da cavalaria. | Favorecido por pastagens; detalhes de recrutamento e manutenção serão balanceados. |
| **Acampamento** | Permite recrutar, reunir e organizar unidades terrestres. | Requer tecnologia militar adequada. |
| **Biblioteca** | Aumenta a velocidade geral de pesquisa. | Não permite pesquisas simultâneas adicionais; o jogador mantém uma pesquisa ativa por vez. |
| **Banco** | Habilita a economia monetária e operações econômicas avançadas. | Construção da Era Industrial; não participa da economia das eras iniciais. |

Aeroporto não faz parte da lista de construções aprovada. A infraestrutura necessária para unidades aéreas da Era Moderna deverá ser definida no detalhamento dessa era, sem presumir que um Aeroporto já esteja aprovado.

### 4.3 Regra tecnológica para construções

As construções precisam ser desbloqueadas por tecnologias. O servidor deve verificar tecnologia, terreno, depósito, espaço e recursos antes de aceitar uma ordem.

Custos, durações e requisitos específicos serão mantidos como dados configuráveis.

---

## 5. Recursos e economia

### 5.1 Recursos e disponibilidade por era

| Recurso | Disponibilidade | Uso principal |
|---|---|---|
| **Comida** | Era Antiga em diante | Demanda fixa das províncias, manutenção de exércitos e abastecimento. |
| **Pedra** | Era Antiga em diante | Construções, fortificações e infraestrutura. |
| **Metal** | Uso econômico e militar a partir da Era Medieval. Depósitos podem estar definidos no mapa desde o início. | Unidades e construções que dependam de metalurgia. Tropas antigas não exigem metal. |
| **Dinheiro** | Era Industrial em diante. | Mercado monetário, manutenção, administração e economia avançada. |
| **Petróleo** | Era Industrial em diante. | Unidades mecanizadas, aviação e infraestrutura avançada, conforme conteúdo futuro. |

Dinheiro e petróleo são recursos de late game. A economia das eras iniciais deve funcionar sem eles.

### 5.2 Depósitos e produção

Os depósitos naturais e sua abundância são definidos no mapa com base em referências geográficas reais. Para balanceamento, produtividade, quantidade ou distribuição de recursos podem ser ajustadas, preservando a prioridade de fidelidade das fronteiras.

A existência de um depósito não gera produção automaticamente. O jogador precisa desbloquear e construir a infraestrutura compatível.

A produção de uma província considera, conforme o recurso:

- depósito natural e abundância;
- terreno e fertilidade;
- construção instalada;
- tecnologias pesquisadas e bônus globais;
- eventuais modificadores temporários de guerra ou cenário.

Não existem melhorias individuais de nível dos edifícios. Tecnologias podem conceder bônus globais de produção para todas as construções pertinentes.

### 5.3 Armazenamento e consumo

- Recursos ficam armazenados por província.
- Celeiros aumentam a capacidade local de armazenamento de comida.
- Cada província possui uma demanda alimentar fixa, usada para abstrair o consumo da população no MVP.
- Exércitos consomem comida periodicamente.
- Não há simulação individual de habitantes no MVP.
- Recursos não sofrem deterioração ou perda automática por passagem de tempo.
- A comida pode ser movimentada automaticamente pela rede de abastecimento.
- Pedra e metal permanecem armazenados localmente; transferências desses recursos são realizadas por ordens específicas ou pelo mercado, conforme o sistema disponível.

A demanda alimentar fixa por província evita a necessidade de simular população individual e facilita o balanceamento dos cercos.

### 5.4 Mercado global e escambo

O MVP terá uma tela de **mercado global**, acessível aos jogadores da partida. Nela, jogadores podem registrar pedidos/ofertas de compra e venda e negociar recursos com outros participantes.

Na Era Antiga e antes do desbloqueio da moeda, as negociações funcionam por **escambo**: um recurso é trocado diretamente por outro. O jogador publica uma oferta/proposta no mercado global e outro jogador pode aceitá-la. O mercado cobra uma taxa de **15%**, liquidada em bens porque a moeda ainda não está disponível. A regra de contabilização da taxa deve ser explícita na interface e configurável no servidor; a configuração inicial de teste aplica a taxa à quantidade de mercadoria anunciada/oferecida.

Quando a moeda for desbloqueada na Era Industrial, o sistema poderá utilizar dinheiro para liquidar taxas e operações monetárias. A regra econômica detalhada dessa transição será definida junto com o Banco e a árvore Industrial.

No MVP, as trocas são resolvidas pelo mercado/sistema de negociação, sem transporte físico por barco ou comboio. Como os estoques são locais por província, a implementação do mercado deverá definir de qual província sai o recurso anunciado e em qual província o recurso recebido será armazenado. Essa ligação entre mercado global e estoques locais permanece um detalhe de implementação a fechar antes do desenvolvimento do módulo.

### 5.5 Alianças comerciais e transporte físico futuro

A aliança ou tratado comercial poderá evoluir para permitir comércio direto entre jogadores que:

- compartilhem uma fronteira terrestre; ou
- possuam portos e conexões marítimas válidas.

Em uma versão futura, mercadorias poderão percorrer estradas ou embarcações entre capitais/portos. Comboios e embarcações poderão ficar vulneráveis a saque ou interceptação.

**Esse transporte comercial físico e o saque de comboios ficam fora do MVP.** No MVP, o mercado global e as negociações simplificadas atendem à necessidade de comércio.

---

## 6. Tecnologias e evolução histórica

### 6.1 Progressão individual

Cada jogador possui sua própria árvore tecnológica e evolui independentemente dos demais. Jogadores podem estar em eras diferentes durante a mesma partida.

- A árvore é dividida em quatro eras: Antiga, Medieval, Industrial e Moderna.
- Cada tecnologia possui pré-requisitos.
- Tecnologias fundamentais liberam a era seguinte.
- Tecnologias desbloqueiam construções, unidades, regras ou bônus.
- O jogador mantém **uma pesquisa ativa por vez**.
- A Biblioteca aumenta a velocidade geral de pesquisa.

### 6.2 Eras

| Era | Exemplos de conteúdo |
|---|---|
| **Antiga** | Agricultura, escrita, construção básica, infantaria simples, arqueiros e primeiras estruturas. |
| **Medieval** | Metalurgia, engenharia, navegação, fortificações, cavalaria avançada e máquinas de cerco. |
| **Industrial** | Industrialização, Banco, dinheiro, petróleo, ferrovias, artilharia e unidades mecanizadas iniciais. |
| **Moderna** | Eletrônica, mecanização avançada, tanques, aviação e tecnologias contemporâneas previstas pelo jogo. |

A Metalurgia pertence à Era Medieval. Unidades da Era Antiga não exigem metal para recrutamento.

### 6.3 Construções como marcos de conhecimento

Algumas construções também funcionam como marcos tecnológicos. Desbloquear uma construção é diferente de receber o marco:

- **Desbloqueio:** uma tecnologia libera a possibilidade de construir determinado edifício.
- **Marco:** a primeira construção concluída de uma categoria concede, uma única vez, um bônus de **50% do progresso/tempo** de uma tecnologia relacionada.

Exemplos:

| Construção realizada | Pesquisa relacionada beneficiada |
|---|---|
| Primeira Fazenda | Técnicas agrícolas |
| Primeira Mina | Engenharia de mineração |
| Primeiro Porto | Navegação |
| Primeiro Acampamento | Doutrina militar |
| Primeira Biblioteca | Conhecimento ou escrita |

O bônus é pontual, não permanente, e não pode ser acumulado repetindo a construção da mesma categoria. A pesquisa relacionada continua disponível sem o marco, mas leva o tempo integral.

Tecnologias também poderão conceder bônus globais de produção, pesquisa ou eficiência. Esses bônus substituem a necessidade de melhorar individualmente o nível de cada edifício.

---

## 7. Sistema militar

### 7.1 Categorias de unidades

O sistema militar terá quatro categorias principais:

| Categoria | Função geral | Exemplos por era |
|---|---|---|
| **Corpo a corpo** | Linha de frente e defesa. | Lanceiros, espadachins, infantaria. |
| **Ranged** | Ataque à distância. | Arqueiros, besteiros e unidades de fogo. |
| **Cavalaria** | Mobilidade e manobras. | Cavalaria leve e pesada. |
| **Cerco** | Ataque a fortificações e apoio a cercos. | Aríetes, catapultas e artilharia. |

Cada unidade representa uma formação ou unidade militar completa, não um soldado individual.

### 7.2 Força inicial

Cada uma das três províncias iniciais de uma nação começa com duas unidades de infantaria básica. Portanto, cada jogador começa com seis unidades militares. Todos os jogadores recebem a mesma quantidade e força militar inicial.

Unidades especializadas precisam ser recrutadas posteriormente.

### 7.3 Recrutamento

- Acampamentos permitem recrutar unidades terrestres.
- Cada unidade possui custo de recursos e tempo de treinamento.
- Tropas da Era Antiga não exigem metal.
- Unidades medievais mais avançadas podem exigir metal após o desbloqueio de Metalurgia.
- Tecnologias militares liberam novas unidades e podem conceder melhorias globais.
- O tempo máximo previsto para concluir uma construção ou treinar uma unidade é de **24 horas**. Os valores reais serão calibrados.

### 7.4 Exércitos e movimentação

Exércitos são grupos de unidades. O jogador seleciona um exército e emite uma ordem para uma província de destino.

- O servidor valida a rota no grafo do mapa.
- Unidades atravessam conexões terrestres naturais válidas mesmo sem estrada.
- A velocidade do exército é baseada na unidade mais lenta do grupo, com modificadores de terreno, tecnologia e infraestrutura.
- Estradas aumentam a velocidade de deslocamento.
- Ordens continuam sendo executadas com o jogador offline.
- O cliente mostra destino, rota e estimativa de chegada.

### 7.5 Combate

O combate é automático e resolvido pelo servidor em rodadas. Reforços podem chegar enquanto uma batalha está em andamento e participar conforme as regras de chegada e a ordem de batalha.

Fatores considerados:

- composição e quantidade dos exércitos;
- ataque e defesa das unidades;
- vantagens e desvantagens entre categorias;
- terreno;
- fortificações;
- tecnologias;
- situação de abastecimento.

O combate não terá controle tático individual de cada soldado no MVP. O resultado deve gerar relatório com perdas, sobreviventes e modificadores relevantes.

---

## 8. Estradas, abastecimento, pilhagem e cercos

A logística é um dos diferenciais centrais do jogo. Uma força militar não deve depender apenas de sua quantidade: precisa ser alimentada, deslocar-se por rotas e proteger suas conexões.

### 8.1 Estradas e conexões

- Províncias vizinhas possuem conexões naturais definidas pelo mapa.
- Tropas podem atravessar essas conexões mesmo sem estradas construídas.
- Estradas são construídas **sobre as conexões entre províncias**, não dentro de um espaço de edifício.
- Estradas aumentam a velocidade de deslocamento e a capacidade/velocidade de abastecimento.
- Estradas podem ser danificadas ou destruídas por ações de guerra.
- Estradas danificadas podem ser reparadas; estradas destruídas precisam ser reconstruídas.

A estrada é infraestrutura de uma ligação territorial. Não é uma construção que ocupa um dos três ou quatro espaços da província.

### 8.2 Abastecimento alimentar automático

No MVP, a movimentação automática de recursos pela rede de abastecimento será limitada à **comida**.

O sistema:

1. identifica províncias com excedente e províncias com déficit de comida;
2. calcula rotas válidas entre fontes de suprimento e destinos;
3. movimenta comida automaticamente dentro da rede logística do jogador;
4. considera capacidade e velocidade das estradas;
5. procura rotas alternativas quando uma conexão é interrompida;
6. permite que o jogador configure prioridades de abastecimento;
7. permite priorizar cidades, províncias ou frentes militares;
8. permite que uma província receba comida de mais de uma fonte abastecedora.

A rede pode utilizar províncias do próprio jogador. O trânsito por províncias aliadas depende de um tratado de acesso compatível e das permissões definidas pela diplomacia.

### 8.3 Lente de rotas e abastecimento

O mapa terá uma **lente de abastecimento** específica. Ao ativá-la, o jogador visualizará:

- setas indicando de onde a comida sai e para onde é enviada;
- províncias com excedente e déficit;
- rotas utilizadas e sua capacidade;
- conexões interrompidas ou danificadas;
- rotas alternativas disponíveis;
- destinos configurados como prioritários.

A lente também permitirá configurar prioridades e orientar a distribuição de comida. O jogador não precisa desenhar manualmente cada trecho de cada rota: o servidor calcula os caminhos válidos e a interface apresenta o fluxo, enquanto as prioridades determinam quais destinos devem ser atendidos primeiro.

### 8.4 Demanda e reservas

- Cada província possui uma demanda alimentar fixa que abstrai o consumo da população.
- Unidades militares consomem comida periodicamente.
- O estoque é local por província.
- Celeiros aumentam a capacidade de reserva local.
- Não há deterioração automática de recursos.
- A falta de comida reduz a capacidade de sustentar forças e a resistência durante cercos, segundo parâmetros configuráveis.

### 8.5 Pilhagem e sabotagem

Ordens militares de pilhagem podem:

- roubar parte dos estoques disponíveis;
- danificar ou destruir estradas e conexões logísticas;
- danificar construções ou reduzir temporariamente a produtividade local, conforme a ação implementada.

A ação exige presença e condições militares adequadas. Custos, limites de saque, duração de dano e regras de reparo serão configuráveis para evitar exploração abusiva.

### 8.6 Cercos e rendição

Fluxo geral de um cerco:

1. Um exército entra em território inimigo e supera ou afasta a defesa de campo.
2. O atacante pode manter um cerco à cidade/província.
3. O abastecimento inimigo precisa ser efetivamente interrompido; destruir uma única estrada não basta se existir outra rota válida.
4. A cidade consome suas reservas locais de comida.
5. Celeiros aumentam o tempo de resistência ao ampliar as reservas.
6. Quando a comida se torna insuficiente, a resistência da guarnição e da população diminui gradualmente.
7. Ao atingir condições configuráveis, a cidade pode se render ou capitular.

O defensor pode enviar reforços, romper o cerco, reabrir uma rota de suprimento ou negociar. O cerco não causa rendição instantânea: reservas, rotas alternativas e contrajogo devem ser considerados.

---

## 9. Multiplayer, diplomacia e comércio

### 9.1 Estrutura da partida

- Partidas para **3–6 jogadores** na versão completa jogável.
- Cada partida é um mundo persistente e isolado.
- A partida continua avançando quando os jogadores estão offline.
- O servidor é a autoridade sobre propriedade, recursos, construções, ordens, movimento, combate, abastecimento e vitória.

### 9.2 Relações e tratados do MVP

O MVP deverá oferecer relações diplomáticas explícitas, com propostas e aceite dos envolvidos:

- **Paz:** encerra hostilidades.
- **Pacto de não agressão:** impede ataques entre os signatários durante sua validade.
- **Aliança:** estabelece cooperação diplomática e militar.
- **Acesso militar:** permite trânsito por províncias do aliado conforme as condições do tratado.
- **Acordo/aliança comercial:** estabelece uma relação de comércio entre os participantes; no MVP, as operações continuam usando o mercado global e as negociações simplificadas.

Tratados devem possuir estados claros, histórico e regras de expiração/dissolução.

### 9.3 Mercado e comércio

O mercado global de escambo é a solução comercial do MVP. Negociações são realizadas por pedidos/ofertas de compra e venda e sofrem taxa de 15% enquanto a moeda não estiver desbloqueada.

A aliança comercial poderá, em versões futuras, permitir rotas físicas entre jogadores com fronteira terrestre compartilhada ou portos conectados. Essas rotas poderão usar estradas e embarcações e, futuramente, ficar vulneráveis a saques. A simulação de comboios e embarcações comerciais não faz parte do MVP.

### 9.4 Vitória por dominação

Um jogador vence por dominação ao controlar **60% de todas as províncias do mapa da partida**, incluindo províncias neutras e províncias inicialmente ocupadas por outros jogadores.

O cálculo usa a quantidade total de províncias, não uma soma ponderada por valor estratégico. Para um mapa com 60 províncias, são necessárias 36; para 50, são necessárias 30. Quando o total não resultar em número inteiro, o requisito será arredondado para cima.

### 9.5 Vitória diplomática

A vitória diplomática é compartilhada entre os jogadores sobreviventes:

1. todos os jogadores ainda participantes da partida formam uma mesma coalizão;
2. todos aceitam formalmente a aliança;
3. a coalizão permanece estável por um período determinado;
4. todos os integrantes elegíveis vencem juntos.

Para esta regra, “jogador ativo/participante” significa jogador que ainda não foi eliminado da partida — não significa necessariamente que esteja conectado naquele momento. O período de estabilidade será um parâmetro configurável de balanceamento, ainda sem duração definitiva aprovada.

### 9.6 Derrota e espectador

- Perder a capital não elimina automaticamente o jogador.
- O jogador permanece na partida enquanto controlar pelo menos uma província.
- Ao perder todas as províncias, é eliminado.
- Jogadores eliminados podem acompanhar a partida em modo espectador.

---

## 10. Ritmo, duração e tempo do mundo

- Duração-alvo de uma partida: **7–14 dias**.
- O mundo permanece ativo quando os jogadores estão desconectados.
- A simulação é processada pelo servidor.
- Eventos e atualizações internas devem ser processados em intervalos de até um minuto, conforme a necessidade de cada sistema.
- A produção econômica ocorrerá inicialmente em ciclos horários.
- Construções e recrutamento de tropas usam temporizadores de tempo real.
- **Construir edifícios e treinar tropas não deve levar mais de 24 horas por ordem.**
- Os tempos de pesquisa e deslocamento serão calibrados para a duração pretendida da partida; o limite de 24 horas definido para construções e recrutamento não deve ser aplicado automaticamente a esses outros sistemas.

Os horários de início e conclusão devem ser persistidos. O navegador não é responsável por manter o mundo ativo.

---

## 11. Diretrizes técnicas de alto nível

Estas são decisões arquiteturais já aprovadas, não uma escolha de linguagem ou framework.

### 11.1 Servidor autoritativo

O navegador envia ordens e apresenta o estado do mundo. O servidor valida permissões, custos, pré-requisitos, tempo, movimento, combate, abastecimento e resultados. O cliente nunca deve decidir o resultado oficial de uma ação.

### 11.2 Mundo persistente e processamento por tempo

Recursos, pesquisas, construções, exércitos, diplomacia e estado territorial continuam existindo mesmo se todos os jogadores estiverem desconectados.

O sistema deve:

- persistir timestamps de início, conclusão e última atualização;
- calcular o tempo transcorrido ao processar entidades;
- executar eventos agendados no servidor;
- impedir duplicação de produção, custos, movimentos, batalhas ou recompensas (idempotência);
- sobreviver a reinícios do servidor sem perder o estado da partida.

### 11.3 Dados separados da interface

Províncias, geometrias, fronteiras, terrenos, depósitos, construções, tecnologias, unidades, rotas e regras devem ser dados estruturados. O desenho do mapa não deve conter a lógica das regras.

### 11.4 Comunicação em tempo real

O cliente deve receber atualizações do servidor sem precisar recarregar a página. A tecnologia de comunicação será escolhida na etapa de arquitetura técnica.

---

## 12. Escopo de produto: protótipo técnico e MVP jogável

O projeto terá dois marcos distintos, para que “MVP” não seja confundido com o primeiro teste de tecnologia.

### 12.1 Protótipo técnico inicial

Objetivo: provar que os sistemas essenciais funcionam juntos antes de produzir todo o mapa e conteúdo.

- mapa reduzido de aproximadamente 8–12 províncias;
- dois jogadores de teste;
- recorte funcional das Eras Antiga e Medieval;
- economia inicial de comida, pedra e metal conforme desbloqueios;
- construções e tecnologias representativas;
- recrutamento de infantaria básica;
- movimentação em tempo real;
- combate e abastecimento simplificados;
- persistência offline e sincronização entre clientes.

O protótipo não é a versão completa destinada a jogadores. É uma validação técnica e de regras.

### 12.2 Primeira versão completa jogável (MVP jogável)

- cenário das Américas;
- aproximadamente 50–60 províncias;
- fronteiras realistas e conexões geográficas fixas;
- 3–6 jogadores;
- nações predefinidas com três províncias iniciais;
- duas unidades de infantaria básica por província inicial;
- todas as construções aprovadas, respeitando a era de desbloqueio;
- economia, mercado global e taxa de escambo;
- lente de abastecimento alimentar;
- estradas, pilhagem, cercos e rendição;
- diplomacia e as duas condições de vitória;
- **quatro eras jogáveis: Antiga, Medieval, Industrial e Moderna**;
- conteúdo tecnológico e militar de cada era reduzido e balanceado para partidas de 7–14 dias.

As quatro eras fazem parte da primeira versão completa jogável. O protótipo técnico valida inicialmente apenas o recorte Antigo/Medieval.

---

## 13. Glossário

| Termo | Definição |
|---|---|
| **Província** | Região geográfica controlável, com uma cidade principal. |
| **Capital** | Província/cidade que funciona como centro político de uma nação na partida. |
| **Depósito natural** | Recurso predefinido no mapa; precisa de infraestrutura e tecnologia para ser explorado. |
| **Espaço de construção** | Vaga dentro de uma província na qual o jogador pode instalar um edifício. |
| **Especialização territorial** | Uso de terreno, depósitos e construções para tornar uma província adequada a determinadas funções. |
| **Marco tecnológico** | Primeira construção de uma categoria que concede, uma única vez, 50% de bônus em pesquisa relacionada. |
| **Estrada** | Infraestrutura construída em uma conexão entre províncias. |
| **Rota de abastecimento** | Caminho pelo qual comida é movimentada para atender uma província ou exército. |
| **Lente de abastecimento** | Visualização do fluxo de comida, excedentes, déficits, rotas e prioridades. |
| **Pilhagem** | Ação militar que saqueia estoques e pode danificar infraestrutura ou construções. |
| **Cerco** | Isolamento militar/logístico que pode levar à rendição após o consumo das reservas e a redução da resistência. |
| **Mercado global** | Sistema de pedidos e negociações de compra/venda entre jogadores. |
| **Hegemonia diplomática** | Vitória compartilhada quando todos os jogadores participantes estão em uma mesma coalizão estável. |

---

## 14. Estado das decisões

### 14.1 Decisões confirmadas

- Jogo multiplayer de navegador, persistente e em tempo real, inspirado em Civilization e Supremacy.
- Primeiro cenário nas Américas, com aproximadamente 50–60 províncias.
- Fidelidade das fronteiras reais tem prioridade sobre realismo absoluto da distribuição de recursos; recursos podem ser ajustados para balanceamento.
- Cada província representa uma região/cidade-estado com cidade principal.
- Nações jogáveis predefinidas; cada uma começa com capital e duas províncias próximas.
- Duas unidades de infantaria básica em cada província inicial; seis unidades por jogador.
- Territórios neutros com guarnições, sem construções ou infraestrutura.
- Nenhuma província começa com construções instaladas.
- Três espaços em província comum e quatro na capital; sem melhorias individuais de nível.
- Construções: Fazenda, Celeiro, Mina, Porto, Curral, Acampamento, Biblioteca e Banco.
- Recursos: comida e pedra na Era Antiga; metal a partir da Medieval; dinheiro e petróleo na Industrial.
- Produção condicionada por depósito, terreno, construção e tecnologia.
- Estoques por província, demanda alimentar fixa por província, consumo militar periódico e sem deterioração automática.
- Mercado global com pedidos de compra e venda; escambo e taxa de 15% antes do desbloqueio da moeda.
- Transporte físico de comércio por estradas/embarcações e saque de comboios ficam para depois do MVP.
- Quatro eras com progressão tecnológica individual; uma pesquisa ativa por jogador.
- Biblioteca aumenta velocidade de pesquisa; primeira construção de uma categoria concede marco único de 50% em pesquisa relacionada.
- Quatro categorias militares: corpo a corpo, ranged, cavalaria e cerco.
- Acampamento para recrutamento; unidades antigas não exigem metal; unidades medievais avançadas podem exigir.
- Exércitos são grupos; movimento em tempo real e velocidade baseada na unidade mais lenta, com modificadores.
- Combate automático em rodadas, permitindo chegada de reforços.
- Estradas nas conexões, não nos espaços de construção; aumentam velocidade e capacidade de abastecimento; podem ser danificadas ou destruídas.
- Abastecimento automático de comida, prioridades configuráveis, rotas alternativas e lente visual com setas.
- Pilhagem, sabotagem, isolamento logístico, reservas e rendição gradual.
- Diplomacia com paz, pacto de não agressão, aliança, acesso militar e acordo comercial.
- Vitória por dominação ao controlar 60% das províncias do mapa.
- Vitória diplomática compartilhada quando todos os jogadores não eliminados integram uma coalizão estável.
- Eliminação ao perder todas as províncias; capital perdida não elimina automaticamente; modo espectador após eliminação.
- Partidas de 7–14 dias; produção horária; eventos processados em intervalos de até um minuto; construções e treinamento limitados a 24 horas por ordem.
- Servidor autoritativo, persistência offline, dados separados da interface e comunicação em tempo real.
- Protótipo técnico Antigo/Medieval; primeira versão completa jogável com as quatro eras.

### 14.2 Parâmetros e conteúdos ainda a definir

As regras fundamentais estão consolidadas, mas vários valores e cadastros precisam ser produzidos durante o desenvolvimento:

- nome oficial e identidade visual;
- lista final das nações jogáveis, capitais e províncias iniciais;
- lista cartográfica completa das 50–60 províncias, cidades, geometrias e fronteiras;
- fontes geográficas utilizadas para validar fronteiras, terrenos e depósitos;
- abundância e produtividade de cada depósito;
- custos, tempos e produção de cada construção;
- capacidade numérica dos celeiros e estoques iniciais;
- árvore detalhada das quatro eras e seus pré-requisitos;
- lista, atributos e custos das unidades por era;
- fórmula de combate, duração das rodadas e regras de chegada de reforços;
- capacidade, custo e tempo de construção/reparo das estradas;
- algoritmo e capacidade das rotas de abastecimento e regras detalhadas de prioridade;
- origem/destino provincial dos bens negociados no mercado global, para conciliar ofertas com estoques locais;
- taxas e contabilização exata do mercado após a introdução da moeda;
- função específica do Porto no MVP, antes da implementação das conexões marítimas;
- duração do período de estabilidade para vitória diplomática;
- limiares de rendição, consumo e desgaste em cercos;
- stack de cliente, servidor, banco de dados e hospedagem.

Quando um valor for necessário antes da decisão definitiva, a equipe deverá usar um parâmetro configurável, marcá-lo como **provisório para teste** e evitar fixá-lo de forma irreversível no código.

---

## 15. Orientações para membros da equipe e ferramentas de IA

1. Leia este documento e `02_DIRECAO_DE_DESENVOLVIMENTO.md` antes de propor mudanças de mecânica ou arquitetura.
2. Preserve as decisões confirmadas. Não transforme ideias futuras em requisitos do MVP.
3. Diferencie **decisão confirmada**, **parâmetro provisório** e **pendência**.
4. Não invente fronteiras fictícias para balancear o mapa; ajuste recursos e condições iniciais antes de alterar a geografia aprovada.
5. Não coloque construções prontas em cidades ou províncias.
6. Respeite três espaços por província comum e quatro por capital; não implemente níveis individuais de edifício.
7. Não exija metal para unidades da Era Antiga.
8. Não introduza dinheiro ou petróleo na economia antes da Era Industrial.
9. Não simule transporte físico de comércio ou saque de comboios no MVP.
10. Mantenha o abastecimento automático limitado à comida no MVP.
11. Não transforme o combate em controle tático individual de soldados.
12. Mantenha produção, custos, movimento, combate, abastecimento e vitória sob autoridade do servidor.
13. Use dados e parâmetros configuráveis para custos, durações, atributos e limiares.
14. Atualize documentação e testes sempre que uma regra confirmada mudar.
