package com.videodac.hls.helpers

import com.videodac.hls.services.Status

object StatusHelper {

    @JvmStatic
    internal var status: Status? = null

    @JvmStatic
    internal lateinit var channels: MutableList<String>

    @JvmStatic
    internal var threeBoxAvatarUris : HashMap<String, String> = HashMap ()

    @JvmStatic
    internal var threeBoxNames : HashMap<String, String> = HashMap ()

    @JvmStatic
    internal var ensNames : HashMap<String, String> = HashMap ()

}