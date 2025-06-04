package com.opendronediary.repository

import com.opendronediary.model.Item

class ItemRepository {
    private val items = mutableListOf<Item>()
    private var nextId = 1

    fun getAll(): List<Item> = items

    fun getById(id: Int): Item? = items.find { it.id == id }

    fun add(item: Item): Item {
        val newItem = item.copy(id = nextId++)
        items.add(newItem)
        return newItem
    }

    fun update(id: Int, item: Item): Boolean {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return false
        items[index] = item.copy(id = id)
        return true
    }

    fun delete(id: Int): Boolean = items.removeIf { it.id == id }
}

