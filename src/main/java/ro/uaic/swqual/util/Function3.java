package ro.uaic.swqual.util;

/**
 * Interface used to accept a lambda with three parameters and a returned value
 * @param <T1> first parameter type
 * @param <T2> second parameter type
 * @param <T3> third parameter type
 * @param <R> returned type
 */
public interface Function3 <T1, T2, T3, R> {
    /**
     * Method that acts as the actual lambda invocation
     * @param t1 first parameter
     * @param t2 second parameter
     * @param t3 third parameter
     * @return value returned by the lambda
     */
    R apply(T1 t1, T2 t2, T3 t3);
}
