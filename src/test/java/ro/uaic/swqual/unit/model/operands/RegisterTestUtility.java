package ro.uaic.swqual.unit.model.operands;

import ro.uaic.swqual.model.operands.Parameter;

import java.util.Objects;

public interface RegisterTestUtility {
    default <T extends Parameter, U extends Parameter> boolean equalsCoverageTest(
            T parameter,
            T equivalent,
            T sameTypeDifferentValue,
            U otherTypeSameValue
    ) {
        var sameTypeEqual = parameter.equals(equivalent);
        var sameTypeNotEqual = parameter.equals(sameTypeDifferentValue);
        var diffTypeEqual = parameter.equals(otherTypeSameValue);
        var nullLhs = Objects.equals(null, parameter);
        var nullRhs = Objects.equals(parameter, null);
        var selfCmp = parameter.equals(parameter);
        return sameTypeEqual && !sameTypeNotEqual && !diffTypeEqual
            && !nullLhs && !nullRhs && selfCmp;
    }
}
