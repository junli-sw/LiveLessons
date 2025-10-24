package com.yourcompany.db.aop;

import com.yourcompany.db.util.SafeStatementCreatorUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Global interceptor for JdbcTemplate to enforce null-safe parameter binding.
 */
@Aspect
@Component
@Order(1)
public class JdbcTemplateNullSafetyAspect {

    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.batchUpdate(..))")
    public Object aroundBatchUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        if (args.length >= 3 && args[1] instanceof List && args[2] instanceof int[]) {
            @SuppressWarnings("unchecked")
            List<Object[]> batchArgs = (List<Object[]>) args[1];
            int[] argTypes = (int[]) args[2];

            // pre-sanitize all nulls
            for (Object[] row : batchArgs) {
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null) {
                        // use default VARCHAR fallback
                        row[i] = new org.springframework.jdbc.core.SqlParameterValue(java.sql.Types.VARCHAR, null);
                        if (SafeStatementCreatorUtils.isLoggingEnabled()) {
                            SafeStatementCreatorUtils.logNull(i + 1, argTypes[i]);
                        }
                    }
                }
            }
        }
        return pjp.proceed(args);
    }

    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.update(..))")
    public Object aroundUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        // sanitize nulls in Object[] args if present
        if (args.length >= 2 && args[1] instanceof Object[]) {
            Object[] params = (Object[]) args[1];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    params[i] = new org.springframework.jdbc.core.SqlParameterValue(java.sql.Types.VARCHAR, null);
                    if (SafeStatementCreatorUtils.isLoggingEnabled()) {
                        SafeStatementCreatorUtils.logNull(i + 1, java.sql.Types.VARCHAR);
                    }
                }
            }
        }
        return pjp.proceed(args);
    }
}
