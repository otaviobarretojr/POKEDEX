# POKEDEX

Aplicativo Android pessoal para organizar jornadas Pokémon, acompanhar a coleção por jogo/região e consultar a National Dex com formas, variantes e Shiny.

## Estado atual — v20.14.0

A **v20.14.0 — Trainer Today** substitui integralmente o experimento de Companion 3D por uma Home de produto: simples, útil e baseada em progresso real.

### Navegação principal

1. Início
2. Jogos
3. Pokédex
4. Coleção
5. Box
6. Config.

**Início** agora é a Central do Treinador. **Jogos** concentra as Jornadas completas e continua sendo a biblioteca de títulos.

### Trainer Today

A Home reúne:

- saudação, data e sequência diária de uso;
- card principal **Continuar Jornada**, com jogo ativo, próximo objetivo e progresso;
- **Pokémon do dia** com acesso direto aos detalhes;
- **Missão diária** baseada em registros ou avanço real da Jornada;
- resumo de registros e objetivos concluídos no dia;
- total atual da coleção;
- capturas e Pokémon vistos recentemente;
- próximos objetivos da Jornada ativa.

As métricas do dia usam baselines locais e não inventam progresso.

### Garantias funcionais

- National Dex #0001–#1025.
- Normal, Shiny, formas regionais, especiais, cosméticas, gênero e formas de batalha.
- Coleção separada em Living Dex, Shiny Dex e Form Dex.
- Box fixa em **6 colunas × 5 linhas = 30 Pokémon por Box**.
- Swipe entre Boxes, retorno para a mesma Box e persistência por jogo/região.
- Registro Normal/Shiny e variantes sem reordenar automaticamente a Box.
- Jornada com progresso, objetivo atual, próximo objetivo, times e conteúdo por jogo.
- Pokémon Champions não faz parte do catálogo nem da arquitetura.
- Backup schema 6 com checksum SHA-256, rollback e reparo de integridade.
- Contexto ativo, região, Box atual, inicial da Jornada e versão do jogo persistidos.

### Limpeza da arquitetura

A linha de Companion 3D foi encerrada. SceneView, modelos Pikachu, cenários 3D, scripts de download/conversão e documentação temporária do renderer foram removidos do projeto. A Home não depende mais de Filament nem de assets 3D.

### Conteúdo offline

Na inicialização o aplicativo:

1. valida a biblioteca e os pacotes pelo manifesto/versão/SHA-256;
2. atualiza somente pacotes alterados;
3. audita a biblioteca local antes de aceitar fallback offline;
4. verifica o inventário completo de artworks;
5. identifica artes ausentes ou modificadas;
6. mantém Normal, Shiny, formas, capas e visuais da Jornada em armazenamento durável;
7. abre usando primeiro os arquivos locais.

O sistema de artwork acompanha revisões do diretório de sprites do PokeAPI e possui guardas de CI para impedir que telas principais voltem a depender diretamente de imagens remotas.

### Performance

- cache de imagem Coil com memória e disco;
- biblioteca offline separada do cache descartável;
- preload prioritário da Jornada, contexto ativo, Box atual, detalhes próximos e coleção;
- prefetch limitado para evitar carregar a National Dex inteira desnecessariamente em memória;
- downloads concorrentes com limites definidos;
- preparo de áudio assíncrono.

### Estabilidade

A v20.14.0 adiciona:

- Home Trainer Today sem renderer 3D;
- acesso direto da Home à Jornada ativa;
- métricas diárias persistentes e baseadas em alterações reais;
- missão diária simples sem sistema de moeda paralelo;
- remoção da dependência SceneView e dos assets 3D experimentais;
- preservação do boot, áudio, conteúdo offline e assinatura estável de atualização.

### Qualidade

O workflow **Android Build** valida:

- versão e identidade do pacote;
- arquitetura e contratos de regressão;
- hardening funcional;
- acessibilidade;
- prontidão de dispositivo;
- artwork e visuais da Jornada;
- National Dex e National Forms;
- formas problemáticas;
- testes unitários;
- Android Lint;
- compilação dos testes instrumentados;
- build do APK atualizável.

A assinatura estável existente é preservada para manter compatibilidade de atualização com as instalações anteriores.
