package ro.uaic.swqual.tester;

import ro.uaic.swqual.mem.ReadableMemoryUnit;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.ConstantMemoryLocation;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.model.operands.ResolvedMemory;
import ro.uaic.swqual.util.Tuple;
import ro.uaic.swqual.util.Tuple2;
import ro.uaic.swqual.util.Tuple3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import static ro.uaic.swqual.tester.Expression.EvaluationType.UNKNOWN;

public class Expression {
    private final BiPredicate<Parameter, Parameter> predicate;
    private Parameter firstParam;
    private Parameter secondParam;
    private String code;
    private final Map<String, Register> namedReferences = new HashMap<>();
    private final Map<Register, Constant> evaluatedValues = new HashMap<>();
    private final Map<Character, Constant> memoryValues = new HashMap<>();
    private Boolean evaluatedAs = null;
    private final List<Tuple3<ReadableMemoryUnit, Character, Character>> locationsToReadAddressesFrom = new ArrayList<>();

    public enum EvaluationType {
        TRUE, FALSE, UNKNOWN
    }

    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        assert unit != null;
        assert begin != null;
        assert end != null;
        locationsToReadAddressesFrom.add(Tuple.of(unit, begin, end));
    }

    private <T extends Parameter> Expression(BiPredicate<Parameter, Parameter> predicate, Tuple2<T, T> parameters) {
        assert predicate != null;
        assert parameters != null;
        assert parameters.getFirst() != null;
        assert parameters.getSecond() != null;
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
        assert string != null;
        var pattern = Pattern.compile("(.*)(==|!=|>=|<=|<|>)(.*)");
        var matcher = pattern.matcher(string);
        if (!matcher.find()) {
            return null;
        }

        var p0 = Parameter.parse(matcher.group(1).trim());
        var p1 = Parameter.parse(matcher.group(3).trim());
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
        assert code != null;
        this.code = code;
        return this;
    }

    public String getCode() {
        return code;
    }

    private Parameter resolve(Map<String, Register> registerMap, Parameter hint) {
        assert registerMap != null;
        assert hint != null;
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
        assert registerMap != null;
        firstParam = resolve(registerMap, firstParam);
        secondParam = resolve(registerMap, secondParam);
    }

    private void recordState(Parameter param) {
        assert param != null;
        if (param instanceof Register reg) {
            evaluatedValues.put(reg, new Constant(reg.getValue()));
        } else if (param instanceof MemoryLocation loc) {
            var located = locate(loc);
            if (located.isEmpty()) {
                return;
            }
            memoryValues.put(loc.getValue(), new Constant(located.get().getValue()));
        }
    }

    private Optional<Parameter> locate(Parameter param) {
        if (!(param instanceof MemoryLocation loc)) {
            return Optional.of(param);
        }

        var addr = loc.getValue();
        var unit = locationsToReadAddressesFrom.stream().filter(t -> t.getSecond() <= addr && addr < t.getThird()).findAny();
        return unit.map(unitOffsetSize -> new ResolvedMemory(
                () -> unitOffsetSize.getFirst().read(
                        new ConstantMemoryLocation((char) (loc.getValue() - unitOffsetSize.getSecond()))
                ),
                null
        ));
    }

    public EvaluationType evaluate() {
        recordState(firstParam);
        recordState(secondParam);

        var located0 = locate(firstParam);
        var located1 = locate(secondParam);

        if (located0.isEmpty() || located1.isEmpty()) {
            return UNKNOWN;
        }

        evaluatedAs = predicate.test(located0.get(), located1.get());
        return Boolean.TRUE.equals(evaluatedAs) ? EvaluationType.TRUE : EvaluationType.FALSE;
    }

    public String dump() {
        if (Boolean.TRUE.equals(evaluatedAs)) {
            return "Correctly evaluated";
        }

        var sb = new StringBuilder();
        var rsb = new StringBuilder();
        for (var refValue : namedReferences.entrySet()) {
            var ref = refValue.getValue();
            var evaluated = evaluatedValues.entrySet().stream().filter(e -> e.getKey() == ref).findFirst().orElse(null);
            rsb
                    .append(refValue.getKey())
                    .append(": ")
                    .append(evaluated == null ? "<unknown>" : (int) evaluated.getValue().getValue())
                    .append("; ");
        }
        if (!rsb.isEmpty()) {
            sb.append("Used Registry State -> ").append(rsb);
        }

        var msb = new StringBuilder();
        for (var memVal : memoryValues.entrySet()) {
            msb
                    .append("[0x")
                    .append(Integer.toString(memVal.getKey(), 16))
                    .append("]: ")
                    .append((int) memVal.getValue().getValue())
                    .append("; ");
        }
        if (!msb.isEmpty()) {
            sb.append("Used Memory State -> ").append(msb);
        }

        return sb.substring(0, sb.length() - 2);
    }
}
