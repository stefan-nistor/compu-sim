package ro.uaic.swqual.util;

public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> {
    public final T3 third;

    public Tuple3(T1 first, T2 second, T3 third) {
        super(first, second);
        this.third = third;
    }

    public T3 getThird() {
        return third;
    }

    public <R> R map(Function3<? super T1, ? super T2, ? super T3, R> mapper) {
        return mapper.apply(first, second, third);
    }
}
