package ro.uaic.swqual.exception.parser;

public class JumpLabelNotFoundException extends ParserException {
    public JumpLabelNotFoundException(String label) {
        super("Label " + label + " not found");
    }
}
