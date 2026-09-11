# POKEDEX

Aplicativo Android pessoal para organizar a jornada nos jogos Pokémon, acompanhar Boxes e consultar informações contextualizadas por jogo.

## Estado atual — v6.21.0

O produto foi consolidado em dois pilares principais:

- **Jornada:** escolha do jogo, rota recomendada, mapa, objetivos, progresso e guia de time.
- **Boxes:** coleção contextual por jogo/região, progresso e acesso à ficha de cada Pokémon.

Telas auxiliares permanecem acessíveis a partir desses fluxos: detalhes do Pokémon, evolução, tipos, localização/mapa regional, referências de golpes/habilidades/itens e guia de campanha.

## Arquitetura

- Kotlin + Jetpack Compose
- Navegação com Navigation Compose
- PokéAPI com cache persistente em disco e memória
- Cache de imagens via Coil
- Preload real na abertura
- Pacotes offline por jogo
- Persistência de Jornada, Boxes, jogo/região ativa e atividade recente
- Uma trilha contínua durante o app, com música própria no preload
- CI com auditoria estrutural, auditoria de assets, testes, lint e build do APK

## Design foundation

A v6.21.0 prepara a próxima fase visual sem alterar a aparência atual:

- tokens centrais de cor, tipografia, espaçamento, raio e elevação;
- tema centralizado;
- componentes da Jornada separados em arquivos menores;
- inicialização de estado centralizada no Application;
- nomenclatura de estado desvinculada do antigo Companion;
- versionamento local e CI alinhados.

## Princípios para o redesign

1. A lógica funcional fica congelada enquanto a camada visual evolui.
2. Jornada e Boxes permanecem como navegação principal.
3. Cache, offline, áudio e back stack não devem depender de componentes visuais.
4. Novos componentes devem consumir o design system central.
5. Cada tela deve continuar passando por testes, lint e build antes de merge.

## Build

O build oficial é gerado pelo workflow Android Build e usa a assinatura estável do projeto para permitir atualização sobre versões anteriores.
