package ro.uaic.swqual.util;

import java.util.function.BiFunction;

public class Tuple2<T1, T2> extends Tuple1<T1> {
    private final T2 second;

    public Tuple2(T1 first, T2 second) {
        super(first);
        this.second = second;
    }

    public T2 getSecond() {
        return second;
    }

    public <R> R map(BiFunction<T1, T2, R> mapper) {
        return mapper.apply(getFirst(), second);
    }
}
