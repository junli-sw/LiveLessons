@Aspect
@Component
@Order(1)
public class JdbcNullSafetyAspect {

  // -------- JdbcTemplate --------

  @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.update(..))")
  public Object aroundJdbcUpdate(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();

    // Handle only the 2-arg form: update(String sql, Object[] args)
    // If a 3rd argument exists and is int[] (types), DO NOT wrap with SqlParameterValue.
    boolean hasArgTypes = (args.length >= 3 && args[2] instanceof int[]);

    if (!hasArgTypes && args.length >= 2 && args[1] instanceof Object[]) {
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
            // With argTypes present, we can safely keep nulls as null, OR wrap—both work.
            // To be conservative, keep null and just log:
            SafeStatementCreatorUtils.logNull(i + 1, type);
          }
        }
      }
    }
    return pjp.proceed(args);
  }

  // -------- NamedParameterJdbcTemplate --------

  @Around("execution(* org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.update(..))")
  public Object aroundNamedUpdate(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();

    // Only handle the Map-based overload; do not alter SqlParameterSource overloads.
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
