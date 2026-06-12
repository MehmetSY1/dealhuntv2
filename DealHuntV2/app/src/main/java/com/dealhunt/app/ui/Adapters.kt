package com.dealhunt.app.ui

import android.view.*
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.dealhunt.app.R
import com.dealhunt.app.data.ExchangeRateManager
import com.dealhunt.app.data.StoreNames
import com.dealhunt.app.databinding.*
import com.dealhunt.app.model.*

// ── Search Results ──────────────────────────────────────────────────────────
class SearchResultAdapter(private val onClick: (GameSearchResult) -> Unit) :
    ListAdapter<GameSearchResult, SearchResultAdapter.VH>(object : DiffUtil.ItemCallback<GameSearchResult>() {
        override fun areItemsTheSame(a: GameSearchResult, b: GameSearchResult) = a.gameId == b.gameId
        override fun areContentsTheSame(a: GameSearchResult, b: GameSearchResult) = a == b
    }) {

    var tlRate: Double = 35.0

    inner class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(g: GameSearchResult) {
            b.tvGameTitle.text = g.title
            val usd = g.cheapest.toDoubleOrNull() ?: 0.0
            b.tvPrice.text = ExchangeRateManager.format(usd, tlRate)
            Glide.with(b.root).load(g.thumbnail)
                .placeholder(R.drawable.placeholder_game).centerCrop().into(b.ivThumbnail)
            b.root.setOnClickListener { onClick(g) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemSearchResultBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}

// ── Featured Deals ──────────────────────────────────────────────────────────
class FeaturedDealAdapter(private val onClick: (DealDetail) -> Unit) :
    ListAdapter<DealDetail, FeaturedDealAdapter.VH>(object : DiffUtil.ItemCallback<DealDetail>() {
        override fun areItemsTheSame(a: DealDetail, b: DealDetail) = a.dealId == b.dealId
        override fun areContentsTheSame(a: DealDetail, b: DealDetail) = a == b
    }) {

    var tlRate: Double = 35.0

    inner class VH(val b: ItemFeaturedDealBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(d: DealDetail) {
            b.tvTitle.text = d.title

            val sale = d.salePrice.toDoubleOrNull() ?: 0.0
            val norm = d.normalPrice.toDoubleOrNull() ?: 0.0
            val sav  = d.savings.toDoubleOrNull() ?: 0.0

            b.tvSalePrice.text   = ExchangeRateManager.format(sale, tlRate)
            b.tvNormalPrice.text = ExchangeRateManager.format(norm, tlRate)

            b.tvSavings.text = "-%${sav.toInt()}"
            b.tvSavings.visibility = if (sav > 0) View.VISIBLE else View.GONE

            // Platform adını göster
            b.tvPlatformBadge.text = StoreNames.get(d.storeId)

            Glide.with(b.root).load(d.thumbnail)
                .placeholder(R.drawable.placeholder_game).centerCrop().into(b.ivThumb)

            b.root.setOnClickListener { onClick(d) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemFeaturedDealBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}

// ── Platform Prices ─────────────────────────────────────────────────────────
class PlatformPriceAdapter(private val onBuy: (PlatformPrice) -> Unit) :
    ListAdapter<PlatformPrice, PlatformPriceAdapter.VH>(object : DiffUtil.ItemCallback<PlatformPrice>() {
        override fun areItemsTheSame(a: PlatformPrice, b: PlatformPrice) = a.dealId == b.dealId
        override fun areContentsTheSame(a: PlatformPrice, b: PlatformPrice) = a == b
    }) {

    var tlRate: Double = 35.0

    inner class VH(val b: ItemPlatformPriceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: PlatformPrice) {
            b.tvStoreName.text = p.storeName
            b.tvCurrentPrice.text = ExchangeRateManager.format(p.currentPrice, tlRate)

            if (p.originalPrice > p.currentPrice && p.savingsPercent > 0) {
                b.tvOriginalPrice.text = ExchangeRateManager.format(p.originalPrice, tlRate)
                b.tvOriginalPrice.visibility = View.VISIBLE
                b.tvDiscount.text = "-%${p.savingsPercent.toInt()}"
                b.tvDiscount.visibility = View.VISIBLE
            } else {
                b.tvOriginalPrice.visibility = View.GONE
                b.tvDiscount.visibility = View.GONE
            }

            b.badgeBest.visibility = if (p.isBestDeal) View.VISIBLE else View.GONE
            val ctx = b.root.context
            b.tvCurrentPrice.setTextColor(
                ctx.getColor(if (p.isBestDeal) R.color.accent_green else R.color.text_primary)
            )
            b.root.setBackgroundResource(
                if (p.isBestDeal) R.drawable.bg_best_deal_card else R.drawable.bg_platform_card
            )
            if (p.logoUrl.isNotEmpty()) {
                Glide.with(b.root).load(p.logoUrl).into(b.ivStoreLogo)
            }
            b.btnBuy.setOnClickListener { onBuy(p) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemPlatformPriceBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
