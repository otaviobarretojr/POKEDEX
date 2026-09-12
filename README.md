# POKEDEX

Aplicativo Android pessoal para organizar a jornada nos jogos Pokémon, acompanhar a coleção por jogo/região e consultar a National Dex com formas e variantes.

## Estado atual — v18.3.0

A v18.3.0 é a **Foundation Lock** da base v18. O foco desta versão é encerrar a fase de saneamento da infraestrutura antes das próximas atualizações funcionais e visuais.

### Principais garantias

- National Dex #0001–#1025.
- Normal, Shiny e formas com identidade canônica.
- Box contextual por jogo/região.
- Backup schema 5 com compatibilidade para backups antigos sem misturar dados novos da instalação atual.
- Restore com rollback e reparo de integridade da coleção.
- Serviço central de auditoria/reparo para ownership global, contextual, Box e variantes.
- Pacotes offline com forms/Shiny obrigatórios: falhas entram em retry em vez de serem ignoradas.
- Manifesto de artes de formas/Shiny e preservação de recursos compartilhados.
- Cache persistente GZIP com limite para entradas não fixadas; recursos offline pinados são preservados.
- Diagnóstico de armazenamento e botão **Verificar integridade** nas Configurações.
- Métrica da duração do preload inicial.
- Startup limitado ao contexto ativo.
- Áudio local assíncrono.
- AGP 8.6.1 + Gradle 8.7 para suporte oficial ao compileSdk 35.

### Navegação principal

1. Jornada
2. Pokédex
3. Boxes
4. Config.

### Qualidade

O workflow **Android Build** valida:

- versão e identidade do pacote;
- arquitetura;
- artwork;
- visuais da Jornada;
- National Dex;
- National Forms;
- formas problemáticas;
- testes unitários;
- Android Lint;
- build do APK atualizável.

A assinatura estável atual permanece preservada para permitir atualização sobre as instalações anteriores.
