package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Persistent local team builder state. */
object TeamStore {
    private const val PREFS = "pokedex_teams"
    private const val KEY_TEAMS = "teams_v1"
    private const val MAX_TEAM_SIZE = 6

    data class Team(
        val id: Long,
        val name: String,
        val members: List<Int>
    )

    private var context: Context? = null

    var teams by mutableStateOf<List<Team>>(emptyList())
        private set

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val prefs = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TEAMS, null)
        teams = raw?.let(::decodeTeams).orEmpty()
        if (teams.isEmpty()) {
            teams = listOf(Team(id = System.currentTimeMillis(), name = "Meu time", members = emptyList()))
            persist()
        }
    }

    fun createTeam(name: String): Long? {
        val clean = sanitizeName(name)
        if (clean.isBlank()) return null
        val id = (System.currentTimeMillis() + teams.size).coerceAtLeast(1L)
        teams = teams + Team(id = id, name = clean, members = emptyList())
        persist()
        return id
    }

    fun createTeam(name: String, members: List<Int>): Long? {
        val clean = sanitizeName(name)
        if (clean.isBlank()) return null
        val valid = members.filter { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }.distinct().take(MAX_TEAM_SIZE)
        val id = (System.currentTimeMillis() + teams.size).coerceAtLeast(1L)
        teams = teams + Team(id = id, name = clean, members = valid)
        persist()
        return id
    }

    fun renameTeam(teamId: Long, name: String): Boolean {
        val clean = sanitizeName(name)
        if (clean.isBlank() || teams.none { it.id == teamId }) return false
        teams = teams.map { if (it.id == teamId) it.copy(name = clean) else it }
        persist()
        return true
    }

    fun deleteTeam(teamId: Long): Boolean {
        if (teams.size <= 1 || teams.none { it.id == teamId }) return false
        teams = teams.filterNot { it.id == teamId }
        persist()
        return true
    }

    fun addPokemon(teamId: Long, pokemonId: Int): Boolean {
        if (pokemonId !in 1..PokeApiService.MAX_NATIONAL_DEX_ID) return false
        val team = teams.firstOrNull { it.id == teamId } ?: return false
        if (pokemonId in team.members) return true
        if (team.members.size >= MAX_TEAM_SIZE) return false
        updateMembers(teamId, team.members + pokemonId)
        return true
    }

    fun removePokemon(teamId: Long, pokemonId: Int) {
        val team = teams.firstOrNull { it.id == teamId } ?: return
        updateMembers(teamId, team.members - pokemonId)
    }

    fun replacePokemon(teamId: Long, oldId: Int, newId: Int): Boolean {
        if (newId !in 1..PokeApiService.MAX_NATIONAL_DEX_ID) return false
        val team = teams.firstOrNull { it.id == teamId } ?: return false
        val index = team.members.indexOf(oldId)
        if (index == -1) return false
        if (newId != oldId && newId in team.members) return false
        val members = team.members.toMutableList().apply { set(index, newId) }
        updateMembers(teamId, members)
        return true
    }

    fun moveMember(teamId: Long, pokemonId: Int, direction: Int): Boolean {
        val team = teams.firstOrNull { it.id == teamId } ?: return false
        val from = team.members.indexOf(pokemonId)
        if (from == -1) return false
        val to = (from + direction).coerceIn(0, team.members.lastIndex)
        if (to == from) return false
        val members = team.members.toMutableList()
        val moved = members.removeAt(from)
        members.add(to, moved)
        updateMembers(teamId, members)
        return true
    }

    fun exportSnapshot(): JSONArray {
        val array = JSONArray()
        teams.forEach { team ->
            array.put(JSONObject().put("id", team.id).put("name", team.name).put("members", JSONArray(team.members)))
        }
        return array
    }

    fun importSnapshot(array: JSONArray): Boolean = runCatching {
        val restored = buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val membersArray = obj.optJSONArray("members") ?: JSONArray()
                val members = buildList {
                    for (j in 0 until membersArray.length()) {
                        membersArray.optInt(j).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID && it !in this }?.let(::add)
                    }
                }.take(MAX_TEAM_SIZE)
                add(Team(obj.optLong("id", i.toLong()+1), sanitizeName(obj.optString("name", "Time " + (i+1))), members))
            }
        }
        teams = restored.ifEmpty { listOf(Team(System.currentTimeMillis(), "Meu time", emptyList())) }
        persist()
        true
    }.getOrDefault(false)

    private fun updateMembers(teamId: Long, members: List<Int>) {
        teams = teams.map { if (it.id == teamId) it.copy(members = members.take(MAX_TEAM_SIZE)) else it }
        persist()
    }

    private fun sanitizeName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").take(32)

    private fun persist() {
        val array = JSONArray()
        teams.forEach { team ->
            val obj = JSONObject()
                .put("id", team.id)
                .put("name", team.name)
                .put("members", JSONArray(team.members))
            array.put(obj)
        }
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_TEAMS, array.toString())
            ?.apply()
    }

    private fun decodeTeams(raw: String): List<Team> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val membersArray = obj.optJSONArray("members") ?: JSONArray()
                val members = buildList {
                    for (j in 0 until membersArray.length()) {
                        val id = membersArray.optInt(j)
                        if (id in 1..PokeApiService.MAX_NATIONAL_DEX_ID && id !in this) add(id)
                    }
                }.take(MAX_TEAM_SIZE)
                add(
                    Team(
                        id = obj.optLong("id", i.toLong() + 1),
                        name = obj.optString("name", "Time ${i + 1}"),
                        members = members
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
