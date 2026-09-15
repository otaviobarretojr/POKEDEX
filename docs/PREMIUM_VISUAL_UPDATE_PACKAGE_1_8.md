# Premium Visual Update Package — Etapas 1 a 8

Status: ACTIVE  
Branch: `feature/nintendo-companion-design-system`  
Baseline funcional: v20.12.1 Trainer Companion  
Checkpoint de entrada: `f2d53057001f5bdca70d6669e6bf0200a3100936`  
CI de entrada: Android Build #1606

## Objetivo

Concluir em sequência a evolução visual premium do aplicativo, preservando comportamento, dados, performance, artwork e contratos de arquitetura. A linguagem visual deve permanecer coerente com o Nintendo / Pokémon Trainer Companion Design System já implantado no projeto.

## Regras de execução

1. Trabalhar somente sobre o projeto existente e a branch acima.
2. Não recriar telas, stores, navegação ou pipelines funcionais sem necessidade.
3. Não alterar regras de captura, Living Dex, Shiny, Forms, Box fixa, Journey ou Game Dex apenas por motivo visual.
4. Preferir `PokedexDesignTokens` e componentes Companion existentes a valores mágicos.
5. Não enfraquecer guards para fazer CI passar. Se um guard bloquear uma evolução válida, atualizar o contrato de forma compatível e documentada.
6. Manter `BoxesV2Screen.kt` dentro do orçamento de hardening vigente (820 linhas) ou extrair componentes.
7. Cada etapa deve gerar checkpoint rastreável e passar pelos gates aplicáveis antes da etapa seguinte.
8. Se houver falha, corrigir a causa no mesmo estágio antes de avançar.
9. Jornada, Pokédex e Coleção já estabilizadas não devem ser redesenhadas sem evidência encontrada na auditoria global.
10. Ao final, atualizar este documento com commits, pipelines, achados e estado final.

## Etapa 1 — Fechar checkpoint atual da Box

- Concluir Android Build #1606.
- Exigir: arquitetura, hardening, acessibilidade, device readiness, artwork, Journey assets, National Dex, Forms, unit tests, Lint, APK e upload.
- Se falhar, corrigir antes de seguir.

**Gate:** workflow completo `success`.

## Etapa 2 — Slots da Box

Refinar os 30 slots preservando obrigatoriamente o grid 5×6 e a ordem fixa.

- Hierarquia de artwork.
- Estados capturado / não capturado.
- Shiny e formas sem poluição visual.
- Feedback de toque e long press.
- Contraste light/dark.
- Estados vazios.
- Sem IO ou cálculos pesados durante scroll/composição.

**Gate:** arquitetura + hardening + acessibilidade + testes + Lint + APK.

## Etapa 3 — Ações e navegação da Box

Polir sem alterar comportamento:

- Pesquisar.
- Todas as Boxes.
- Filtro de evolução.
- Swipe horizontal.
- Box atual / total.
- Seletores jogo e região/DLC.
- Pop-ups de captura e variantes.

Eliminar duplicidades entre contexto superior, cabeçalho compacto e progresso.

**Gate:** fluxo funcional preservado + CI completo.

## Etapa 4 — Auditoria consolidada da Box

Inspecionar a Box como produto completo:

`Jogo → Região/DLC → Box → slot → detalhes/captura → variante → pesquisa → todas as Boxes → filtro de evolução`.

Revisar:
- densidade;
- hierarquia;
- tipografia;
- espaçamento;
- ícones;
- estados;
- acessibilidade;
- dark mode;
- performance;
- persistência de página/contexto;
- ausência de controles de movimentação;
- ausência de duplicidades.

Corrigir somente achados concretos.

**Gate:** checkpoint final verde da Box.

## Etapa 5 — Companion Center / Configurações

Arquivo principal: `CompanionCenterScreen.kt`.

Reorganizar visualmente:
- cabeçalho/contexto;
- Conteúdo offline;
- Armazenamento e desempenho;
- Áudio;
- Informações da versão;
- Backup e restauração;
- feedback/status.

Direção:
- menos aparência de painel técnico;
- seções Companion claras;
- superfícies leves;
- ações destrutivas/manutenção visualmente distintas;
- linguagem compreensível sem remover diagnóstico útil;
- preservar downloads, recuperação, cache, integridade, áudio e backup.

