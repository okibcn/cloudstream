package com.lagradost.cloudstream3.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.getCaptchaToken
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

import android.util.Log

open class Streamplay : ExtractorApi() {
    override val name = "Streamplay"
    override val mainUrl = "https://streamplay.to"
    override val requiresReferer = true

    private val idMatch = Regex("""streamplay\.to\/(embed-|)([A-Za-z0-9]*)""")
    private val realServer = "https://str4ampay.one"
    private val keyRegex = Regex("""sitekey:.*'(.*?)'""") 

    data class Source(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val realUrl = idMatch.find(url)?.groupValues?.get(2)?.let { id ->
            "$realServer/$id"
        } ?: url
        Log.d("CS3debug","  Streamplay inputURL: $realUrl")
        val page1 = app.get(
            realUrl,
            referrer = referer
        )
        captchaKey = keyRegex.find(page1.text)?.groupValues?.get(1) ?: ""
        Log.d("CS3debug","  Streamplay inputURL: $realUrl")
        Log.d("CS3debug","           captchaKey: $captchaKey")
        token = getCaptchaToken(
            realUrl,
            captchaKey,
            referer = url
        )
        Log.d("CS3debug","                Token: $token")

        val page2 = app.post(
            "$mainUrl/dl",
            data = mapOf(
                "op" to "download1",
                "id" to $id,
                "file_code" to id,
                "auto" to "1",
                "referer" to "$realUrl",
                "g-recaptcha-response" to token,
                "token" to token
            ),
            referer = url,
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Origin" to realServer,
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "same-origin",
                "Sec-Fetch-User" to "?1",
                "Upgrade-Insecure-Requests" to "1",
            )
        ).documentLarge
        Log.d("CS3debug","                HTML: $token")
        packedScript = page2.select("script").find { script ->
            script.data().contains("eval(function(p,a,c,k,e,d)")
        } ?: ""
        Log.d("CS3debug","              SCRIPT: ${packedScript.data()}")
       
        val data = getAndUnpack(packedScript.data()).substringAfter("sources=[").substringBefore(",desc")
            .replace("file", "\"file\"")
            .replace("label", "\"label\"")
        Log.d("CS3debug","       JSON: $data")      
        tryParseJson<List<Source>>("[$data}]")?.map { res ->
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    res.file ?: return@map null,
                ) {
                    this.referer = "$mainServer/"
                    this.quality = when (res.label) {
                        "HD" -> Qualities.P720.value
                        "SD" -> Qualities.P480.value
                        else -> Qualities.Unknown.value
                    }
                    this.headers = mapOf(
                        "Range" to "bytes=0-"
                    )
                }
            )
        }
    }
}









        // val request = app.get(url, referer = referer)
        // val redirectUrl = request.url
        // val mainServer = URI(redirectUrl).let {
        //     "${it.scheme}://${it.host}"
        // }
        // Log.d("CS3debug","  Streamplay inputURL: $url")
        // Log.d("CS3debug","             redirect: $redirectUrl")
        // Log.d("CS3debug","           mainserver: $mainServer")

        // val key = redirectUrl.substringAfterLast("/")
        // Log.d("CS3debug","                  key: $key")
        // val token =
        //     request.document.select("script").find { it.data().contains("sitekey:") }?.data()
        //         ?.substringAfterLast("sitekey: '")?.substringBefore("',")?.let { captchaKey ->
        //             Log.d("CS3debug","           captchakey: $captchaKey")
        //             getCaptchaToken(
        //                 redirectUrl,
        //                 captchaKey,
        //                 referer = "$mainServer/"
        //             )
        //         } ?: run {
        //             Log.d("CS3debug","           can't bypass captcha")
        //             throw ErrorLoadingException("can't bypass captcha") 
        //         }
        // Log.d("CS3debug","        captchatoken: $token")       
        // app.post(
        //     "$mainServer/player-$key-488x286.html", data = mapOf(
        //         "op" to "embed",
        //         "token" to token
        //     ),
        //     referer = redirectUrl,
        //     headers = mapOf(
        //         "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        //         "Content-Type" to "application/x-www-form-urlencoded"
        //     )
        // ).document.select("script").find { script ->
        //     script.data().contains("eval(function(p,a,c,k,e,d)")
        // }?.let {
        //     Log.d("CS3debug","        FOUND PACKED SCRIPT")       
        //     val data = getAndUnpack(it.data()).substringAfter("sources=[").substringBefore(",desc")
        //         .replace("file", "\"file\"")
        //         .replace("label", "\"label\"")
        //     Log.d("CS3debug","       JSON: $data")       
        //     tryParseJson<List<Source>>("[$data}]")?.map { res ->
        //         callback.invoke(
        //             newExtractorLink(
        //                 this.name,
        //                 this.name,
        //                 res.file ?: return@map null,
        //             ) {
        //                 this.referer = "$mainServer/"
        //                 this.quality = when (res.label) {
        //                     "HD" -> Qualities.P720.value
        //                     "SD" -> Qualities.P480.value
        //                     else -> Qualities.Unknown.value
        //                 }
        //                 this.headers = mapOf(
        //                     "Range" to "bytes=0-"
        //                 )
        //             }
        //         )
        //     }
        // }

