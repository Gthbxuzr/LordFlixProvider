package com.example.lordflix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class LordFlixProvider : MainAPI() {
    override var mainUrl = "https://lordflix.org"
    override var name = "LordFlix"
    override val hasMainPage = true
    override var lang = "ar"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/movies/" to "Movies",
        "$mainUrl/series/" to "Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val doc = app.get(url).document
        val items = doc.select("article.item, .movie-item").mapNotNull {
            val title = it.selectFirst("h2, h3, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("abs:href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${query.replace(" ", "+")}").document
        return doc.select("article.item").mapNotNull {
            val title = it.selectFirst("h2, h3")?.text() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("abs:href") ?: return@mapNotNull null
            newMovieSearchResponse(title, href, TvType.Movie)
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: return null
        return newMovieLoadResponse(title, url, TvType.Movie, url)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
