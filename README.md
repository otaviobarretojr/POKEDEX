# POKEDEX

Aplicativo Android pessoal focado exclusivamente em Pokédex, coleção, Boxes, formas, Shinies e disponibilidade por jogo.

## Estado atual — v21.1.0

A v21.1.0 consolida o rework **Pokedex-only** e endurece a distribuição, compatibilidade e experiência de uso.

### Navegação principal

1. Início
2. Pokédex
3. Coleção
4. Box
5. Config.

### Garantias funcionais

- National Dex #0001–#1025.
- Metadados locais completos para as 1.025 espécies: tipos, seis stats base e habilidades.
- Normal, Shiny, formas regionais, especiais, cosméticas, gênero e formas de batalha.
- Coleção separada em Living Dex, Shiny Dex e Form Dex.
- Box fixa em **6 colunas × 5 linhas = 30 Pokémon por Box**.
- Persistência por jogo/região e atualização sem perda da coleção.
- Backup **schema 7 Pokedex-only**, com SHA-256, rollback e leitura compatível dos schemas 1–6.
- Conteúdo offline validado por versão e SHA-256.
- Atualizador interno valida versão, pacote, assinatura e SHA-256 do APK.

### Android e distribuição

- minSdk 26.
- compileSdk / targetSdk 37 (Android 17).
- APK de produção gerado pela variante **Release**, com R8 e resource shrinking.
- Smoke test completo em Android 11/API 30.
- Matriz de compatibilidade em API 26, 35 e 37.
- Upgrade real testado partindo da v21.0.0.

### Performance

- cache de imagem Coil com memória e disco;
- biblioteca offline separada do cache descartável;
- preload assíncrono e não bloqueante;
- prefetch limitado de detalhes e Boxes;
- ProfileInstaller incluído e baseline profile do fluxo principal;
- downloads concorrentes com limites definidos.

### Qualidade

O workflow **Android Build** valida:

- contrato de versão e pacote;
- arquitetura Pokedex-only;
- hardening funcional;
- acessibilidade;
- prontidão de dispositivo;
- artwork;
- National Dex e National Forms;
- testes unitários;
- Android Lint em Release;
- build Release + Debug + instrumentação;
- compatibilidade de assinatura e update;
- upgrade in-place preservando dados;
- navegação, persistência e chrome;
- lançamento em APIs 26, 35 e 37.

> A rotação/remoção da chave de assinatura do repositório foi deliberadamente deixada para uma etapa separada, conforme planejamento do projeto.
