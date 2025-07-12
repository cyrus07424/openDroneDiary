package com.opendronediary.repository

import com.opendronediary.model.MaintenanceInspectionRecord
import com.opendronediary.database.MaintenanceInspectionRecords
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class MaintenanceInspectionRecordRepository {
    
    fun getAllByUserId(userId: Int): List<MaintenanceInspectionRecord> = transaction {
        MaintenanceInspectionRecords.select { MaintenanceInspectionRecords.userId eq userId }
            .map { 
                MaintenanceInspectionRecord(
                    id = it[MaintenanceInspectionRecords.id],
                    inspectionDate = it[MaintenanceInspectionRecords.inspectionDate],
                    location = it[MaintenanceInspectionRecords.location],
                    inspectorName = it[MaintenanceInspectionRecords.inspectorName],
                    contentAndReason = it[MaintenanceInspectionRecords.contentAndReason],
                    userId = it[MaintenanceInspectionRecords.userId]
                )
            }
    }

    fun getByIdAndUserId(id: Int, userId: Int): MaintenanceInspectionRecord? = transaction {
        MaintenanceInspectionRecords.select { (MaintenanceInspectionRecords.id eq id) and (MaintenanceInspectionRecords.userId eq userId) }
            .map { 
                MaintenanceInspectionRecord(
                    id = it[MaintenanceInspectionRecords.id],
                    inspectionDate = it[MaintenanceInspectionRecords.inspectionDate],
                    location = it[MaintenanceInspectionRecords.location],
                    inspectorName = it[MaintenanceInspectionRecords.inspectorName],
                    contentAndReason = it[MaintenanceInspectionRecords.contentAndReason],
                    userId = it[MaintenanceInspectionRecords.userId]
                )
            }
            .singleOrNull()
    }

    fun add(maintenanceInspectionRecord: MaintenanceInspectionRecord): MaintenanceInspectionRecord = transaction {
        val insertedId = MaintenanceInspectionRecords.insert {
            it[inspectionDate] = maintenanceInspectionRecord.inspectionDate
            it[location] = maintenanceInspectionRecord.location
            it[inspectorName] = maintenanceInspectionRecord.inspectorName
            it[contentAndReason] = maintenanceInspectionRecord.contentAndReason
            it[userId] = maintenanceInspectionRecord.userId
        } get MaintenanceInspectionRecords.id
        maintenanceInspectionRecord.copy(id = insertedId)
    }

    fun update(id: Int, maintenanceInspectionRecord: MaintenanceInspectionRecord, userId: Int): Boolean = transaction {
        val updateCount = MaintenanceInspectionRecords.update(
            { (MaintenanceInspectionRecords.id eq id) and (MaintenanceInspectionRecords.userId eq userId) }
        ) {
            it[inspectionDate] = maintenanceInspectionRecord.inspectionDate
            it[location] = maintenanceInspectionRecord.location
            it[inspectorName] = maintenanceInspectionRecord.inspectorName
            it[contentAndReason] = maintenanceInspectionRecord.contentAndReason
        }
        updateCount > 0
    }

    fun delete(id: Int, userId: Int): Boolean = transaction {
        val deleteCount = MaintenanceInspectionRecords.deleteWhere { 
            (MaintenanceInspectionRecords.id eq id) and (MaintenanceInspectionRecords.userId eq userId) 
        }
        deleteCount > 0
    }
}