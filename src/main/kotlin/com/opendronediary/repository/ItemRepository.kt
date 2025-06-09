package com.opendronediary.repository

import com.opendronediary.model.Item

class ItemRepository {
    private val items = mutableListOf<Item>()
    private var nextId = 1

    fun getAllByUserId(userId: Int): List<Item> = items.filter { it.userId == userId }

    fun getByIdAndUserId(id: Int, userId: Int): Item? = items.find { it.id == id && it.userId == userId }

    fun add(item: Item): Item {
        val newItem = item.copy(id = nextId++)
        items.add(newItem)
        return newItem
    }

    fun update(id: Int, item: Item, userId: Int): Boolean {
        val index = items.indexOfFirst { it.id == id && it.userId == userId }
        if (index == -1) return false
        items[index] = item.copy(id = id, userId = userId)
        return true
    }

    fun delete(id: Int, userId: Int): Boolean = items.removeIf { it.id == id && it.userId == userId }
}

