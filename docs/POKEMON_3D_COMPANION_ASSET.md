# Living Pokémon 3D — contrato do asset

A Home já possui a integração SceneView 2.3.0 / Filament. O modelo final deve ser incluído em:

`app/src/main/assets/models/pikachu_companion.glb`

## Requisitos do GLB

- glTF 2.0 / GLB autocontido
- +Y para cima
- pivô coerente com o chão
- malha, texturas e rig no mesmo arquivo quando possível
- tamanho recomendado do arquivo final: abaixo de 8–12 MB para manter abertura rápida da Home

## Clipes esperados

A integração procura estes nomes, sem diferenciar maiúsculas/minúsculas:

- `Idle`
- `Pet`
- `Call`
- `EatBerry`
- `Play`
- `Happy`

Há aliases para alguns nomes comuns. Se nenhum nome for encontrado, o primeiro clip do GLB é usado como fallback.

## Comportamento já implementado

- idle contínuo
- toque direto no modelo → reação Happy
- Carinho → Pet
- Chamar → Call
- Dar Berry → EatBerry
- Brincar → Play
- retorno automático ao Idle
- afinidade local da Home
- fallback 2D enquanto o GLB não estiver presente
- Scene Compose com superfície transparente para permitir composição com a UI Compose

O asset 3D não deve ser substituído por geometria procedural no build final.
