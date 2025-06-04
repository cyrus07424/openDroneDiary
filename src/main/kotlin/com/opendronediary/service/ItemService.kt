package com.opendronediary.service

import com.opendronediary.model.Item
import com.opendronediary.repository.ItemRepository

class ItemService(private val repository: ItemRepository) {
    fun getAll(): List<Item> = repository.getAll()
    fun getById(id: Int): Item? = repository.getById(id)
    fun add(item: Item): Item = repository.add(item)
    fun update(id: Int, item: Item): Boolean = repository.update(id, item)
    fun delete(id: Int): Boolean = repository.delete(id)
}

