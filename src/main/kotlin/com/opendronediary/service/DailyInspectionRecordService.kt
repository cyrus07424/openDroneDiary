package com.opendronediary.service

import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.repository.DailyInspectionRecordRepository

class DailyInspectionRecordService(private val repository: DailyInspectionRecordRepository) {
    fun getAllByUserId(userId: Int): List<DailyInspectionRecord> = repository.getAllByUserId(userId)
    fun getByIdAndUserId(id: Int, userId: Int): DailyInspectionRecord? = repository.getByIdAndUserId(id, userId)
    fun add(dailyInspectionRecord: DailyInspectionRecord): DailyInspectionRecord = repository.add(dailyInspectionRecord)
    fun update(id: Int, dailyInspectionRecord: DailyInspectionRecord, userId: Int): Boolean = repository.update(id, dailyInspectionRecord, userId)
    fun delete(id: Int, userId: Int): Boolean = repository.delete(id, userId)
}