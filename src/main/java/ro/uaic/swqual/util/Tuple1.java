package ro.uaic.swqual.util;

import java.util.function.Function;

public class Tuple1<T1> {
    private final T1 first;

    public Tuple1(T1 first) {
        this.first = first;
    }

    public T1 getFirst() {
        return first;
    }

    public <R> R map(Function<? super T1, R> mapper) {
        return mapper.apply(first);
    }
}
