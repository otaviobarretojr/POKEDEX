# POKEDEX

Aplicativo Android pessoal para organizar jornadas Pokémon, acompanhar a coleção por jogo/região e consultar a National Dex com formas, variantes e Shiny.

## Estado atual — v20.13.6

A **v20.13.6 — Textured Companion** consolida a base atual do aplicativo depois do rework de Jornada, Jogos, Pokédex, Coleção, Box, conteúdo offline e artwork. O objetivo desta versão é funcionar como nova baseline estável para as próximas evoluções, preservando compatibilidade com as instalações anteriores.

### Navegação principal

1. Jornada
2. Jogos
3. Pokédex
4. Coleção
5. Box
6. Config.

A Jornada é a Home. Jogos funciona como biblioteca de títulos e não duplica a tela principal da aventura.

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

A v20.13.6 adiciona:

- substituição do modelo provisório por Pikachu 3D rigado e licenciado em CC BY 4.0;
- build-time audit do GLB com validação de rig, animações e texturas;
- animações remapeadas para Idle, Walking, Dance e Jump;
- enquadramento recalibrado sem alterar a orientação interna do rig;
- sombra e balão reposicionados para integrar o companion ao palco fixo;


- orientação frontal do Pikachu 3D com rotação Y de 180°;
- escala do companion aumentada para recuperar protagonismo visual sem voltar a cortar o corpo;
- reposicionamento horizontal e vertical para manter o personagem centralizado acima do painel inferior;


- Home Companion independente de jogo/região, com palco visual fixo do aplicativo;
- remoção da lógica de fundos por Paldea, Lumiose, Galar, Hisui e demais regiões na Home;
- nova calibração do Pikachu 3D para enquadramento completo, com escala menor e posição vertical corrigida;
- HUD lateral e painel de humor/afinidade mais compactos, liberando área visual para o personagem;


- renderer 3D reduzido para um único ModelNode na abertura da Home;
- Pikachu normalizado mantendo `.gltf + .bin` externo, o caminho que já não causava crash no aparelho físico;
- cenário 3D regional desacoplado temporariamente da inicialização para eliminar a regressão nativa da v20.13.2;


- renderer 3D corrigido para o Companion, com Pikachu normalizado e enquadramento independente do `centerOrigin` defeituoso do SceneView 2.3.0;
- modelo do Pikachu empacotado como glTF autocontido para eliminar dependência de resolução externa do `.bin`;
- primeiro cenário regional em geometria 3D real: Paldea / Mesagoza, substituindo o placeholder geométrico 2D da Home;


- preservação do boot concluído em recriações da Activity;
- áudio protegido contra recriação de configuração;
- Busca, Evoluções e Game Dex tratados como telas secundárias;
- fallback offline somente após auditoria real da biblioteca;
- tolerância a novos pacotes remotos ainda desconhecidos pelo cliente;
- versionCode **21300**, superior à linha v20.12.1, preservando o caminho de atualização;
- compilação dos testes instrumentados no CI.

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
