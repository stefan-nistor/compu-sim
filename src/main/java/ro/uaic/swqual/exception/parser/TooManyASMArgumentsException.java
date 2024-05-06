package ro.uaic.swqual.exception.parser;


public class TooManyASMArgumentsException extends ParserException {

    public TooManyASMArgumentsException(int len) {
        super(String.format("Too many ASM arguments: %d. Maximum 2 arguments allowed", len));
    }
}
