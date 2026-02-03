package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.newExtractorLink

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils

import android.util.Log

open class Mp4Upload : ExtractorApi() {
    override var name = "Mp4Upload"
    override var mainUrl = "https://www.mp4upload.com"
    override val requiresReferer = true

    private val idMatch = Regex("""mp4upload\.com/(embed-|)([A-Za-z0-9]*)""")
    private val srcRegex = Regex("""src:\s*"([^"]+)"""")      //")
    // private val srcRegex = Regex("""player\.src\("(.*?)"""")
    // private val srcRegex2 = Regex("""player\.src\([\w\W]*src: "(.*?)"""")


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
    // override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val realUrl = idMatch.find(url)?.groupValues?.get(2)?.let { id ->
            "$mainUrl/embed-$id.html"
        } ?: url
        Log.d("CS3debug","  MP4Upload inputURL: $realUrl")
        val response = app.get(realUrl)
        if (response == null)  Log.d("CS3debug","  MP4Upload Cant retrieve: $realUrl")
        val unpackedText = getAndUnpack(response.text)
        Log.d("CS3debug","  MP4Upload HTML: $unpackedText")
        val res =
            unpackedText.lowercase().substringAfter(" height=").substringBefore(" ").toIntOrNull()

        srcRegex.find(unpackedText)?.groupValues?.get(1)?.let { link ->
            Log.d("CS3debug","decoded URL1: $link")
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = link
                ) {
                    this.referer = url
                    this.quality = res ?: Qualities.Unknown.value
                }
            )
            // return
        }


        // srcRegex.find(unpackedText)?.groupValues?.get(1)?.let { link ->
        //     Log.d("CS3debug","decoded URL1: $link")
        //     return listOf(
        //         newExtractorLink(
        //             name,
        //             name,
        //             link,
        //         ) {
        //             this.referer = url
        //             this.quality = quality ?: Qualities.Unknown.value
        //         }
        //     )
        // }
        // srcRegex2.find(unpackedText)?.groupValues?.get(1)?.let { link ->
        //     Log.d("CS3debug","decoded URL2: $link")
        //     return listOf(
        //         newExtractorLink(
        //             name,
        //             name,
        //             link,
        //         ) {
        //             this.referer = url
        //             this.quality = quality ?: Qualities.Unknown.value
        //         }
        //     )
        // }
        // return
    }
}