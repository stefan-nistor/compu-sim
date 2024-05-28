package ro.uaic.swqual.util;

/**
 * Tuple of three values. Effectively extends a tuple of two values
 * @param <T1> Type of the `{@link Tuple1#getFirst first}` field.
 * @param <T2> Type of the `{@link Tuple2#getSecond second}` field.
 * @param <T3> Type of the `{@link Tuple3#third}` field.
 */
public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> {
    /* Third field of the tuple */
    private final T3 third;


    /**
     * Primary constructor
     * @param first value to be stored in the `{@link Tuple1#getFirst first}` field of the tuple
     * @param second value to be stored in the `{@link Tuple2#getSecond second}` field of the tuple
     * @param third value to be stored in the `{@link Tuple3#third}` field of the tuple
     */
    public Tuple3(T1 first, T2 second, T3 third) {
        super(first, second);
        this.third = third;
    }

    /**
     * Getter for the `{@link Tuple3#third}` field of the tuple
     * @return field value
     */
    public T3 getThird() {
        return third;
    }

    /**
     * Method used to apply a lambda to the fields of the tuple
     * @param mapper lambda to be called with the fields
     * @return value the lambda returns
     * @param <R> type of the returned value
     */
    public <R> R map(Function3<? super T1, ? super T2, ? super T3, R> mapper) {
        return mapper.apply(getFirst(), getSecond(), third);
    }
}
