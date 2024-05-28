package ro.uaic.swqual.util;

import java.util.function.Function;

/**
 * Tuple of one value
 * @param <T1> Type of the `{@link Tuple1#first}` field
 */
public class Tuple1<T1> {
    /* First and only field of the tuple */
    private final T1 first;

    /**
     * Primary constructor
     * @param first value to be stored in the `{@link Tuple1#first}` field of the tuple
     */
    public Tuple1(T1 first) {
        this.first = first;
    }

    /**
     * Getter for the `{@link Tuple1#first}` field of the tuple
     * @return field value
     */
    public T1 getFirst() {
        return first;
    }

    /**
     * Method used to apply a lambda to the fields of the tuple
     * @param mapper lambda to be called with the fields
     * @return value the lambda returns
     * @param <R> type of the returned value
     */
    public <R> R map(Function<? super T1, R> mapper) {
        return mapper.apply(first);
    }
}
