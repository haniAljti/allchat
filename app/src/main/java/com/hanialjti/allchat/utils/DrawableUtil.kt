package com.hanialjti.allchat.utils

import com.hanialjti.allchat.R

fun getDefaultDrawableRes(isGroupChat: Boolean) =
    if (isGroupChat) R.drawable.ic_group else R.drawable.ic_user