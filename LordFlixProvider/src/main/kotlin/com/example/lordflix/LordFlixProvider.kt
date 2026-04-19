package com.example.lordflix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class LordFlixProvider : MainAPI() {

    override var mainUrl            = "https://lordflix.org"
    override var name               = "LordFlix"
    override val hasMainPage        = true
    override var lang               = "en"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries)

    private val ua = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/movies/" to "Movies",
        "$mainUrl/series/" to "Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val doc = app.get(url, headers = ua).document
        val items = doc.select("article.item, .movie-item, .entry-box, div.item")
            .mapNotNull { it.toSearchResult() }
        val hasNext = doc.select("a.next, .next-page, .pagination .next").isNotEmpty()
        return newHomePageResponse(HomePageList(request.name, items, true), hasNext)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2, h3, .title, .entry-title, a[title]")
            ?.let { it.attr("title").ifEmpty { it.text() } }
            ?.trim() ?: return null
        val href = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img")
            ?.let { it.attr("src").ifEmpty { it.attr("data-src") } }
        val year = selectFirst(".year, .date, time")
            ?.text()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        return if (href.contains("series") || href.contains("season")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
                this.year = year
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                this.year = year
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get(
            "$mainUrl/?s=${query.replace(" ", "+")}",
            headers = ua
        ).document
        return doc.select("article.item, .movie-item, .entry-box, div.item")
            .mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, headers = ua).document
        val title = doc.selectFirst("h1.entry-title, h1.movie-title, h1")
            ?.text()?.trim() ?: return null
        val poster = doc.selectFirst(".poster img, .movie-poster img, article img")
            ?.let { it.attr("src").ifEmpty { it.attr("data-src") } }
        val description = doc.selectFirst(".description p, .story, .entry-content > p")
            ?.text()?.trim()
        val year = doc.selectFirst(".year, .date, time")
            ?.text()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        val tags = doc.select(".genre a, .genres a, .cats a").map { it.text() }
        val isSeries = doc.selectFirst(".episodes, .seasons-list, #episodes") != null
                || url.contains("series") || url.contains("season")
        return if (isSeries) {
            val episodes = doc.select(".episode-item a, li.ep a, .eps a, .episode a")
                .mapIndexed { i, el ->
                    newEpisode(el.attr("abs:href")) {
                        this.name    = el.text().ifEmpty { "Episode ${i + 1}" }
                        this.episode = i + 1
                    }
                }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot      = description
                this.year      = year
                this.tags      = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot      = description
                this.year      = year
                this.tags      = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = ua).document

        doc.select("iframe[src], iframe[data-src]").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotBlank()) loadExtractor(src, data, subtitleCallback, callback)
        }

        doc.select("source[src], video[src]").forEach { el ->
            val src = el.attr("src")
            if (src.contains(".m3u8") || src.contains(".mp4")) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name   = name,
                        url    = src
                    ) {
                        this.referer = mainUrl
                        this.quality = Qualities.Unknown.value
                        this.isM3u8  = src.contains(".m3u8")
                    }
                )
            }
        }

        val scripts = doc.select("script:not([src])").joinToString("\n") { it.data() }
        Regex("""["'](https?://[^"']+\.(?:m3u8|mp4)[^"']*)["']""")
            .findAll(scripts).forEach { match ->
                val videoUrl = match.groupValues[1]
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name   = name,
                        url    = videoUrl
                    ) {
                        this.referer = mainUrl
                        this.quality = Qualities.Unknown.value
                        this.isM3u8  = videoUrl.contains(".m3u8")
                    }
                )
            }

        return true
    }
}
