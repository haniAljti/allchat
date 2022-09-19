package com.hanialjti.allchat.models

sealed class ListChange<out T>(val item: T) {
    class ItemAdded<T>(item: T): ListChange<T>(item)
    class ItemUpdated<T>(item: T): ListChange<T>(item)
    class ItemDeleted<T>(item: T): ListChange<T>(item)
}