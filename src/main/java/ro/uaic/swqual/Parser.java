package ro.uaic.swqual;

import ro.uaic.swqual.exception.ParameterException;
import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Label;
import ro.uaic.swqual.model.operands.MemoryLocation;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.model.operands.Register;
import ro.uaic.swqual.model.operands.RegisterReference;
import ro.uaic.swqual.util.Tuple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents the base assembly code parser object.
 */
public class Parser {
    /** List of {@link Instruction Instructions} from the current ongoing parse & link */
    private final List<Instruction> instructions = new ArrayList<>();
    /** Map from label string to instruction addresses.
     * Used to replace labels with actual address values in jump instructions. */
    private final Map<String, Constant> jumpMap = new HashMap<>();

    /**
     * Method used to reset the parser state. It will clear {@link Parser#instructions} and {@link Parser#jumpMap}
     */
    public void clear() {
        instructions.clear();
        jumpMap.clear();
    }

    /**
     * Getter for the {@link Parser#instructions instruction list} currently parsed.
     * @return list of instructions
     */
    public List<Instruction> getInstructions() {
        return instructions;
    }

    /**
     * Method used to parse a single line with a known line number
     * @param line the line to parse
     * @param lineIdx the line number
     */
    protected void parseLine(String line, int lineIdx) {
        assert line != null;
        // skip empty / comment lines
        if (line.trim().isEmpty() || line.trim().startsWith("//")) {
            return;
        }
        // if not a label line, parse instruction
        if (!line.trim().startsWith("@")) {
            parseInstruction(lineIdx, line);
        } else {
            // otherwise, store label location in the jump map
            var labelKey = line.trim().substring(0, line.length() - 1);
            if (jumpMap.containsKey(labelKey)) {
                throw new DuplicateJumpTargetException(line);
            }
            jumpMap.put(labelKey, new Constant((char) instructions.size()));
        }
    }

    /**
     * Method used to parse a source file from a given path
     * @param path the path of the file to use
     * @return instance to self for chained operations
     */
    public Parser parse(String path) {
        assert path != null;
        // clear the previous parse
        clear();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            var lineIndex = 0;
            String line;
            while ((line = br.readLine()) != null) {
                // parse each line in the dedicated method until exhaustion.
                parseLine(line, ++lineIndex);
            }
            return this;
        } catch (IOException e) {
            throw new ParserException(e.getMessage());
        }
    }

    /**
     * Method used to group address expressions (e.g. [r0 + 24]), since they are split beforehand.
     * @param tokens list of tokens that were previously whitespace spearated.
     * @return the new list of tokens, after merging the address tokens.
     */
    private List<String> mergeAddressParameters(List<String> tokens) {
        assert tokens != null;
        var newList = new ArrayList<String>();
        // keep in mind if we are currently inside an address identification field ([...])
        boolean inAddress = false;
        StringBuilder addressCompound = new StringBuilder();
        for (var token : tokens) {
            if (token.startsWith("[") && token.endsWith("]")) {
                // if token is just an isolated address ([r0]), check the current state.
                if (inAddress) {
                    throw new ParserException("In address identifier, unexpected '[");
                }
                newList.add(token);
            } else if (token.startsWith("[")) {
                // otherwise, if in start of address group ([r0...)
                if (inAddress) {
                    throw new ParserException("In address identifier, unexpected '['");
                }

                inAddress = true;
                // start a compounding operation
                addressCompound = new StringBuilder(token);
            } else if (token.endsWith("]")) {
                // if reached an end of an address group (... + 24])
                if (!inAddress) {
                    throw new ParserException("Not in address identifier, unexpected ']'");
                }

                inAddress = false;
                // end the compounding operation
                addressCompound.append(token);
                // add the compound to the list
                newList.add(addressCompound.toString());
                addressCompound = new StringBuilder();
            } else if (inAddress) {
                // keep compounding if we are in [ ... token ... ].
                addressCompound.append(token);
            } else {
                // if not in address, add token as-is
                newList.add(token);
            }
        }

        // check if we have a non-ended address group (mov r0 [r1 + 24;)
        if (inAddress) {
            throw new ParserException("In address identifier that was never terminated");
        }

        return newList;
    }

    /**
     * Method used to parse a definite instruction from a given line
     * @param lineIndex the line number
     * @param line the instruction code
     * @return reference to self used in chain operations
     */
    public Parser parseInstruction(int lineIndex, String line) {
        assert line != null;
        line = line.trim();
        // ensure presence of ';'
        if (!line.endsWith(";") && !line.endsWith(":")) {
            throw new ParserException("Error at line " + line + ": expected ';' or ':'");
        }

        line = line.substring(0, line.length() - 1);
        // split by whitespace and construct the instruction
        var parsed = line.split("\\s+");
        var instruction = new Instruction();
        var parameterList = new ArrayList<Parameter>();

        try {
            // first token is the instruction type
            instruction.setType(InstructionType.fromLabel(parsed[0]));
            // remaining tokens are the parameters.
            mergeAddressParameters(
                    Arrays.stream(parsed).dropWhile(str -> InstructionType.fromLabel(str) != null).toList()
            ).forEach(
                    param -> parameterList.add(Parameter.parse(lineIndex, param))
            );
        } catch (ParameterException exception) {
            throw new ParserException(exception);
        }

        // add the parameters, if present
        instruction.setParameters(Tuple.of(
                parameterList.isEmpty() ? null : parameterList.get(0),
                parameterList.size() <= 1 ? null : parameterList.get(1)
        ));

        instructions.add(instruction);
        return this;
    }

    /**
     * Method used to resolve the in-source references.
     * This is not a full reference resolve, as it will only cover local references ({@link Label}).
     * @return Reference to self for use in chain operations
     */
    public Parser link() {
        instructions.stream()
                .filter(instruction -> instruction.getParam1() instanceof Label)
                .forEach(instruction -> {
                    // after parse, jumps will contain a label in the first parameter
                    // resolve this to the actual instruction index the label refers to.
                    var label = (Label) instruction.getParam1();
                    var jmpTarget = jumpMap.get(label.getName());
                    if (jmpTarget == null) {
                        throw new JumpLabelNotFoundException(label.getName());
                    }
                    instruction.setParam1(jmpTarget);
                });
        return this;
    }

    /**
     * Method used to resolve the out-of-source references.
     * This is not a full reference resolve, as it will only cover the external references ({@link RegisterReference}).
     * @param registerMap a map associating register assembly labels to actual {@link Register} objects.
     * @return Reference to self for use in chain operations
     * @throws UndefinedReferenceException if a {@link RegisterReference} in the current list of instructions cannot
     *   be resolved using the received map.
     */
    public Parser resolveReferences(
            Map<String, Register> registerMap
    ) throws UndefinedReferenceException {
        assert registerMap != null;
        instructions.forEach(instruction -> {

            // Generic RegistryReference consumer, using the received map.
            BiConsumer<Supplier<Parameter>, Consumer<Parameter>> referenceResolver = (supplier, consumer) -> {
                var param = supplier.get();
                if (param instanceof RegisterReference ref) {
                    var resolved = registerMap.get(ref.getName());
                    if (resolved == null) {
                        throw new UndefinedReferenceException(ref);
                    }
                    consumer.accept(resolved);
                }

                if (param instanceof MemoryLocation memLoc) {
                    memLoc.resolveInnerReferences(registerMap);
                }
            };

            // look at both params as any could be a RegisterReference.
            referenceResolver.accept(instruction::getParam1, instruction::setParam1);
            referenceResolver.accept(instruction::getParam2, instruction::setParam2);
        });
        return this;
    }
}
