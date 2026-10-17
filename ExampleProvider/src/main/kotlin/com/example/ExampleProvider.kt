package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

// Токен VK хранится здесь в памяти. Вводится в настройках плагина (шестерёнка),
// в код и в GitHub его вставлять НЕ нужно.
object VkConfig {
    @Volatile
    var token: String = ""
}

class RuCatalogProvider : MainAPI() {
    override var mainUrl = "https://vk.com"
    override var name = "RU Каталог (VK)"
    override var lang = "ru"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    private val vkApi = "https://api.vk.com/method/video.search"
    private val vkReferer = "https://vk.com/"
    private val userAgent =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    override val mainPage = mainPageOf(
        "русские фильмы" to "Русские фильмы",
        "советские фильмы" to "Советские фильмы",
        "комедия фильм" to "Комедии",
        "боевик фильм" to "Боевики",
        "драма фильм" to "Драмы",
        "мультфильм" to "Мультфильмы",
    )

    private suspend fun vkSearch(
        query: String,
        offset: Int,
        count: Int,
        longer: Int,
        hdOnly: Boolean
    ): List<VkVideo> {
        if (VkConfig.token.isBlank()) {
            throw ErrorLoadingException("Нет токена VK. Откройте настройки плагина (шестерёнка) и вставьте токен.")
        }
        val params = mutableMapOf(
            "q" to query,
            "count" to count.toString(),
            "offset" to offset.toString(),
            "longer" to longer.toString(),
            "adult" to "0",
            "sort" to "2",
            "access_token" to VkConfig.token,
            "v" to "5.199"
        )
        if (hdOnly) params["hd"] = "1"
        val res = parseJson<VkResponse>(app.get(vkApi, params = params).text)
        return res.response?.items
            ?: throw ErrorLoadingException(res.error?.errorMsg ?: "VK ничего не вернул")
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val count = 30
        val items = vkSearch(request.data, (page - 1) * count, count, 1800, true)
            .mapNotNull { it.toSearch() }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return vkSearch(query, 0, 30, 600, false).mapNotNull { it.toSearch() }
    }

    private fun VkVideo.bestImage(): String? {
        val list = this.image ?: return null
        return list.filter { (it.width ?: 0) >= 300 }.minByOrNull { it.width ?: 0 }?.url
            ?: list.lastOrNull()?.url
    }

    private fun VkVideo.toSearch(): SearchResponse? {
        val itemTitle = this.title ?: return null
        val playerUrl = this.player ?: return null
        val itemData = VkItemData(
            title = itemTitle,
            poster = this.bestImage(),
            player = playerUrl,
            description = this.description,
            duration = this.duration
        )
        return newMovieSearchRespon
