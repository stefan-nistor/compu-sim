package ro.uaic.swqual;

import ro.uaic.swqual.exception.parser.*;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Label;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.proc.Processor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private final Processor processor;
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Constant> jumpMap = new HashMap<>();

    public Parser(Processor processor) {
        this.processor = processor;
    }

    public List<Instruction> parse(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!line.trim().startsWith("@")) {
                    instructions.add(parseInstruction(line));
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

    public Instruction parseInstruction(String line) {
        var parsed = line.trim().split("\\s+");

        if(parsed.length > 3) {
            throw new TooManyASMArgumentsException(parsed.length);
        }

        var instruction = new Instruction();
        var parameterList = new ArrayList<Parameter>();
        instruction.setType(InstructionType.fromLabel(parsed[0]));

        for (int i = 1; i < parsed.length; i++) {
            if (parsed[i].startsWith("r")) {
                var registerIndex = Integer.parseInt(parsed[i].substring(1));
                parameterList.add(processor.getDataRegisters().get(registerIndex));
            }

            else if (parsed[i].startsWith("#")) {
                var value = (char) Integer.parseInt(parsed[i].substring(1));
                parameterList.add(new Constant(value));
            }

            else if (parsed[i].startsWith("@")) {
                parameterList.add(new Label(parsed[i]));
            }

            else {
                throw new ASMParserException(parsed[i]);
            }
        }
        if(parameterList.isEmpty()) {
            throw new ParserException("No parameters found for instruction: " + line);
        }
        instruction.setParameters(parameterList);
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

}
