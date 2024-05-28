package ro.uaic.swqual.util;

import java.util.function.BiFunction;

/**
 * Tuple of two values. Effectively extends a tuple of one value
 * @param <T1> Type of the `{@link Tuple1#getFirst first}` field.
 * @param <T2> Type of the `{@link Tuple2#second}` field.
 */
public class Tuple2<T1, T2> extends Tuple1<T1> {
    /* Second field of the tuple */
    private final T2 second;

    /**
     * Primary constructor
     * @param first value to be stored in the `{@link Tuple1#getFirst first}` field of the tuple
     * @param second value to be stored in the `{@link Tuple2#second}` field of the tuple
     */
    public Tuple2(T1 first, T2 second) {
        super(first);
        this.second = second;
    }

    /**
     * Getter for the `{@link Tuple2#second}` field of the tuple
     * @return field value
     */
    public T2 getSecond() {
        return second;
    }

    /**
     * Method used to apply a lambda to the fields of the tuple
     * @param mapper lambda to be called with the fields
     * @return value the lambda returns
     * @param <R> type of the returned value
     */
    public <R> R map(BiFunction<T1, T2, R> mapper) {
        return mapper.apply(getFirst(), second);
    }
}
