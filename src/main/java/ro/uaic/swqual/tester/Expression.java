package ro.uaic.swqual.tester;

import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public class Expression {
    private final BiPredicate<Parameter, Parameter> predicate;
    private Parameter firstParam;
    private Parameter secondParam;
    private String code;
    private final Map<String, Register> namedReferences = new HashMap<>();
    private final Map<Register, Constant> evaluatedValues = new HashMap<>();
    private Boolean evaluatedAs = null;

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

    public static Expression from(String string) {
        var pattern = Pattern.compile("(.*)(==|!=|>=|<=|<|>)(.*)");
        var matcher = pattern.matcher(string);
        if (!matcher.find()) {
            return null;
        }

        var p0 = Parameter.parse(matcher.group(1).trim());
        var p1 = Parameter.parse(matcher.group(3).trim());
        if (p0 == null || p1 == null) {
            return null;
        }

        var params = Tuple.of(p0, p1);
        return switch (matcher.group(2)) {
            case "==" -> eq(params).setCode(string);
            case "!=" -> ne(params).setCode(string);
            case "<" -> lt(params).setCode(string);
            case "<=" -> le(params).setCode(string);
            case ">" -> gt(params).setCode(string);
            case ">=" -> ge(params).setCode(string);
            default -> null;
        };
    }

    public Expression setCode(String code) {
        this.code = code;
        return this;
    }

    public String getCode() {
        return code;
    }

    private Parameter resolve(Map<String, Register> registerMap, Parameter hint) {
        if (!(hint instanceof RegisterReference ref)) {
            return hint;
        }

        var ident = registerMap.get(ref.getName());
        if (ident != null) {
            namedReferences.put(ref.getName(), ident);
            return ident;
        }
        return hint;
    }

    public void resolveReferences(Map<String, Register> registerMap) {
        firstParam = resolve(registerMap, firstParam);
        secondParam = resolve(registerMap, secondParam);
    }

    private void recordState(Parameter param) {
        if (!(param instanceof Register reg)) {
            return;
        }

        evaluatedValues.put(reg, new Constant(reg.getValue()));
    }

    public boolean evaluate() {
        recordState(firstParam);
        recordState(secondParam);
        evaluatedAs = predicate.test(firstParam, secondParam);
        return evaluatedAs;
    }

    public String dump() {
        if (Boolean.TRUE.equals(evaluatedAs)) {
            return "Correctly evaluated";
        }

        var sb = new StringBuilder().append("Current state -> ");
        for (var refValue : namedReferences.entrySet()) {
            var ref = refValue.getValue();
            var evaluated = evaluatedValues.entrySet().stream().filter(e -> e.getKey() == ref).findFirst().orElse(null);
            sb
                    .append(refValue.getKey())
                    .append(": ")
                    .append(evaluated == null ? "<unknown>" : (int) evaluated.getValue().getValue())
                    .append("; ");
        }
        return sb.substring(0, sb.length() - 2);
    }
}
