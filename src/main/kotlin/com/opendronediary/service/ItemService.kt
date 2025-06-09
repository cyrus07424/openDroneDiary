package com.opendronediary.service

import com.opendronediary.model.Item
import com.opendronediary.repository.ItemRepository

class ItemService(private val repository: ItemRepository) {
    fun getAllByUserId(userId: Int): List<Item> = repository.getAllByUserId(userId)
    fun getByIdAndUserId(id: Int, userId: Int): Item? = repository.getByIdAndUserId(id, userId)
    fun add(item: Item): Item = repository.add(item)
    fun update(id: Int, item: Item, userId: Int): Boolean = repository.update(id, item, userId)
    fun delete(id: Int, userId: Int): Boolean = repository.delete(id, userId)
}

