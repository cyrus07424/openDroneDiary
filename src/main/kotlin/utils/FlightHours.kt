package utils

import com.opendronediary.model.Aircraft
import com.opendronediary.model.FlightLog
import com.opendronediary.model.MaintenanceInspectionRecord

data class AircraftMaintenanceStatus(
    val aircraft: Aircraft,
    val totalMinutes: Int,
    val minutesSinceMaintenance: Int,
    val intervalMinutes: Int,
    val lastMaintenanceDate: String?,
    val hasMaintenanceRecord: Boolean
) {
    val remainingMinutes: Int = intervalMinutes - minutesSinceMaintenance

    val level: String = when {
        minutesSinceMaintenance >= intervalMinutes -> "overdue"
        remainingMinutes <= 120 -> "due_soon"
        !hasMaintenanceRecord -> "no_record"
        else -> "ok"
    }
}

object FlightHours {
    fun liveTotalMinutes(initialMinutes: Int, logs: List<FlightLog>): Int {
        val flown = logs.sumOf { flightMinutes(it) }
        return (initialMinutes.coerceAtLeast(0) + flown).coerceAtLeast(0)
    }

    /**
     * 当該飛行を含む、飛行日時点の製造後総飛行時間。
     * 同じ日の既存記録は、新規はすべて先行、更新時は小さいIDを先行とみなす。
     */
    fun snapshotMinutes(
        initialMinutes: Int,
        otherLogs: List<FlightLog>,
        thisMinutes: Int,
        thisDate: String,
        thisId: Int
    ): Int {
        val prior = otherLogs.filter { log ->
            log.flightDate < thisDate || (log.flightDate == thisDate && (thisId == 0 || log.id < thisId))
        }.sumOf { flightMinutes(it) }
        return initialMinutes.coerceAtLeast(0) + prior + thisMinutes.coerceAtLeast(0)
    }

    fun minutesSince(logs: List<FlightLog>, lastMaintenanceDate: String?): Int {
        val relevant = if (lastMaintenanceDate.isNullOrBlank()) {
            logs
        } else {
            logs.filter { it.flightDate > lastMaintenanceDate }
        }
        return relevant.sumOf { flightMinutes(it) }
    }

    fun statuses(
        aircraft: List<Aircraft>,
        logs: List<FlightLog>,
        maintenance: List<MaintenanceInspectionRecord>
    ): List<AircraftMaintenanceStatus> {
        return aircraft.map { craft ->
            val craftLogs = logs.filter { it.aircraftId == craft.id }
            val craftMaintenance = maintenance.filter { it.aircraftId == craft.id }
            val last = craftMaintenance.maxByOrNull { it.inspectionDate }
            val total = liveTotalMinutes(craft.initialTotalMinutes, craftLogs)
            val since = if (last == null) {
                total
            } else {
                minutesSince(craftLogs, last.inspectionDate)
            }
            AircraftMaintenanceStatus(
                aircraft = craft,
                totalMinutes = total,
                minutesSinceMaintenance = since,
                intervalMinutes = craft.maintenanceIntervalHours.coerceIn(1, 500) * 60,
                lastMaintenanceDate = last?.inspectionDate,
                hasMaintenanceRecord = last != null
            )
        }
    }

    fun flightMinutes(log: FlightLog): Int {
        return FlightTimeCalculator.durationToMinutes(log.totalFlightTime)
            ?: FlightTimeCalculator.durationToMinutes(log.flightDuration)
            ?: 0
    }
}
