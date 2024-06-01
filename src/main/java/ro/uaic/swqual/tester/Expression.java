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

/**
 * Represents an expression found in expectation comments (// expect-true {r0 == 4})
 *                                                                         ^~~~~~~
 * Will store required information, resolve upon parse, evaluate on {@link TesterParser} request and store the
 * state of the involved {@link Parameter Parameters} at the time of evaluation.
 */
public class Expression {
    /** Root {@link java.util.function.Predicate Predicate} used in the expression, resolves to a comparison */
    private final BiPredicate<Parameter, Parameter> predicate;
    /** Reference to the left-hand-side {@link Parameter} involved in the expression */
    private Parameter firstParam;
    /** Reference to the right-hand-side {@link Parameter} involved in the expression */
    private Parameter secondParam;
    /** Textual contents of the expression */
    private String code;

    /** Used in dumping the stored state of an evaluation.
     *  Traces back the names of the {@link Register} objects */
    private final Map<String, Register> namedReferences = new HashMap<>();
    /** Used in dumping the stored state of an evaluation.
     *  Traces back the at-the-time values of the {@link Register} objects */
    private final Map<Register, Constant> evaluatedValues = new HashMap<>();
    /** Used in dumping the stored state of an evaluation.
     *  Traces back the at-the-time memory values at accessed addresses in memory. */
    private final Map<Character, Constant> memoryValues = new HashMap<>();
    /** Used in dumping the stored state of an evaluation.
     *  Stores the evaluation result */
    private Boolean evaluatedAs = null;
    /** Used in dumping the stored state of an evaluation.
     *  Reads the memory values from accessible {@link ReadableMemoryUnit} that are part of an expression,
     *  upon its access. */
    private final List<Tuple3<ReadableMemoryUnit, Character, Character>> locationsToReadAddressesFrom = new ArrayList<>();

    /**
     * Represents the actual expression evaluation result.
     */
    public enum EvaluationType {
        /** Evaluated to true */
        TRUE,
        /** Evaluated to false */
        FALSE,
        /** Unable to discern binary state due to missing information (references) */
        UNKNOWN
    }

    /**
     * Method used to provide access to a {@link ReadableMemoryUnit} to store the memory state from
     * @param unit the unit in question
     * @param begin the start of the unit's address space
     * @param end the end of the unit's address space
     */
    public void readAddressesFrom(ReadableMemoryUnit unit, Character begin, Character end) {
        assert unit != null;
        assert begin != null;
        assert end != null;
        locationsToReadAddressesFrom.add(Tuple.of(unit, begin, end));
    }

    /**
     * Primary constructor
     * @param predicate root operation executed in the expression, as functional interface
     * @param parameters parameters involved in the expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    private <T extends Parameter> Expression(BiPredicate<Parameter, Parameter> predicate, Tuple2<T, T> parameters) {
        assert predicate != null;
        assert parameters != null;
        assert parameters.getFirst() != null;
        assert parameters.getSecond() != null;
        this.predicate = predicate;
        firstParam = parameters.getFirst();
        secondParam = parameters.getSecond();
    }

    /**
     * Factory method for constructing an equals-expression (r0 == 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression eq(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() == p1.getValue(), parameters);
    }

    /**
     * Factory method for constructing a not-equals-expression (r0 != 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression ne(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() != p1.getValue(), parameters);
    }

    /**
     * Factory method for constructing a less-than-expression (r0 < 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression lt(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() < p1.getValue(), parameters);
    }

    /**
     * Factory method for constructing a less-than-or-equal-expression (r0 <= 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression le(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() <= p1.getValue(), parameters);
    }

    /**
     * Factory method for constructing a greater-than-expression (r0 > 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression gt(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() > p1.getValue(), parameters);
    }

    /**
     * Factory method for constructing a greater-than-or-equal-expression (r0 >= 2).
     * @param parameters parameters involved in the expression
     * @return the newly created expression
     * @param <T> the concrete type of the parameters, extended from {@link Parameter}
     */
    public static <T extends Parameter> Expression ge(Tuple2<T, T> parameters) {
        return new Expression((p0, p1) -> p0.getValue() >= p1.getValue(), parameters);
    }

