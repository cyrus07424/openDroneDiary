package com.opendronediary.repository

import com.opendronediary.model.DailyInspectionRecord

class DailyInspectionRecordRepository {
    private val dailyInspectionRecords = mutableListOf<DailyInspectionRecord>()
    private var nextId = 1

    fun getAllByUserId(userId: Int): List<DailyInspectionRecord> = dailyInspectionRecords.filter { it.userId == userId }

    fun getByIdAndUserId(id: Int, userId: Int): DailyInspectionRecord? = dailyInspectionRecords.find { it.id == id && it.userId == userId }

    fun add(dailyInspectionRecord: DailyInspectionRecord): DailyInspectionRecord {
        val newRecord = dailyInspectionRecord.copy(id = nextId++)
        dailyInspectionRecords.add(newRecord)
        return newRecord
    }

    fun update(id: Int, dailyInspectionRecord: DailyInspectionRecord, userId: Int): Boolean {
        val index = dailyInspectionRecords.indexOfFirst { it.id == id && it.userId == userId }
        if (index == -1) return false
        dailyInspectionRecords[index] = dailyInspectionRecord.copy(id = id, userId = userId)
        return true
    }

    fun delete(id: Int, userId: Int): Boolean = dailyInspectionRecords.removeIf { it.id == id && it.userId == userId }
}