package com.dealhunt.app.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.dealhunt.app.R
import com.dealhunt.app.databinding.*
import com.dealhunt.app.model.*
import com.dealhunt.app.ui.*
import com.dealhunt.app.util.WishlistManager

private fun Fragment.openDetail(gameId: String, title: String, thumb: String) {
    startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
        putExtra("GAME_ID", gameId)
        putExtra("GAME_TITLE", title)
        putExtra("GAME_THUMB", thumb)
    })
}

// ── HOME ─────────────────────────────────────────────────────────────────────
class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var dealsAdapter: FeaturedDealAdapter
    private lateinit var aiAdapter: FeaturedDealAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentHomeBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        searchAdapter = SearchResultAdapter { g -> openDetail(g.gameId, g.title, g.thumbnail) }
        dealsAdapter  = FeaturedDealAdapter { d -> openDetail(d.gameId, d.title, d.thumbnail) }
        aiAdapter     = FeaturedDealAdapter { d -> openDetail(d.gameId, d.title, d.thumbnail) }

        b.rvSearch.apply  { layoutManager = LinearLayoutManager(context); adapter = searchAdapter }
        b.rvDeals.apply   { layoutManager = GridLayoutManager(context, 2); adapter = dealsAdapter }
        b.rvAiStrip.apply { layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false); adapter = aiAdapter }

        b.swipeRefresh.setOnRefreshListener { vm.loadDeals() }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b2: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                vm.searchGames(q)
                b.btnClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })
        b.btnClear.setOnClickListener { b.etSearch.text?.clear(); vm.clearSearch() }

        // Canlı kur güncellemesi
        vm.rate.observe(viewLifecycleOwner) { rate ->
            searchAdapter.tlRate = rate
            dealsAdapter.tlRate = rate
            aiAdapter.tlRate = rate
            searchAdapter.notifyDataSetChanged()
            dealsAdapter.notifyDataSetChanged()
            aiAdapter.notifyDataSetChanged()
        }

        vm.search.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle    -> { b.homeScroll.visibility = View.VISIBLE;  b.rvSearch.visibility = View.GONE;    b.tvSearchError.visibility = View.GONE;    b.progressSearch.visibility = View.GONE }
                is UiState.Loading -> { b.homeScroll.visibility = View.GONE;     b.rvSearch.visibility = View.GONE;    b.tvSearchError.visibility = View.GONE;    b.progressSearch.visibility = View.VISIBLE }
                is UiState.Success -> { b.homeScroll.visibility = View.GONE;     b.rvSearch.visibility = View.VISIBLE; b.tvSearchError.visibility = View.GONE;    b.progressSearch.visibility = View.GONE; searchAdapter.submitList(state.data) }
                is UiState.Error   -> { b.homeScroll.visibility = View.GONE;     b.rvSearch.visibility = View.GONE;    b.tvSearchError.visibility = View.VISIBLE; b.progressSearch.visibility = View.GONE; b.tvSearchError.text = state.msg }
            }
        }

        vm.deals.observe(viewLifecycleOwner) { state ->
            b.swipeRefresh.isRefreshing = false
            if (state is UiState.Success) {
                dealsAdapter.submitList(state.data)
                aiAdapter.submitList(state.data.shuffled().take(10))
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── EXPLORE ──────────────────────────────────────────────────────────────────
class ExploreFragment : Fragment() {
    private var _b: FragmentExploreBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter

    private val genres = listOf("🔥 Trend","⚔️ RPG","🎯 Aksiyon","🧩 Strateji","👾 Indie","🏎️ Yarış","⚽ Spor","🔫 FPS","🌍 Açık Dünya","🧟 Korku","🎮 Platform")
    private val queries = listOf("top","rpg","action","strategy","indie","racing","sports","fps","open world","horror","platformer")
    private var selected = 0

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentExploreBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        adapter = SearchResultAdapter { g -> openDetail(g.gameId, g.title, g.thumbnail) }
        b.rvExplore.apply { layoutManager = LinearLayoutManager(context); adapter = this@ExploreFragment.adapter }

        genres.forEachIndexed { i, label ->
            val chip = TextView(requireContext()).apply {
                text = label; textSize = 13f; setPadding(28, 12, 28, 12)
                setTextColor(if (i == 0) Color.parseColor("#0A0C10") else Color.parseColor("#B0B8C8"))
                background = requireContext().getDrawable(if (i == 0) R.drawable.bg_chip_active else R.drawable.bg_chip)
                val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 8; layoutParams = lp
                setOnClickListener { selectGenre(i) }
            }
            b.chipGroup.addView(chip)
        }

        vm.rate.observe(viewLifecycleOwner) { rate ->
            adapter.tlRate = rate
            adapter.notifyDataSetChanged()
        }

        vm.search.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> b.progressExplore.visibility = View.VISIBLE
                is UiState.Success -> { b.progressExplore.visibility = View.GONE; adapter.submitList(state.data) }
                else -> b.progressExplore.visibility = View.GONE
            }
        }
        selectGenre(0)
    }

    private fun selectGenre(idx: Int) {
        selected = idx
        for (i in 0 until b.chipGroup.childCount) {
            val chip = b.chipGroup.getChildAt(i) as? TextView ?: continue
            chip.setTextColor(Color.parseColor(if (i == idx) "#0A0C10" else "#B0B8C8"))
            chip.background = requireContext().getDrawable(if (i == idx) R.drawable.bg_chip_active else R.drawable.bg_chip)
        }
        vm.searchByGenre(queries[idx])
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── WISHLIST ──────────────────────────────────────────────────────────────────
class WishlistFragment : Fragment() {
    private var _b: FragmentWishlistBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentWishlistBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        val adapter = SearchResultAdapter { g -> openDetail(g.gameId, g.title, g.thumbnail) }
        b.rvWishlist.apply { layoutManager = LinearLayoutManager(context); this.adapter = adapter }
        vm.rate.observe(viewLifecycleOwner) { rate -> adapter.tlRate = rate; adapter.notifyDataSetChanged() }
        val list = WishlistManager.getAll(requireContext())
        if (list.isEmpty()) { b.emptyView.visibility = View.VISIBLE; b.rvWishlist.visibility = View.GONE }
        else { b.emptyView.visibility = View.GONE; b.rvWishlist.visibility = View.VISIBLE; adapter.submitList(list) }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── AI ────────────────────────────────────────────────────────────────────────
class AiFragment : Fragment() {
    private var _b: FragmentAiBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: FeaturedDealAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAiBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        adapter = FeaturedDealAdapter { d -> openDetail(d.gameId, d.title, d.thumbnail) }
        b.rvAi.apply { layoutManager = GridLayoutManager(context, 2); adapter = this@AiFragment.adapter }
        vm.rate.observe(viewLifecycleOwner) { rate -> adapter.tlRate = rate; adapter.notifyDataSetChanged() }
        vm.deals.observe(viewLifecycleOwner) { if (it is UiState.Success) adapter.submitList(it.data.shuffled()) }
        vm.loadDeals()
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── PROFILE ───────────────────────────────────────────────────────────────────
class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentProfileBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        b.tvWishlistCount.text = WishlistManager.getAll(requireContext()).size.toString()
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
