package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.helper.JwPlayerHelper
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink

import com.lagradost.cloudstream3.utils.M3u8Helper

class Vidmolyme : Vidmoly() {
    override val mainUrl = "https://vidmoly.me"
}

class Vidmolyto : Vidmoly() {
    override val mainUrl = "https://vidmoly.to"
}

class Vidmolybiz : Vidmoly() {
    override val mainUrl = "https://vidmoly.biz"
}

open class Vidmoly : ExtractorApi() {
    override val name = "Vidmoly"
    override val mainUrl = "https://vidmoly.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "user-agent" to USER_AGENT,
            "Sec-Fetch-Dest" to "iframe"
        )
        
        val newUrl =
            if (url.contains("/w/") || url.contains("/v/"))
                url.replaceFirst("/w/", "/embed-")
                .replaceFirst("/v/", "/embed-") + ".html"
            else url
        println("HDFull Vidmoly: $newUrl")  // DEBUG 


        val script = app.get(newUrl, headers = headers, referer = referer)
            .document.select("script")
            .replace("'","\"")
            .firstOrNull { it.data().contains("sources:") }
            ?.data()
        val regex = Regex("""file:\s*'(https[^']+)'""")
        val match = regex.find(script ?: "")
        val m3u8Url = match?.groupValues?.get(1)
        println("HDFull Vidmoly: $m3u8Url")  // DEBUG 

        
        // Extracts and parses videoData
        JwPlayerHelper.extractStreamLinks(script.orEmpty(), name, mainUrl, callback, subtitleCallback)
        // if (cleanUrl.contains(".m3u8") || cleanUrl.contains(".txt")) {
        //     try {
        //         println("HDFull VidmolyOK: $cleanUrl")  // DEBUG 
        //         M3u8Helper.generateM3u8(
        //             source = name,
        //             streamUrl = cleanUrl,
        //             referer = mainUrl,
        //             headers = headers,
        //         )
        //     } catch (e: Exception) {
        //         Log.d("JW_PLAYER_HELPER", "Error generating M3U8 links: ${e.message}")
        //         emptyList()
        //     }
        // } else {
        //     listOf(
        //         newExtractorLink(
        //             source = sourceName,
        //             name = sourceName,
        //             url = fixUrl(cleanUrl, mainUrl),
        //         ) {
        //             this.referer = url
        //             this.headers = headers
        //         }
        //     )
        // }

    }
}
