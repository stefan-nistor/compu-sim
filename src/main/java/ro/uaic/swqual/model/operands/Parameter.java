package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.exception.ValueException;

import java.util.ArrayList;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;

/**
 * Represents an abstract value that can be part of an {@link ro.uaic.swqual.model.Instruction Instruction}
 */
public abstract class Parameter {
    /** {@link Pattern} matching a base 2 integral constant */
    private static final Pattern CONSTANT_BASE_2_PATTERN = Pattern.compile("#?0[bB]([0-1]+)");
    /** {@link Pattern} matching a base 8 integral constant */
    private static final Pattern CONSTANT_BASE_8_PATTERN = Pattern.compile("#?0([0-7]+)");
    /** {@link Pattern} matching a base 10 integral constant */
    private static final Pattern CONSTANT_BASE_10_PATTERN = Pattern.compile("#?(0|[1-9]\\d*)");
    /** {@link Pattern} matching a base 16 integral constant */
    private static final Pattern CONSTANT_BASE_16_PATTERN = Pattern.compile("#?0[xX]([0-9a-fA-F]+)");
    /** {@link Pattern} matching an identifier */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[_a-zA-Z]\\w*");

    /**
     * Base value for any parameter. These should be unsigned short by rule (16 bit unsigned).
     * As it happens, char = unsigned short.
     **/
    protected char value;

    /**
     * Default value getter. Will return default value unless overridden
     * @return {@link Parameter#value}
     */
    public char getValue() {
        return value;
    }

    /**
     * Default value setter. Parameters are not writeable unless explicitly stated, so by default, this throws.
     * @param value unused. In writeable parameters, it will represent the value to be stored in the parameter.
     * @throws ParameterException by default.
     */
    public void setValue(char value) throws ParameterException {
        // By default, make all Parameter types non-writeable
        throw new ParameterException("Attempted write in a non-writeable parameter");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return value == parameter.value;
    }

    /**
     * Method used to attempt to create a {@link Parameter} from a {@link String} of the appropriate instance.
     * @param string where to extract a from
     * @return Newly created {@link Parameter}, if parse resulted in a valid {@link Parameter}. Otherwise, null
     */
    public static Parameter parse(String string) {
        return parse(0, string);
    }

    /**
     * Method used to attempt to create a {@link Constant} from a {@link String}.
     * @param constantStr where to extract from
     * @return Newly created {@link Constant}, if parse resulted in a valid {@link Constant}. Otherwise, null
     */
    private static Constant identifyConstant(String constantStr) {
        var b2Match = CONSTANT_BASE_2_PATTERN.matcher(constantStr);
        var b8Match = CONSTANT_BASE_8_PATTERN.matcher(constantStr);
        var b10Match = CONSTANT_BASE_10_PATTERN.matcher(constantStr);
        var b16Match = CONSTANT_BASE_16_PATTERN.matcher(constantStr);

        if (b2Match.matches()) {
            return new Constant((char) Integer.parseInt(b2Match.group(1), 2));
        }

        if (b8Match.matches()) {
            return new Constant((char) Integer.parseInt(b8Match.group(1), 8));
        }

        if (b10Match.matches()) {
            return new Constant((char) Integer.parseInt(b10Match.group(1), 10));
        }

        if (b16Match.matches()) {
            return new Constant((char) Integer.parseInt(b16Match.group(1), 16));
        }

        return null;
    }

    /**
     * Method used to parse a {@link MemoryLocation} from a {@link String}.
     * @param lineIndex line location (optional)
     * @param text where to extract from
     * @return Newly created {@link MemoryLocation} of appropriate instance, if parse resulted in a concrete
     * {@link MemoryLocation}. Otherwise, null.
     * @throws ParameterException when cannot resolve a {@link MemoryLocation}
     */
    private static Parameter parseAddress(int lineIndex, String text) throws ParameterException {
        // Memory Locations are parameters and relations:
        // [r0 + 24 - r1] is:
        // - paramList: r0, 24, r1.
        // - relList: sum, sub.
        var paramList = new ArrayList<Parameter>();
        var relList = new ArrayList<BinaryOperator<Character>>();

        // TODO: come up with a better way. This only covers +-, and cannot be expanded with operator precedence or braces.
        // Preserve delimiters, so split with them included
        for (var token : text.splitWithDelimiters("[+-]", Integer.MAX_VALUE)) {
            token = token.strip();
            // Mutate operator to appropriate BinaryOperator interface
            // If no appropriate operator matches, it is a parameter, not a relation.
            if (token.equals("+")) {
                relList.add((a, b) -> (char)(a + b));
            } else if (token.equals("-")) {
                relList.add((a, b) -> (char)(a - b));
            } else {
                paramList.add(parse(lineIndex, token));
            }
        }

        // If there are no relations, this either a constant ([0x200]) or an absolute ([r0]) memory location
        if (paramList.size() == 1) {
            var param = paramList.getFirst();
            if (param instanceof Constant con) {
                return new ConstantMemoryLocation(con.getValue());
            }
            return new AbsoluteMemoryLocation(param);
        }

        // Otherwise, it must be a relative memory location ([r0 + 0x200]).
        try {
            return new RelativeMemoryLocation(paramList, relList);
        } catch (ValueException exception) {
            // If none, raise exception.
            throw new ParameterException(exception);
        }
    }

    /**
     * Method used to attempt to create a {@link Parameter} from a {@link String} of the appropriate instance.
     * It also allows tracking the line index of the string (for any {@link Parameter} objects making use of the line index).
     * @param lineIdx current line index in a multi-line context.
     * @param string where to extract a from
     * @return Newly created {@link Parameter}, if parse resulted in a valid {@link Parameter}. Otherwise, null
     * @throws ParameterException when we cannot create any {@link Parameter} object from the given {@link String}
     */
    public static Parameter parse(int lineIdx, String string) throws ParameterException {
        assert string != null;
        // Identify Parameter extractor.

        // If [...], attempt to parse MemoryLocation
        if (string.startsWith("[") && string.endsWith("]")) {
            return parseAddress(lineIdx, string.substring(1, string.length() - 1));
        }

        // If matching identifier, create a RegisterReference parameter
        var idMatcher = IDENTIFIER_PATTERN.matcher(string);
        if (idMatcher.matches()) {
            return new RegisterReference(lineIdx, idMatcher.group(0));
        }

        // If starting with label identifier, create a Label parameter
        if (string.startsWith("@")) {
            return new Label(string);
        }

        // Otherwise, attempt to locate a Constant.
        var asConstant = identifyConstant(string);
        if (asConstant != null) {
            return asConstant;
        }

        // If nothing matches, throw
        throw new ParameterException("Unknown parameter: " + string);
    }

    /**
     * This function is intentionally forced final.
     * This enforces hash-by-address for each {@link Parameter} type object.
     * Reason: take a memory location for example:
     *  [r0] -> AbsMemLoc over Register
     *  If Register value changes, hashCode would change if overridden
     *  We do not want this.
     * @return hash code the object.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
