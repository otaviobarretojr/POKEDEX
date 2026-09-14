package com.otaviobarreto.pokedex.data

enum class CanonicalAcquisitionKind(val label:String){
    WILD("Captura selvagem"),
    EVOLUTION("Evolução"),
    TRADE("Troca"),
    GIFT_STARTER("Presente / inicial"),
    RAID("Raid"),
    EVENT_SPECIAL("Evento / método especial"),
    HOME_TRANSFER("Transferência / HOME"),
    OTHER_METHOD("Outro método"),
    UNAVAILABLE("Não disponível nesta Pokédex")
}

enum class AvailabilityConfidence { CONFIRMED, PARTIAL }

data class CanonicalAvailability(
    val pokemonId:Int,
    val context:GameContext,
    val inRegionalDex:Boolean,
    val regionalNumber:Int?,
    val version:VersionAvailability?,
    val acquisitionKind:CanonicalAcquisitionKind,
    val acquisitionLabel:String,
    val requirement:String?=null,
    val locations:List<String> = emptyList(),
    val confidence:AvailabilityConfidence,
    val provenance:List<String> = emptyList()
)

object CanonicalAvailabilityResolver {

    fun resolve(
        pokemonId:Int,
        context:GameContext,
        encounters:List<PokeApiService.EncounterLocation>,
        dex:List<GameDexService.GameDexEntry>,
        evolutionChain:List<PokeApiService.EvolutionStage> = emptyList()
    ):CanonicalAvailability {
        val dexEntry=dex.firstOrNull{it.nationalId==pokemonId}
        val inDex=dexEntry!=null
        val version=VersionAvailabilityCatalog.forPokemon(pokemonId,context,inDex)
        if(!inDex){
            return CanonicalAvailability(
                pokemonId=pokemonId,
                context=context,
                inRegionalDex=false,
                regionalNumber=null,
                version=version,
                acquisitionKind=CanonicalAcquisitionKind.UNAVAILABLE,
                acquisitionLabel="Fora da Pokédex ${context.regionLabel}",
                confidence=AvailabilityConfidence.CONFIRMED,
                provenance=listOf("Pokédex regional")
            )
        }

        val locations=LocationIntelligence.filter(encounters,context)
            .map{LocationIntelligence.displayName(it.location)}
            .distinct()
        if(locations.isNotEmpty()){
            return CanonicalAvailability(
                pokemonId=pokemonId,
                context=context,
                inRegionalDex=true,
                regionalNumber=dexEntry.gameNumber,
                version=version,
                acquisitionKind=CanonicalAcquisitionKind.WILD,
                acquisitionLabel="Captura selvagem em ${context.regionLabel}",
                locations=locations,
                confidence=if(LocationIntelligence.coverage(context)==LocationIntelligence.Coverage.RELIABLE) AvailabilityConfidence.CONFIRMED else AvailabilityConfidence.PARTIAL,
                provenance=listOf("Pokédex regional","Encontros por versão")
            )
        }

        val index=evolutionChain.indexOfFirst{it.pokemonId==pokemonId}
        val current=evolutionChain.getOrNull(index)
        val previous=if(index>0)evolutionChain.getOrNull(index-1) else null
        if(previous!=null && dex.any{it.nationalId==previous.pokemonId}){
            val req=current?.requirement
            val trade=req?.contains("Troca",true)==true || req?.contains("Trocar",true)==true
            return CanonicalAvailability(
                pokemonId=pokemonId,
                context=context,
                inRegionalDex=true,
                regionalNumber=dexEntry.gameNumber,
                version=version,
                acquisitionKind=if(trade)CanonicalAcquisitionKind.TRADE else CanonicalAcquisitionKind.EVOLUTION,
                acquisitionLabel=if(trade)"Evolução por troca de ${previous.name}" else "Evolua ${previous.name}",
                requirement=req,
                confidence=AvailabilityConfidence.CONFIRMED,
                provenance=listOf("Pokédex regional","Cadeia evolutiva")
            )
        }

        val special=SpecialAcquisitionCatalog.lookup(pokemonId,context)
        if(special!=null){
            return CanonicalAvailability(
                pokemonId=pokemonId,
                context=context,
                inRegionalDex=true,
                regionalNumber=dexEntry.gameNumber,
                version=version,
                acquisitionKind=special.kind,
                acquisitionLabel=special.label,
                requirement=special.requirement,
                confidence=AvailabilityConfidence.CONFIRMED,
                provenance=special.provenance
            )
        }

        return CanonicalAvailability(
            pokemonId=pokemonId,
            context=context,
            inRegionalDex=true,
            regionalNumber=dexEntry.gameNumber,
            version=version,
            acquisitionKind=CanonicalAcquisitionKind.OTHER_METHOD,
            acquisitionLabel="",
            requirement=null,
            confidence=AvailabilityConfidence.PARTIAL,
            provenance=listOf("Pokédex regional")
        )
    }
}

data class SpecialAcquisition(
    val kind:CanonicalAcquisitionKind,
    val label:String,
    val requirement:String?=null,
    val provenance:List<String> = listOf("Curadoria multi-fonte")
)

object SpecialAcquisitionCatalog {
    fun lookup(id:Int,context:GameContext):SpecialAcquisition? {
        val key=context.pokedexSlug
        return when {
            key in setOf("lumiose-city","hyperspace") && id in setOf(152,498,158) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            key=="paldea" && id in setOf(906,909,912) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            key=="hisui" && id in setOf(722,155,501) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            key=="galar" && id in setOf(810,813,816) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            key=="original-sinnoh" && id in setOf(387,390,393) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            key=="letsgo-kanto" && id in setOf(25,133) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Parceiro inicial","A espécie parceira depende da versão")
            key=="kanto" && id in setOf(1,4,7) -> SpecialAcquisition(CanonicalAcquisitionKind.GIFT_STARTER,"Pokémon inicial","Escolha inicial da jornada")
            else -> null
        }
    }
}
