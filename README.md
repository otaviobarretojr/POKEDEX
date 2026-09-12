# POKEDEX

Aplicativo Android pessoal para organizar a jornada nos jogos Pokémon, acompanhar a coleção por jogo/região e consultar a National Dex com formas e variantes.

## Estado atual — v18.2.0

A v18.2.0 consolida a segunda rodada de auditoria da base v18, com foco em **offline, cache de formas, startup e robustez de restauração**.

### Navegação principal

1. **Jornada**
2. **Pokédex**
3. **Boxes**
4. **Config.**

## Principais garantias da v18.2.0

- National Dex #0001–#1025.
- Identidade canônica de Normal, Shiny e formas.
- Cache offline de formas usando espécie + Pokémon da forma + formKey + estado Shiny.
- Manifesto próprio de artes de formas/Shiny e auditoria de integridade dessas imagens.
- Remoção de pacote offline preservando recursos compartilhados por outros jogos.
- Limpeza das artes de formas quando um pacote é removido.
- Splash mostrando automaticamente a versão real do aplicativo.
- Startup limitado ao contexto ativo, evitando bloquear a abertura com todos os jogos/catálogos.
- Nenhuma auditoria fatal no splash.
- Restauração de backup executada fora da thread principal, com rollback de segurança.
- Cache de rede consolidado no PersistentApiCache; camada legada HttpResponseCache removida.

## Fundação técnica

- Kotlin + Jetpack Compose
- Navigation Compose
- PokéAPI com cache em memória + PersistentApiCache GZIP
- Coil para cache de imagens
- Pacotes offline por jogo/região
- Persistência de Jornada, coleção, variantes, times, jogo/região e página da Box
- Áudio local com preparação assíncrona
- Backup local com rollback

## Qualidade

O workflow **Android Build** valida:

- versão;
- arquitetura;
- artwork;
- visuais da Jornada;
- National Dex;
- formas;
- formas problemáticas;
- testes unitários;
- Android Lint;
- build do APK atualizável.

## Build

O APK oficial é gerado pelo workflow **Android Build** com a assinatura estável atual, preservada para manter atualização sobre versões anteriores.
