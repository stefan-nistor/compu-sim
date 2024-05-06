package ro.uaic.swqual.exception.parser;

import ro.uaic.swqual.model.operands.RegisterReference;

public class UndefinedReferenceException extends ParserException {
    public UndefinedReferenceException(RegisterReference registerReference) {
        super("Error at line " + registerReference.getReferencedAtLine()
                + ": Undefined Reference to symbol '" + registerReference.getName() + "'");
    }
}
