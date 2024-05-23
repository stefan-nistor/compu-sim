package ro.uaic.swqual.model.operands;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.exception.ValueException;

import java.util.ArrayList;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;

public abstract class Parameter {
    private static final Pattern CONSTANT_BASE_2_PATTERN = Pattern.compile("#?0[bB]([0-1]+)");
    private static final Pattern CONSTANT_BASE_8_PATTERN = Pattern.compile("#?0([0-7]+)");
    private static final Pattern CONSTANT_BASE_10_PATTERN = Pattern.compile("#?(0|[1-9]\\d*)");
    private static final Pattern CONSTANT_BASE_16_PATTERN = Pattern.compile("#?0[xX]([0-9a-fA-F]+)");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[_a-zA-Z]\\w*");

    /** As it happens, char = unsigned short. */
    protected char value;
    public char getValue() {
        return value;
    }

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

    public static Parameter parse(String string) {
        return parse(0, string);
    }

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

    private static Parameter parseAddress(int lineIndex, String text) {
        // TODO: come up with a better way. This only covers +-
        var paramList = new ArrayList<Parameter>();
        var relList = new ArrayList<BinaryOperator<Character>>();
        for (var token : text.splitWithDelimiters("[+-]", Integer.MAX_VALUE)) {
            token = token.strip();
            if (token.equals("+")) {
                relList.add((a, b) -> (char)(a + b));
            } else if (token.equals("-")) {
                relList.add((a, b) -> (char)(a - b));
            } else {
                paramList.add(parse(lineIndex, token));
            }
        }

        if (paramList.size() == 1) {
            var param = paramList.getFirst();
            if (param instanceof Constant con) {
                return new ConstantMemoryLocation(con.getValue());
            }
            return new AbsoluteMemoryLocation(param);
        }

        try {
            return new RelativeMemoryLocation(paramList, relList);
        } catch (ValueException exception) {
            throw new ParameterException(exception);
        }
    }

    public static Parameter parse(int lineIdx, String string) {
        assert string != null;
        if (string.startsWith("[") && string.endsWith("]")) {
            return parseAddress(lineIdx, string.substring(1, string.length() - 1));
        }

        var idMatcher = IDENTIFIER_PATTERN.matcher(string);
        if (idMatcher.matches()) {
            return new RegisterReference(lineIdx, idMatcher.group(0));
        }

        if (string.startsWith("@")) {
            return new Label(string);
        }

        var asConstant = identifyConstant(string);
        if (asConstant != null) {
            return asConstant;
        }

        throw new ParameterException("Unknown parameter: " + string);
    }

    // HashCode is intentionally forced final here.
    // Reason: take a memory location for example:
    //  [r0] -> AbsMemLoc over Register
    //  If Register value changes, hashCode would change if overridden
    //  We do not want this.
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
