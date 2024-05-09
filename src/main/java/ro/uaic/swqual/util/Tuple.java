package ro.uaic.swqual.util;

public class Tuple {
    private Tuple() {}

    public static <T1> Tuple1<T1> of(T1 first) {
        return new Tuple1<>(first);
    }

    public static <T1, T2> Tuple2<T1, T2> of(T1 first, T2 second) {
        return new Tuple2<>(first, second);
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 first, T2 second, T3 third) {
        return new Tuple3<>(first, second, third);
    }
}
