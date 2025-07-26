package com.opendronediary.repository

import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.database.DailyInspectionRecords
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DailyInspectionRecordRepository {
    
    fun getAllByUserId(userId: Int): List<DailyInspectionRecord> = transaction {
        DailyInspectionRecords.select { DailyInspectionRecords.userId eq userId }
            .map { 
                DailyInspectionRecord(
                    id = it[DailyInspectionRecords.id],
                    inspectionDate = it[DailyInspectionRecords.inspectionDate],
                    location = it[DailyInspectionRecords.location],
                    inspectorName = it[DailyInspectionRecords.inspectorName],
                    inspectionResult = it[DailyInspectionRecords.inspectionResult],
                    userId = it[DailyInspectionRecords.userId],
                    createdAt = it[DailyInspectionRecords.createdAt],
                    updatedAt = it[DailyInspectionRecords.updatedAt]
                )
            }
    }

    fun getByIdAndUserId(id: Int, userId: Int): DailyInspectionRecord? = transaction {
        DailyInspectionRecords.select { (DailyInspectionRecords.id eq id) and (DailyInspectionRecords.userId eq userId) }
            .map { 
                DailyInspectionRecord(
                    id = it[DailyInspectionRecords.id],
                    inspectionDate = it[DailyInspectionRecords.inspectionDate],
                    location = it[DailyInspectionRecords.location],
                    inspectorName = it[DailyInspectionRecords.inspectorName],
                    inspectionResult = it[DailyInspectionRecords.inspectionResult],
                    userId = it[DailyInspectionRecords.userId],
                    createdAt = it[DailyInspectionRecords.createdAt],
                    updatedAt = it[DailyInspectionRecords.updatedAt]
                )
            }
            .singleOrNull()
    }

    fun add(dailyInspectionRecord: DailyInspectionRecord): DailyInspectionRecord = transaction {
        val now = LocalDateTime.now()
        val insertedId = DailyInspectionRecords.insert {
            it[inspectionDate] = dailyInspectionRecord.inspectionDate
            it[location] = dailyInspectionRecord.location
            it[inspectorName] = dailyInspectionRecord.inspectorName
            it[inspectionResult] = dailyInspectionRecord.inspectionResult
            it[userId] = dailyInspectionRecord.userId
            it[createdAt] = now
            it[updatedAt] = now
        } get DailyInspectionRecords.id
        dailyInspectionRecord.copy(id = insertedId, createdAt = now, updatedAt = now)
    }

    fun update(id: Int, dailyInspectionRecord: DailyInspectionRecord, userId: Int): Boolean = transaction {
        val updateCount = DailyInspectionRecords.update(
            { (DailyInspectionRecords.id eq id) and (DailyInspectionRecords.userId eq userId) }
        ) {
            it[inspectionDate] = dailyInspectionRecord.inspectionDate
            it[location] = dailyInspectionRecord.location
            it[inspectorName] = dailyInspectionRecord.inspectorName
            it[inspectionResult] = dailyInspectionRecord.inspectionResult
            it[updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    fun delete(id: Int, userId: Int): Boolean = transaction {
        val deleteCount = DailyInspectionRecords.deleteWhere { 
            (DailyInspectionRecords.id eq id) and (DailyInspectionRecords.userId eq userId) 
        }
        deleteCount > 0
    }
}