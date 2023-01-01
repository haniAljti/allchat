package com.hanialjti.allchat.presentation.paging

interface Pagination<Key, Item> {
    suspend fun loadNextPage()
    suspend fun loadPrevPage()
    fun reset()
}