package ro.uaic.swqual.exception.parser;

public class DuplicateJumpTargetException extends ParserException {
    public DuplicateJumpTargetException(String line) {
        super("Duplicate jump label for: " + line );
    }
}
