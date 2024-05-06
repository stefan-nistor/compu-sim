package ro.uaic.swqual.exception.parser;

public class ASMParserException extends ParserException {
    public ASMParserException(String message) {
        super(String.format("Unrecognized ASM parameter: '%s'%n Expected 'r<reg>' '#<constant>' '@<label>'"
                ,  message));
    }
}
