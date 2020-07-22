package com.videodac.hls.helpers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AccountKeySpec(
    val type: String,
    val derivationPath: String? = null,
    val source: String? = null,
    val pwd: String? = null,
    val initPayload: String? = null,
    val name: String? = null
) : Parcelable