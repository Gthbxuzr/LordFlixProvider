    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        
        // جلب الروابط من iframes
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotBlank()) loadExtractor(src, data, subtitleCallback, callback)
        }

        // جلب الروابط المباشرة (m3u8 / mp4)
        doc.select("source[src], video[src]").forEach { el ->
            val src = el.attr("src")
            if (src.contains(".m3u8") || src.contains(".mp4")) {
                callback.invoke(
                    ExtractorLink(
                        source  = name,
                        name    = name,
                        url     = src,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        type    = if (src.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    )
                )
            }
        }

        // البحث عن الروابط داخل السكريبتات
        val scripts = doc.select("script:not([src])").joinToString("\n") { it.data() }
        Regex("""["'](https?://[^"']+\.(?:m3u8|mp4)[^"']*)["']""")
            .findAll(scripts).forEach { match ->
                val videoUrl = match.groupValues[1]
                callback.invoke(
                    ExtractorLink(
                        source  = name,
                        name    = name,
                        url     = videoUrl,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        type    = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    )
                )
            }
        return true
    }
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
