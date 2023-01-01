package com.hanialjti.allchat.presentation.paging

class MessagePagination<Key, Item>(
    private val initialKey: Key,
    private val onItemsUpdated: (Boolean) -> Unit,
    private val onAppend: (nextKey: Key) -> Result<List<Item>>,
    private val onPrepend: (prevKey: Key) -> Result<List<Item>>,
    private val getNextKey: (List<Item>) -> Key,
    private val getPrevKey: (List<Item>) -> Key,
    private val onError: (Throwable?) -> Unit,
    private val onSuccess: (items: List<Item>, nextKey: Key, prevKey: Key) -> Unit
) : Pagination<Key, Item> {

    override suspend fun loadNextPage() {
        TODO("Not yet implemented")
    }

    override suspend fun loadPrevPage() {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

}