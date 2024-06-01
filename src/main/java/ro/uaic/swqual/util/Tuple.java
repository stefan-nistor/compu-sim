package ro.uaic.swqual.util;

/**
 * Utility class used to instantiate tuples through the overridden `{@link Tuple#of}` method
 */
public class Tuple {
    /**
     * Private constructor ensuring that this remains a utility class
     */
    private Tuple() {}

    /**
     * Method used to instantiate an appropriate tuple of number and types of parameters
     * @param first parameter that will act as the `{@link Tuple1#getFirst  first}` field of the tuple
     * @return instantiated tuple
     * @param <T1> type of the first parameter and of the `{@link Tuple1#getFirst first}` field of the tuple
     */
    public static <T1> Tuple1<T1> of(T1 first) {
        return new Tuple1<>(first);
    }

    /**
     * Method used to instantiate an appropriate tuple of number and types of parameters
     * @param first parameter that will act as the `{@link Tuple1#getFirst first}` field of the tuple
     * @param second parameter that will act as the `{@link Tuple2#getSecond second}` field of the tuple
     * @return instantiated tuple
     * @param <T1> type of the first parameter and of the `{@link Tuple1#getFirst first}` field of the tuple
     * @param <T2> type of the second parameter and of the `{@link Tuple2#getSecond second}` field of the tuple
     */
    public static <T1, T2> Tuple2<T1, T2> of(T1 first, T2 second) {
        return new Tuple2<>(first, second);
    }

    /**
     * Method used to instantiate an appropriate tuple of number and types of parameters
     * @param first parameter that will act as the `{@link Tuple1#getFirst first}` field of the tuple
     * @param second parameter that will act as the `{@link Tuple2#getSecond second}` field of the tuple
     * @param third parameter that will act as the `{@link Tuple3#getThird third}` field of the tuple
     * @return instantiated tuple
     * @param <T1> type of the first parameter and of the `{@link Tuple1#getFirst first}` field of the tuple
     * @param <T2> type of the second parameter and of the `{@link Tuple2#getSecond second}` field of the tuple
     * @param <T3> type of the second parameter and of the `{@link Tuple3#getThird third}` field of the tuple
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 first, T2 second, T3 third) {
        return new Tuple3<>(first, second, third);
    }
}
