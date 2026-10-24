package cn.dytt.replica.data

import org.json.JSONArray
import org.json.JSONObject

data class Filter(val id: String, val name: String)

data class Filters(
    val type: List<Filter>,
    val cls: List<Filter>,
    val area: List<Filter>,
    val year: List<Filter>,
    val lang: List<Filter>,
    val sort: List<Filter>,
)

data class VodItem(
    val id: String,
    val name: String,
    val pic: String,
    val remarks: String,
    val typeId: Int,
    val score: String,
    val hits: String,
)

data class VodPage(
    val items: List<VodItem>,
    val total: Int,
    val totalPages: Int,
)

data class Banner(val vodId: String, val title: String, val pic: String)

data class PlayItem(val name: String, val url: String)

data class PlayLine(val key: String, val name: String, val type: Int, val items: List<PlayItem>)

data class VodDetail(
    val item: VodItem,
    val blurb: String,
    val content: String,
    val area: String,
    val year: String,
    val director: String,
    val actor: String,
    val lines: List<PlayLine>,
)

data class LiveItem(
    val id: String,
    val name: String,
    val type: String,
    val pic: String,
    val url: String,
)

data class ShortItem(
    val id: String,
    val title: String,
    val pic: String,
    val url: String,
    val player: String,
    val count: Int,
)

private fun JSONArray.toFilters(): List<Filter> = (0 until length()).map {
    val o = getJSONObject(it)
    Filter(o.optString("id"), o.optString("name"))
}

private fun JSONArray.toVodItems(): List<VodItem> = (0 until length()).map {
    val o = getJSONObject(it)
    VodItem(
        id = o.optString("vod_id"),
        name = o.optString("vod_name"),
        pic = o.optString("vod_pic"),
        remarks = o.optString("vod_remarks"),
        typeId = o.optInt("type_id"),
        score = o.optString("vod_score"),
        hits = o.optString("vod_hits"),
    )
}

private fun JSONObject.toVodPage(): VodPage = VodPage(
    items = (optJSONArray("list") ?: JSONArray()).toVodItems(),
    total = optInt("count"),
    totalPages = optInt("page_total"),
)

private fun JSONArray.toPlayLines(): List<PlayLine> = (0 until length()).map {
    val o = getJSONObject(it)
    val items = o.optJSONArray("list") ?: JSONArray()
    PlayLine(
        key = o.optString("key"),
        name = o.optString("name"),
        type = o.optInt("type"),
        items = (0 until items.length()).map { j ->
            val ep = items.getJSONObject(j)
            PlayItem(ep.optString("name"), ep.optString("url"))
        },
    )
}

private fun stripHtml(s: String): String = s
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace(Regex("<[^>]*>"), "")
    .trim()

object Repository {

    fun filters(): Filters {
        val o = JSONObject(ApiClient.get("/api/app/vodClass"))
        return Filters(
            type = o.optJSONArray("type").toFilters(),
            cls = o.optJSONArray("class").toFilters(),
            area = o.optJSONArray("area").toFilters(),
            year = o.optJSONArray("year").toFilters(),
            lang = o.optJSONArray("lang").toFilters(),
            sort = o.optJSONArray("sort").toFilters(),
        )
    }

    fun homeTypes(): List<Filter> {
        val o = JSONObject(ApiClient.get("/api/app/home"))
        val arr = o.optJSONArray("type") ?: return emptyList()
        return (0 until arr.length()).map {
            val o2 = arr.getJSONObject(it)
            Filter(o2.optString("id"), o2.optString("name"))
        }
    }

    fun banners(): List<Banner> {
        val arr = JSONArray(ApiClient.get("/api/app/slideshow"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Banner(o.optString("vod_id"), o.optString("title"), o.optString("pic"))
        }
    }

    fun vodList(
        page: Int,
        type: String = "",
        typePid: String = "",
        area: String = "",
        year: String = "",
        cls: String = "",
        lang: String = "",
        sort: String = "",
    ): VodPage {
        val o = JSONObject(
            ApiClient.get(
                "/api/app/vodList",
                mapOf(
                    "type" to type,
                    "type_pid" to typePid,
                    "area" to area,
                    "year" to year,
                    "class" to cls,
                    "lang" to lang,
                    "sort" to sort,
                    "page" to page.toString(),
                ),
            ),
        )
        return o.toVodPage()
    }

    fun search(keyword: String, page: Int): VodPage {
        val o = JSONObject(
            ApiClient.get(
                "/api/app/searchList",
                mapOf("wd" to keyword, "page" to page.toString()),
            ),
        )
        return o.toVodPage()
    }

    fun detail(id: String): VodDetail {
        val o = JSONObject(ApiClient.get("/api/vod/details", mapOf("id" to id)))
        val item = VodItem(
            id = o.optString("vod_id", id),
            name = o.optString("vod_name"),
            pic = o.optString("vod_pic"),
            remarks = o.optString("vod_remarks"),
            typeId = o.optInt("type_id"),
            score = o.optString("vod_score"),
            hits = o.optString("vod_hits"),
        )
        return VodDetail(
            item = item,
            blurb = stripHtml(o.optString("vod_blurb")),
            content = stripHtml(o.optString("vod_content")),
            area = o.optString("vod_area"),
            year = o.optString("vod_year"),
            director = o.optJSONArray("vod_director")?.join(",").orEmpty(),
            actor = o.optJSONArray("vod_actor")?.join(",").orEmpty(),
            lines = (o.optJSONArray("vod_play_url") ?: JSONArray()).toPlayLines(),
        )
    }

    /** 取播放地址。type=1 的线路是带签名与时间戳的缓存直链，播放前重新取一次可拿到新的有效期。 */
    fun play(id: String, player: String): PlayLine? {
        val o = JSONObject(ApiClient.get("/api/vod/play", mapOf("id" to id, "player" to player)))
        val items = o.optJSONArray("list") ?: JSONArray()
        return PlayLine(
            key = o.optString("key"),
            name = o.optString("name"),
            type = o.optInt("type"),
            items = (0 until items.length()).map { j ->
                val ep = items.getJSONObject(j)
                PlayItem(ep.optString("name"), ep.optString("url"))
            },
        )
    }

    fun lives(page: Int): List<LiveItem> {
        val o = JSONObject(ApiClient.get("/api/live/list", mapOf("page" to page.toString())))
        val list = o.optJSONObject("list") ?: return emptyList()
        val out = mutableListOf<LiveItem>()
        val groups = list.keys()
        while (groups.hasNext()) {
            val arr = list.optJSONArray(groups.next()) ?: continue
            for (i in 0 until arr.length()) {
                val o2 = arr.getJSONObject(i)
                out += LiveItem(
                    id = o2.optString("live_id"),
                    name = o2.optString("live_name"),
                    type = o2.optString("live_type"),
                    pic = o2.optString("live_pic"),
                    url = o2.optString("live_url"),
                )
            }
        }
        return out
    }

    fun shorts(page: Int): List<ShortItem> {
        val o = JSONObject(ApiClient.get("/api/short/list", mapOf("page" to page.toString())))
        val arr = o.optJSONArray("list") ?: JSONArray()
        return (0 until arr.length()).map {
            val o2 = arr.getJSONObject(it)
            ShortItem(
                id = o2.optString("short_id"),
                title = o2.optString("short_title"),
                pic = o2.optString("short_pic"),
                url = o2.optString("short_play_url"),
                player = o2.optString("short_player"),
                count = o2.optInt("short_play_count"),
            )
        }
    }
}
