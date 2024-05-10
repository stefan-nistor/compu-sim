package ro.uaic.swqual;

import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.exception.parser.UndefinedReferenceException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Label;
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

public class Parser {
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Constant> jumpMap = new HashMap<>();

    public void clear() {
        instructions.clear();
        jumpMap.clear();
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    protected void parseLine(String line, int lineIdx) {
        if (line.trim().isEmpty() || line.trim().startsWith("//")) {
            return;
        }
        if (!line.trim().startsWith("@")) {
            parseInstruction(lineIdx, line);
        } else {
            var labelKey = line.trim().substring(0, line.length() - 1);
            if (jumpMap.containsKey(labelKey)) {
                throw new DuplicateJumpTargetException(line);
            }
            jumpMap.put(labelKey, new Constant((char) instructions.size()));
        }
    }

    public Parser parse(String path) {
        clear();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            var lineIndex = 0;
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(line, ++lineIndex);
            }
            return this;
        } catch (IOException e) {
            throw new ParserException(e.getMessage());
        }
    }

    private Parameter identifyParameter(int lineIndex, String string) {
        if (string.startsWith("@")) {
            return new Label(string);
        }

        if (string.startsWith("r")) {
            return new RegisterReference(lineIndex, string);
        }

        if (string.startsWith("#")) {
            return new Constant((char) Integer.parseInt(string.substring(1)));
        }

        throw new ParserException("Unknown parameter: " + string);
    }

    public Parser parseInstruction(int lineIndex, String line) {
        line = line.trim();
        if (!line.endsWith(";") && !line.endsWith(":")) {
            throw new ParserException("Error at line " + line + ": expected ';' or ':'");
        }

        line = line.substring(0, line.length() - 1);
        var parsed = line.split("\\s+");
        var instruction = new Instruction();
        var parameterList = new ArrayList<Parameter>();

        instruction.setType(InstructionType.fromLabel(parsed[0]));
        Arrays.stream(parsed).dropWhile(str -> InstructionType.fromLabel(str) != null).forEach(
                param -> parameterList.add(identifyParameter(lineIndex, param))
        );

        instruction.setParameters(Tuple.of(
                parameterList.isEmpty() ? null : parameterList.get(0),
                parameterList.size() == 1 ? null : parameterList.get(1)
        ));

        instructions.add(instruction);
        return this;
    }

    public Parser link() {
        instructions.stream()
                .filter(instruction -> instruction.getParam1() instanceof Label)
                .forEach(instruction -> {
                    var label = (Label) instruction.getParam1();
                    var jmpTarget = jumpMap.get(label.getName());
                    if (jmpTarget == null) {
                        throw new JumpLabelNotFoundException(label.getName());
                    }
                    instruction.setParam1(jmpTarget);
                });
        return this;
    }

    public Parser resolveReferences(
            Map<String, Register> registerMap
    ) throws UndefinedReferenceException {
        instructions.forEach(instruction -> {
            BiConsumer<Supplier<Parameter>, Consumer<Parameter>> referenceResolver = (supplier, consumer) -> {
                var param = supplier.get();
                if (param instanceof RegisterReference ref) {
                    var resolved = registerMap.get(ref.getName());
                    if (resolved == null) {
                        throw new UndefinedReferenceException(ref);
                    }
                    consumer.accept(resolved);
                }
            };
            referenceResolver.accept(instruction::getParam1, instruction::setParam1);
            referenceResolver.accept(instruction::getParam2, instruction::setParam2);
        });
        return this;
    }
}
