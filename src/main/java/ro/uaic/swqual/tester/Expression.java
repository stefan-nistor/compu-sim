package ro.uaic.swqual.tester;

import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public class Expression {
    private final BiPredicate<Parameter, Parameter> predicate;
    private Parameter firstParam;
    private Parameter secondParam;

    private <T extends Parameter> Expression(BiPredicate<Parameter, Parameter> predicate, Tuple2<T, T> parameters) {
        this.predicate = predicate;
        firstParam = parameters.getFirst();
        secondParam = parameters.getSecond();
    }

    public static <T extends Parameter> Expression eq(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() == p1.getValue(), parameters);
    }

    public static <T extends Parameter> Expression ne(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() != p1.getValue(), parameters);
    }

    public static <T extends Parameter> Expression lt(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() < p1.getValue(), parameters);
    }

    public static <T extends Parameter> Expression le(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() <= p1.getValue(), parameters);
    }

    public static <T extends Parameter> Expression gt(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() > p1.getValue(), parameters);
    }

    public static <T extends Parameter> Expression ge(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() >= p1.getValue(), parameters);
    }

    private static Parameter asParameter(String string) {
        var numMatch = Pattern.compile("(\\d*)");
        var refMatch = Pattern.compile("([_a-zA-Z]\\w*)");
        if (numMatch.matcher(string).matches()) {
            return new Constant((char) Integer.parseInt(string));
        }

        if (refMatch.matcher(string).matches()) {
            return new RegisterReference(-1, string);
        }

        return null;
    }

    public static Expression from(String string) {
        var pattern = Pattern.compile("(.*)(==|!=|>|>=|<|<=)(.*)");
        var matcher = pattern.matcher(string);
        if (!matcher.find()) {
            return null;
        }

        var p0 = asParameter(matcher.group(1));
        var p1 = asParameter(matcher.group(3));
        var params = Tuple.of(p0, p1);
        return switch (matcher.group(2)) {
            case "==" -> eq(params);
            case "!=" -> ne(params);
            case "<" -> lt(params);
            case "<=" -> le(params);
            case ">" -> gt(params);
            case ">=" -> ge(params);
            default -> null;
        };
    }

    private static Parameter resolve(Map<String, Register> registerMap, Parameter hint) {
        if (!(hint instanceof RegisterReference ref)) {
            return hint;
        }

        var ident = registerMap.get(ref.getName());
        return ident == null ? hint : ident;
    }

    public void resolveReferences(Map<String, Register> registerMap) {
        firstParam = resolve(registerMap, firstParam);
        secondParam = resolve(registerMap, secondParam);
    }

    public boolean evaluate() {
        return predicate.test(firstParam, secondParam);
    }
}
