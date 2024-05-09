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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Parser {
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Constant> jumpMap = new HashMap<>();

    public List<Instruction> parse(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            var lineIndex = 0;
            String line;
            while ((line = br.readLine()) != null) {
                ++lineIndex;
                if (line.trim().isEmpty() || line.trim().startsWith("//")) {
                    continue;
                }
                if (!line.trim().startsWith("@")) {
                    instructions.add(parseInstruction(lineIndex, line));
                } else {
                    var labelKey = line.trim().substring(0, line.length() - 1);
                    if (jumpMap.containsKey(labelKey)) {
                        throw new DuplicateJumpTargetException(line);
                    }
                    jumpMap.put(labelKey, new Constant((char) instructions.size()));
                }
            }
            return instructions;
        } catch (IOException e) {
            throw new ParserException(e.getMessage());
        }
    }

    public Instruction parseInstruction(int lineIndex, String line) {
        var parsed = line.trim().split("\\s+");
        var instruction = new Instruction();
        var parameterList = new ArrayList<Parameter>();

        instruction.setType(InstructionType.fromLabel(parsed[0]));

        for (String param : parsed) {
            if (param.startsWith("r")) {
                parameterList.add(new RegisterReference(lineIndex, param));
            }

            if (param.startsWith("#")) {
                var value = (char) Integer.parseInt(param.substring(1));
                parameterList.add(new Constant(value));
            }

            if (param.startsWith("@")) {
                parameterList.add(new Label(param));
            }
        }

        instruction.setParameters(Tuple.of(
                parameterList.isEmpty() ? null : parameterList.get(0),
                parameterList.size() == 1 ? null : parameterList.get(1)
        ));
        return instruction;
    }

    public void link() {
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
    }

    public static List<Instruction> resolveReferences(
            List<Instruction> instructions,
            Map<String, Register> registerMap
    ) throws UndefinedReferenceException {
        return instructions.stream().map(instruction -> {
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
            return instruction;
        }).toList();
    }
}
