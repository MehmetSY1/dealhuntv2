package com.dealhunt.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dealhunt.app.R
import com.dealhunt.app.data.ExchangeRateManager
import com.dealhunt.app.data.NetworkClient
import com.dealhunt.app.databinding.ActivityDetailBinding
import com.dealhunt.app.model.GameDetailUiState
import com.dealhunt.app.model.GameSearchResult
import com.dealhunt.app.util.WishlistManager

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private lateinit var priceAdapter: PlatformPriceAdapter

    private var gameId = ""
    private var gameTitle = ""
    private var gameThumbnail = ""
    private var currentRate = 35.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameId        = intent.getStringExtra("GAME_ID") ?: run { finish(); return }
        gameTitle     = intent.getStringExtra("GAME_TITLE") ?: ""
        gameThumbnail = intent.getStringExtra("GAME_THUMB") ?: ""

        if (gameId.isBlank()) { finish(); return }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = gameTitle
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (gameThumbnail.isNotEmpty()) {
            Glide.with(this).load(gameThumbnail)
                .placeholder(R.drawable.placeholder_game).centerCrop()
                .into(binding.ivGameHero)
        }

        priceAdapter = PlatformPriceAdapter { price ->
            try {
                val url = NetworkClient.dealUrl(price.dealId)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) { e.printStackTrace() }
        }

        binding.rvPrices.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity)
            adapter = priceAdapter
        }

        binding.btnRefresh.setOnClickListener { viewModel.load(gameId) }

        updateWishlistIcon()
        binding.fabWishlist.setOnClickListener {
            val game = GameSearchResult(gameId = gameId, title = gameTitle, thumbnail = gameThumbnail)
            if (WishlistManager.has(this, gameId)) WishlistManager.remove(this, gameId)
            else WishlistManager.add(this, game)
            updateWishlistIcon()
        }

        viewModel.rate.observe(this) { rate ->
            currentRate = rate
            priceAdapter.tlRate = rate
            priceAdapter.notifyDataSetChanged()
        }

        viewModel.detail.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressDetail.visibility = View.VISIBLE
                    binding.contentLayout.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressDetail.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    render(state.data)
                }
                is UiState.Error -> {
                    binding.progressDetail.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.msg
                }
                else -> {}
            }
        }

        viewModel.load(gameId)
    }

    private fun render(d: GameDetailUiState) {
        binding.tvGameTitle.text = d.title

        val best = d.platformPrices.firstOrNull()
        if (best != null) {
            binding.tvBestPlatform.text = best.storeName
            binding.tvBestPrice.text = ExchangeRateManager.format(best.currentPrice, currentRate)

            if (best.originalPrice > best.currentPrice && best.savingsPercent > 0) {
                binding.tvBestDiscount.text = "-%${best.savingsPercent.toInt()}"
                binding.tvBestDiscount.visibility = View.VISIBLE
                binding.tvBestOriginal.text = ExchangeRateManager.format(best.originalPrice, currentRate)
                binding.tvBestOriginal.visibility = View.VISIBLE
            } else {
                binding.tvBestDiscount.visibility = View.GONE
                binding.tvBestOriginal.visibility = View.GONE
            }

            binding.btnBuyBest.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NetworkClient.dealUrl(best.dealId))))
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        priceAdapter.tlRate = currentRate
        priceAdapter.submitList(d.platformPrices)
        binding.tvCheapestEver.text = "${d.cheapestEver} (${d.cheapestEverDate})"
        binding.tvPlatformCount.text = "${d.platformPrices.size} platformda mevcut"
    }

    private fun updateWishlistIcon() {
        binding.fabWishlist.setImageResource(
            if (WishlistManager.has(this, gameId)) R.drawable.ic_wishlist_filled
            else R.drawable.ic_wishlist_outline
        )
    }
}
