package com.opendronediary.repository

import com.opendronediary.model.MaintenanceInspectionRecord

class MaintenanceInspectionRecordRepository {
    private val maintenanceInspectionRecords = mutableListOf<MaintenanceInspectionRecord>()
    private var nextId = 1

    fun getAllByUserId(userId: Int): List<MaintenanceInspectionRecord> = maintenanceInspectionRecords.filter { it.userId == userId }

    fun getByIdAndUserId(id: Int, userId: Int): MaintenanceInspectionRecord? = maintenanceInspectionRecords.find { it.id == id && it.userId == userId }

    fun add(maintenanceInspectionRecord: MaintenanceInspectionRecord): MaintenanceInspectionRecord {
        val newRecord = maintenanceInspectionRecord.copy(id = nextId++)
        maintenanceInspectionRecords.add(newRecord)
        return newRecord
    }

    fun update(id: Int, maintenanceInspectionRecord: MaintenanceInspectionRecord, userId: Int): Boolean {
        val index = maintenanceInspectionRecords.indexOfFirst { it.id == id && it.userId == userId }
        if (index == -1) return false
        maintenanceInspectionRecords[index] = maintenanceInspectionRecord.copy(id = id, userId = userId)
        return true
    }

    fun delete(id: Int, userId: Int): Boolean = maintenanceInspectionRecords.removeIf { it.id == id && it.userId == userId }
}