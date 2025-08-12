package com.opendronediary.repository

import com.opendronediary.database.Pilots
import com.opendronediary.model.Pilot
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class PilotRepository {
    fun getAllByUserId(userId: Int): List<Pilot> {
        return Pilots.selectAll().where { Pilots.userId eq userId }
            .orderBy(Pilots.name)
            .map { resultRowToPilot(it) }
    }
    
    fun getByIdAndUserId(id: Int, userId: Int): Pilot? {
        return Pilots.selectAll().where { (Pilots.id eq id) and (Pilots.userId eq userId) }
            .map { resultRowToPilot(it) }
            .firstOrNull()
    }
    
    fun insert(pilot: Pilot): Pilot {
        val insertedId = Pilots.insert {
            it[name] = pilot.name
            it[userId] = pilot.userId
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        } get Pilots.id
        
        return pilot.copy(id = insertedId)
    }
    
    fun update(id: Int, pilot: Pilot, userId: Int): Boolean {
        val updatedRows = Pilots.update({ (Pilots.id eq id) and (Pilots.userId eq userId) }) {
            it[name] = pilot.name
            it[updatedAt] = LocalDateTime.now()
        }
        return updatedRows > 0
    }
    
    fun delete(id: Int, userId: Int): Boolean {
        val deletedRows = Pilots.deleteWhere { (Pilots.id eq id) and (Pilots.userId eq userId) }
        return deletedRows > 0
    }
    
    private fun resultRowToPilot(row: ResultRow): Pilot {
        return Pilot(
            id = row[Pilots.id],
            name = row[Pilots.name],
            userId = row[Pilots.userId],
            createdAt = row[Pilots.createdAt],
            updatedAt = row[Pilots.updatedAt]
        )
    }
}