package com.yourcompany.db.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.lang.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Global null-safe JDBC parameter setter for Oracle.
 * Fixes ORA-17004 by enforcing setNull() with a known SQL type.
 */
public final class SafeStatementCreatorUtils {

    private static final Logger log = LoggerFactory.getLogger(SafeStatementCreatorUtils.class);

    // Toggle this if you ever want to silence the logs entirely
    private static final boolean LOG_NULL_TYPES = true;

    private SafeStatementCreatorUtils() {}

    // -------------------------------------------------------------------------
    // Core safe setter
    // -------------------------------------------------------------------------
    public static void setParameterValueSafely(
            PreparedStatement ps, int paramIndex, int sqlType, @Nullable Object inValue)
            throws SQLException {

        if (inValue == null) {
            int effectiveType = (sqlType == SqlTypeValue.TYPE_UNKNOWN)
                    ? Types.VARCHAR
                    : sqlType;
            ps.setNull(paramIndex, effectiveType);
            logNull(paramIndex, effectiveType);
        } else {
            StatementCreatorUtils.setParameterValue(ps, paramIndex, sqlType, null, inValue);
            if (LOG_NULL_TYPES && log.isTraceEnabled()) {
                log.trace("Param #{} = {} (SQL type: {})",
                        paramIndex, abbreviate(inValue), sqlTypeName(sqlType));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers used by AOP interceptors
    // -------------------------------------------------------------------------
    public static boolean isLoggingEnabled() {
        return LOG_NULL_TYPES;
    }

    public static void logNull(int paramIndex, int sqlType) {
        if (LOG_NULL_TYPES && log.isDebugEnabled()) {
            log.debug("Param #{} is NULL (SQL type resolved to: {})",
                    paramIndex, sqlTypeName(sqlType));
        }
    }

    public static void logNamedParam(String name, int sqlType) {
        if (LOG_NULL_TYPES && log.isDebugEnabled()) {
            log.debug("Named param '{}' is NULL (SQL type resolved to: {})",
                    name, sqlTypeName(sqlType));
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------
    private static String sqlTypeName(int sqlType) {
        switch (sqlType) {
            case Types.VARCHAR:   return "VARCHAR";
            case Types.NVARCHAR:  return "NVARCHAR";
            case Types.NUMERIC:   return "NUMERIC";
            case Types.INTEGER:   return "INTEGER";
            case Types.TIMESTAMP: return "TIMESTAMP";
            case Types.CLOB:      return "CLOB";
            default:              return String.valueOf(sqlType);
        }
    }

    private static String abbreviate(Object value) {
        String s = String.valueOf(value);
        return (s.length() > 60) ? s.substring(0, 57) + "..." : s;
    }
}
