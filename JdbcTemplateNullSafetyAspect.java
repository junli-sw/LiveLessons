package com.yourcompany.db.aop;

import com.yourcompany.db.util.SafeStatementCreatorUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Global interceptor enforcing typed nulls for both JdbcTemplate
 * and NamedParameterJdbcTemplate operations.
 *
 * Works transparently – no DAO changes required.
 */
@Aspect
@Component
@Order(1)
public class JdbcNullSafetyAspect {

    // ---------------------------------------------------------------------
    // JdbcTemplate support
    // ---------------------------------------------------------------------
    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.update(..))")
    public Object aroundJdbcUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        // update(String sql, Object[] args, int[] argTypes)
        if (args.length >= 2 && args[1] instanceof Object[]) {
            Object[] params = (Object[]) args[1];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    params[i] = new SqlParameterValue(Types.VARCHAR, null);
                    SafeStatementCreatorUtils.logNull(i + 1, Types.VARCHAR);
                }
            }
        }
        return pjp.proceed(args);
    }

    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.batchUpdate(..))")
    public Object aroundJdbcBatchUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        // batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes)
        if (args.length >= 3 && args[1] instanceof List<?> && args[2] instanceof int[]) {
            @SuppressWarnings("unchecked")
            List<Object[]> batchArgs = (List<Object[]>) args[1];
            int[] argTypes = (int[]) args[2];

            for (Object[] row : batchArgs) {
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null) {
                        int type = (i < argTypes.length) ? argTypes[i] : Types.VARCHAR;
                        row[i] = new SqlParameterValue(type, null);
                        SafeStatementCreatorUtils.logNull(i + 1, type);
                    }
                }
            }
        }
        return pjp.proceed(args);
    }

    // ---------------------------------------------------------------------
    // NamedParameterJdbcTemplate support
    // ---------------------------------------------------------------------
    @Around("execution(* org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.update(..))")
    public Object aroundNamedUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        // update(String sql, Map<String, ?> paramMap)
        if (args.length >= 2 && args[1] instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramMap = (Map<String, Object>) args[1];
            paramMap.replaceAll((k, v) -> {
                if (v == null) {
                    SafeStatementCreatorUtils.logNamedParam(k, Types.VARCHAR);
                    return new SqlParameterValue(Types.VARCHAR, null);
                }
                return v;
            });
        }
        return pjp.proceed(args);
    }
}
