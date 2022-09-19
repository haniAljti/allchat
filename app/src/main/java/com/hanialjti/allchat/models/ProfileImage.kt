package com.hanialjti.allchat.models

sealed class ProfileImage {
    class ImageUrl(val url: String): ProfileImage()
    class ImageByteArray(val bytes: String): ProfileImage()
}
