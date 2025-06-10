package com.opendronediary.service

import com.opendronediary.model.MaintenanceInspectionRecord
import com.opendronediary.repository.MaintenanceInspectionRecordRepository

class MaintenanceInspectionRecordService(private val repository: MaintenanceInspectionRecordRepository) {
    fun getAllByUserId(userId: Int): List<MaintenanceInspectionRecord> = repository.getAllByUserId(userId)
    fun getByIdAndUserId(id: Int, userId: Int): MaintenanceInspectionRecord? = repository.getByIdAndUserId(id, userId)
    fun add(maintenanceInspectionRecord: MaintenanceInspectionRecord): MaintenanceInspectionRecord = repository.add(maintenanceInspectionRecord)
    fun update(id: Int, maintenanceInspectionRecord: MaintenanceInspectionRecord, userId: Int): Boolean = repository.update(id, maintenanceInspectionRecord, userId)
    fun delete(id: Int, userId: Int): Boolean = repository.delete(id, userId)
}