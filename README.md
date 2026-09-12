# POKEDEX

Aplicativo Android pessoal para organizar a jornada nos jogos Pokémon, acompanhar a coleção por jogo/região e consultar a National Dex com formas e variantes.

## Estado atual — v18.1.0

A v18.1.0 é a baseline estável de saneamento da arquitetura atual. Ela preserva o comportamento aprovado da v18.0.0 e reforça segurança de restauração, inicialização de áudio, documentação e guards de regressão.

### Navegação principal

1. **Jornada** — escolha do jogo, objetivos, mapas, progresso e guia de campanha.
2. **Pokédex** — National Dex #0001–#1025, formas, variantes e Shiny.
3. **Boxes** — coleção contextual por jogo/região, 30 Pokémon por Box, busca, progresso e gerenciamento Normal/Shiny/formas.
4. **Config.** — downloads offline, backup/restauração, áudio, cache e informações da versão.

## Fundação técnica

- Kotlin + Jetpack Compose
- Navigation Compose com back stack compatível com o botão Voltar do Android
- PokéAPI com cache em memória + cache persistente GZIP
- Coil com cache de imagens e preload priorizado
- Pacotes offline por jogo/região com auditoria de integridade
- Persistência de Jornada, coleção, variantes, jogo/região ativa, página da Box, times e atividade recente
- Backup local com rollback de segurança em restaurações incompletas
- Áudio local com preparação assíncrona
- Identidade canônica de formas para evitar conflito entre variantes que compartilham Pokémon ID

## Qualidade e regressão

O workflow **Android Build** executa em cada atualização relevante:

- validação da versão;
- auditoria da arquitetura;
- auditoria de artwork;
- auditoria de visuais da Jornada;
- auditoria completa da National Dex;
- auditoria de formas;
- auditoria de formas problemáticas;
- testes unitários;
- Android Lint;
- build do APK atualizável.

Os guards históricos permanecem no repositório como **compatibility guards**: eles protegem comportamentos estabilizados ao longo das versões anteriores sem representar a versão atual do aplicativo.

## Performance

A estratégia de dados prioriza:

1. memória;
2. cache persistente;
3. rede.

O preload de abertura é limitado e prioriza Pokémon recentes, Box ativa, variantes possuídas e conteúdo do jogo atual. A Box também aquece uma janela limitada ao redor da página atual.

## Build

O APK oficial é gerado pelo workflow **Android Build**. A assinatura estável atual foi preservada nesta atualização para manter compatibilidade de instalação com APKs anteriores.
