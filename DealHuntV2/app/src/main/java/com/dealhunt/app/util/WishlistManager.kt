package com.dealhunt.app.util

import android.content.Context
import com.dealhunt.app.model.GameSearchResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WishlistManager {
    private val gson = Gson()

    fun getAll(ctx: Context): MutableList<GameSearchResult> {
        val json = prefs(ctx).getString("wl", "[]") ?: "[]"
        return try { gson.fromJson(json, object : TypeToken<MutableList<GameSearchResult>>() {}.type) } catch (e: Exception) { mutableListOf() }
    }

    fun add(ctx: Context, game: GameSearchResult) {
        val list = getAll(ctx)
        if (list.none { it.gameId == game.gameId }) { list.add(game); save(ctx, list) }
    }

    fun remove(ctx: Context, gameId: String) { save(ctx, getAll(ctx).filter { it.gameId != gameId }.toMutableList()) }

    fun has(ctx: Context, gameId: String) = getAll(ctx).any { it.gameId == gameId }

    private fun save(ctx: Context, list: List<GameSearchResult>) =
        prefs(ctx).edit().putString("wl", gson.toJson(list)).apply()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("dh_prefs", Context.MODE_PRIVATE)
}
