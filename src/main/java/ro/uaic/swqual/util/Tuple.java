package ro.uaic.swqual.util;

public class Tuple {
    public static <LT1, LT2> Tuple2<LT1, LT2> of(LT1 first, LT2 second) {
        return new Tuple2<>(first, second);
    }

    public static <LT1, LT2, LT3> Tuple3<LT1, LT2, LT3> of(LT1 first, LT2 second, LT3 third) {
        return new Tuple3<>(first, second, third);
    }
}
