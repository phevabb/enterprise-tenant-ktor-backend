package com.example.cloudinary

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils

object CloudinaryClient {

    val instance: Cloudinary by lazy {
        Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", "dshe6c8db",
                "api_key", "213798122585258",
                "api_secret", "ItL4DmcjTlTtAbdffHgwOrxBLPA"
            )
        )
    }
}