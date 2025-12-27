package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResidentUnitLookupService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<AddressInfo> resolveByResident(UUID residentId, UUID unitId) {
        if (residentId == null && unitId == null) {
            return Optional.empty();
        }

        // Build dynamic query to avoid NULL parameter type issues in PostgreSQL
        StringBuilder query = new StringBuilder("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name,
                       hm.resident_id,
                       r.full_name
                FROM data.household_members hm
                JOIN data.households h ON h.id = hm.household_id
                JOIN data.units u ON u.id = h.unit_id
                JOIN data.buildings b ON b.id = u.building_id
                JOIN data.residents r ON r.id = hm.resident_id
                WHERE hm.left_at IS NULL
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (residentId != null) {
            query.append(" AND hm.resident_id = :residentId");
            params.addValue("residentId", residentId);
        }
        
        if (unitId != null) {
            query.append(" AND h.unit_id = :unitId");
            params.addValue("unitId", unitId);
        }
        
        query.append(" ORDER BY hm.is_primary DESC NULLS LAST, hm.created_at DESC NULLS LAST LIMIT 1");

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query.toString(), params);

        if (rowSet.next()) {
            return Optional.of(mapRow(rowSet));
        }

        if (unitId != null) {
            return resolveByUnit(unitId);
        }

        return Optional.empty();
    }

    public Optional<AddressInfo> resolveByUser(UUID userId, UUID unitId) {
        if (userId == null && unitId == null) {
            return Optional.empty();
        }

        // Build dynamic query to avoid NULL parameter type issues in PostgreSQL
        StringBuilder query = new StringBuilder("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name,
                       hm.resident_id,
                       r.full_name,
                       b.id AS building_id
                FROM data.residents r
                LEFT JOIN data.household_members hm ON hm.resident_id = r.id AND hm.left_at IS NULL
                LEFT JOIN data.households h ON h.id = hm.household_id
                LEFT JOIN data.units u ON u.id = h.unit_id
                LEFT JOIN data.buildings b ON b.id = u.building_id
                WHERE 1=1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (userId != null) {
            query.append(" AND r.user_id = :userId");
            params.addValue("userId", userId);
        }
        
        if (unitId != null) {
            query.append(" AND h.unit_id = :unitId");
            params.addValue("unitId", unitId);
        }
        
        query.append(" ORDER BY hm.is_primary DESC NULLS LAST, hm.created_at DESC NULLS LAST LIMIT 1");

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query.toString(), params);

        if (rowSet.next()) {
            AddressInfo info = mapRow(rowSet);
            // If we found a residentId, return it even if apartmentNumber/buildingName is null
            // This handles cases where Owner creates request but is not in household_members of that unit
            if (info.residentId() != null) {
                return Optional.of(info);
            }
            // If no residentId but has address info, return it
            if (info.apartmentNumber() != null || info.buildingName() != null) {
                return Optional.of(info);
            }
        }

        // Fallback: If no result from household_members join, query directly from residents table
        // This handles cases where Owner is not in household_members but exists in residents table
        if (userId != null) {
            MapSqlParameterSource fallbackParams = new MapSqlParameterSource();
            fallbackParams.addValue("userId", userId);
            
            SqlRowSet fallbackRowSet = jdbcTemplate.queryForRowSet("""
                    SELECT r.id AS resident_id,
                           r.full_name,
                           NULL AS apartment_number,
                           NULL AS building_name,
                           NULL AS building_id
                    FROM data.residents r
                    WHERE r.user_id = :userId
                    LIMIT 1
                    """, fallbackParams);
            
            if (fallbackRowSet.next()) {
                UUID residentId = getUuid(fallbackRowSet, "resident_id");
                if (residentId != null) {
                    log.debug("✅ [ResidentUnitLookupService] Found residentId {} directly from residents table for userId {}", residentId, userId);
                    return Optional.of(new AddressInfo(
                            null, // apartmentNumber
                            null, // buildingName
                            residentId, // residentId
                            getString(fallbackRowSet, "full_name"), // residentFullName
                            null // buildingId
                    ));
                }
            }
        }

        if (unitId != null) {
            return resolveByUnit(unitId);
        }

        return Optional.empty();
    }

    public Optional<AddressInfo> resolveByUnit(UUID unitId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("unitId", unitId);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name,
                       b.id AS building_id
                FROM data.units u
                LEFT JOIN data.buildings b ON b.id = u.building_id
                WHERE u.id = :unitId
                LIMIT 1
                """, params);

        if (rowSet.next()) {
            return Optional.of(new AddressInfo(
                    normalize(rowSet.getString("apartment_number")),
                    normalize(rowSet.getString("building_name")),
                    null,
                    null,
                    getUuid(rowSet, "building_id")
            ));
        }
        return Optional.empty();
    }
    
    public Optional<UUID> getBuildingIdFromUnitId(UUID unitId) {
        return resolveByUnit(unitId)
                .map(AddressInfo::buildingId);
    }
    
    private UUID getUuid(SqlRowSet rowSet, String column) {
        if (!hasColumn(rowSet, column)) {
            return null;
        }
        Object value = rowSet.getObject(column);
        if (value instanceof UUID uuid) {
            return uuid;
        } else if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return UUID.fromString(str.trim());
            } catch (IllegalArgumentException ignored) {
                log.debug("Cannot parse {} value {}", column, str);
            }
        }
        return null;
    }

    private AddressInfo mapRow(SqlRowSet rowSet) {
        UUID residentId = null;
        if (hasColumn(rowSet, "resident_id")) {
            Object value = rowSet.getObject("resident_id");
            if (value instanceof UUID uuid) {
                residentId = uuid;
            } else if (value instanceof String str && StringUtils.hasText(str)) {
                try {
                    residentId = UUID.fromString(str.trim());
                } catch (IllegalArgumentException ignored) {
                    log.debug("Cannot parse resident_id value {}", str);
                }
            }
        }
        UUID buildingId = getUuid(rowSet, "building_id");
        return new AddressInfo(
                normalize(getString(rowSet, "apartment_number")),
                normalize(getString(rowSet, "building_name")),
                residentId,
                normalize(getString(rowSet, "full_name")),
                buildingId
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasColumn(SqlRowSet rowSet, String column) {
        try {
            rowSet.findColumn(column);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getString(SqlRowSet rowSet, String column) {
        return hasColumn(rowSet, column) ? rowSet.getString(column) : null;
    }

    public record AddressInfo(String apartmentNumber, String buildingName, UUID residentId, String residentFullName, UUID buildingId) {
        public AddressInfo(String apartmentNumber, String buildingName, UUID residentId, String residentFullName) {
            this(apartmentNumber, buildingName, residentId, residentFullName, null);
        }
        
        public boolean hasAddress() {
            return StringUtils.hasText(apartmentNumber) || StringUtils.hasText(buildingName);
        }
    }
}