**Gate:** funções existentes preservadas + hardening + acessibilidade + testes + Lint + APK.

## Etapa 6 — Telas secundárias

Auditar e harmonizar somente onde houver inconsistência:
- Universal Search;
- Evolution Center;
- Pokémon Detail / Forms;
- Game Dex;
- Campaign Guide;
- Reference Hub;
- dialogs e telas full-screen auxiliares.

Verificar uso consistente de:
- `CompanionContextHeader`;
- `CompanionSectionHeader`;
- tokens;
- artwork;
- navegação/back;
- estados loading/empty/error;
- transições.

**Gate:** nenhuma regressão de rotas ou detalhes + CI completo.

## Etapa 7 — Auditoria visual global

Percorrer o produto como uma experiência única:

`Jornada → Pokédex → Coleção → Box → Configurações → rotas secundárias`.

Inspecionar:
- identidade visual;
- consistência Nintendo/Companion;
- tipografia;
- espaçamento;
- superfícies;
- artwork;
- cores por jogo;
- motion;
- feedback tátil;
- navegação inferior;
- status bar/navigation bar;
- light/dark;
- telas pequenas e grandes;
- duplicidades entre módulos;
- performance percebida.

Não reabrir módulos estáveis sem achado verificável.

**Gate:** lista de achados zerada ou documentada como não bloqueante.

## Etapa 8 — Fechamento da versão

Executar validação final:
- Verify source architecture;
- Product hardening audit;
- Accessibility audit;
- Device readiness;
- Artwork audit;
- Journey visual/boss artwork;
- Full National Dex;
- National Forms;
- Problem Forms;
- Unit tests;
- Android Lint;
- Build debug APK;
- Upload APK.

Depois:
- registrar commit final;
- registrar pipeline final;
- registrar APK;
- atualizar roadmap;
- documentar módulos congelados e pendências futuras;
- manter rollback possível para checkpoints anteriores.

## Critério de conclusão

O pacote 1–8 só é considerado concluído quando a Etapa 8 estiver verde. Nenhuma etapa pode ser marcada como concluída apenas por alteração de código sem o respectivo gate.

## Log de execução

| Etapa | Estado | Commit | Pipeline | Observação |
|---|---|---|---|---|
| 1 | CONCLUÍDA | f2d53057001f5bdca70d6669e6bf0200a3100936 | #1606 | Pipeline completo verde |
| 2 | CONCLUÍDA | f2356f37ce39096ef3a0edbc9e04619b363e2502 | #1608 | Slots validados |
| 3 | CONSOLIDADA | pacote 3–8 | — | Ações e navegação revisadas |
| 4 | CONSOLIDADA | pacote 3–8 | — | Auditoria Box incorporada |
| 5 | CONSOLIDADA | pacote 3–8 | — | Hierarquia Companion aplicada |
| 6 | AUDITADA | pacote 3–8 | — | Rotas estáveis preservadas |
| 7 | AUDITADA | pacote 3–8 | — | Consistência transversal revisada |
| 8 | EM VALIDAÇÃO | pacote 3–8 | próximo CI | Gate final único |


## Pós-pacote — Evolução 4–8

Base validada: `2e9651af32248589e2df674b65456bc4807fc48b` · Android Build #1614 SUCCESS.

Execução autorizada em sequência:

- 4 — Motion & feedback: microinterações curtas, feedback háptico consistente e transições sem comprometer performance/acessibilidade.
- 5 — Pokémon Detail 2.0: consolidar identidade, formas, evolução, golpes e localização sem duplicar conteúdo.
- 6 — Jornada inteligente: priorizar automaticamente o próximo objetivo e contexto útil, preservando progresso existente.
- 7 — Coleção inteligente: transformar lacunas da coleção em atalhos acionáveis sem criar dashboard duplicado.
- 8 — Release Candidate: instalação/atualização, auditorias, testes, lint, APK e artefato final.

Regra: implementar apenas mudanças sustentadas pela auditoria do código atual; módulos já adequados devem ser preservados. O RC só fecha com pipeline integral verde.