    /**
     * Method used to obtain an expression from a string describing it.
     * @param string the code of the expression (r0 == 4)
     * @return the parsed {@link Expression}. Returns null if was not able to create a valid expression.
     */
    public static Expression from(String string) {
        assert string != null;
        // split by compatible operations
        var pattern = Pattern.compile("(.*)(==|!=|>=|<=|<|>)(.*)");
        var matcher = pattern.matcher(string);
        if (!matcher.find()) {
            return null;
        }

        // Parse the parameters equivalent to the code parse
        var p0 = Parameter.parse(matcher.group(1).trim());
        var p1 = Parameter.parse(matcher.group(3).trim());
        // Match the results to the factory methods.
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

    /**
     * Method used to add the code that is representative of the expression.
     * @param code the text containing the code representation
     * @return Reference to self to be used in chaining operations
     */
    public Expression setCode(String code) {
        assert code != null;
        this.code = code;
        return this;
    }

    /**
     * Getter for the expression representative code.
     * @return string containing the code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Method used to resolve {@link RegisterReference} to {@link Register} used inside the expression.
     * @param registerMap the map from assembly labels to actual {@link Register} objects
     * @param hint the {@link Parameter} that is potentially a {@link RegisterReference}
     * @return resolved {@link Parameter}.
     */
    private Parameter resolve(Map<String, Register> registerMap, Parameter hint) {
        assert registerMap != null;
        assert hint != null;
        // if requested value is not a reference, return as-is
        if (!(hint instanceof RegisterReference ref)) {
            return hint;
        }

        var ident = registerMap.get(ref.getName());
        if (ident != null) {
            // also keep track of its name so it can be resolved later on dump
            namedReferences.put(ref.getName(), ident);
            return ident;
        }
        return hint;
    }

    /**
     * Method used to resolve {@link RegisterReference} to {@link Register} used inside the expression.
     * @param registerMap the map from assembly labels to actual {@link Register} objects
     */
    public void resolveReferences(Map<String, Register> registerMap) {
        assert registerMap != null;
        firstParam = resolve(registerMap, firstParam);
        secondParam = resolve(registerMap, secondParam);
    }

    /**
     * Method used to store the state of a {@link Parameter}.
     * @param param the parameter in question.
     */
    private void recordState(Parameter param) {
        assert param != null;
        if (param instanceof Register reg) {
            // if it is a register, store value directly
            evaluatedValues.put(reg, new Constant(reg.getValue()));
        } else if (param instanceof MemoryLocation loc) {
            // if it is a memory location, attempt to locate a ResolvedMemory to read from
            var located = locate(loc);
            if (located.isEmpty()) {
                return;
            }
            memoryValues.put(loc.getValue(), new Constant(located.get().getValue()));
        }
    }

    /**
     * Method used to acquire a {@link ResolvedMemory} based on a given {@link MemoryLocation} by searching the
     *   {@link Expression#locationsToReadAddressesFrom} for a matching {@link ReadableMemoryUnit} to provide it.
     * @param param location in question
     * @return a {@link ResolvedMemory} if located, {@link Optional#empty} otherwise.
     */
    private Optional<Parameter> locate(Parameter param) {
        // if not-MemoryLocation, return unchanged
        if (!(param instanceof MemoryLocation loc)) {
            return Optional.of(param);
        }

        var addr = loc.getValue();
        return locationsToReadAddressesFrom.stream()
                .filter(t -> t.getSecond() <= addr && addr < t.getThird())
                .findAny()
                .map(unitOffsetSize -> new ResolvedMemory(
                () -> unitOffsetSize.getFirst().read(
                        new ConstantMemoryLocation((char) (loc.getValue() - unitOffsetSize.getSecond()))
                ),
                null
        ));
    }

    /**
     * Method used to invoke expression evaluation and state storage.
     * @return evaluation result
     */
    public EvaluationType evaluate() {
        // first record the state of the values
        recordState(firstParam);
        recordState(secondParam);

        // identify and resolve locations
        var located0 = locate(firstParam);
        var located1 = locate(secondParam);

        // if resolving failed, unable to discern.
        if (located0.isEmpty() || located1.isEmpty()) {
            return UNKNOWN;
        }

        // execute the actual evaluation.
        evaluatedAs = predicate.test(located0.get(), located1.get());
        return Boolean.TRUE.equals(evaluatedAs) ? EvaluationType.TRUE : EvaluationType.FALSE;
    }

    /**
     * Method used to dump the stored state of the expression upon evaluation into a string
     * @return string containing the dump
     */
    public String dump() {
        if (Boolean.TRUE.equals(evaluatedAs)) {
            return "Correctly evaluated";
        }

        // only proceed with invalid evaluations
        var sb = new StringBuilder();
        var rsb = new StringBuilder();
        // collect register evaluation recorded states
        for (var refValue : namedReferences.entrySet()) {
            var ref = refValue.getValue();
            var evaluated = evaluatedValues.entrySet().stream().filter(e -> e.getKey() == ref).findFirst().orElse(null);
            rsb
                    .append(refValue.getKey())
                    .append(": ")
                    .append(evaluated == null ? "<unknown>" : (int) evaluated.getValue().getValue())
                    .append("; ");
        }
        // ignore register states if none were part of the expression.
        if (!rsb.isEmpty()) {
            sb.append("Used Registry State -> ").append(rsb);
        }

        var msb = new StringBuilder();
        // collect memory evaluation recorded states
        for (var memVal : memoryValues.entrySet()) {
            msb
                    .append("[0x")
                    .append(Integer.toString(memVal.getKey(), 16))
                    .append("]: ")
                    .append((int) memVal.getValue().getValue())
                    .append("; ");
        }
        // ignore memory states if none were a part of the expression
        if (!msb.isEmpty()) {
            sb.append("Used Memory State -> ").append(msb);
        }

        return sb.substring(0, sb.length() - 2);
    }
}
