package ro.uaic.swqual;

import ro.uaic.swqual.exception.parser.DuplicateJumpTargetException;
import ro.uaic.swqual.exception.parser.JumpLabelNotFoundException;
import ro.uaic.swqual.exception.parser.ParserException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Label;
import ro.uaic.swqual.model.operands.Parameter;
import ro.uaic.swqual.proc.CPU;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private final CPU CPU;
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Constant> jumpMap = new HashMap<>();

    public Parser(CPU CPU) {
        this.CPU = CPU;
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
        var instruction = new Instruction();
        var parameterList = new ArrayList<Parameter>();

        instruction.setType(InstructionType.fromLabel(parsed[0]));

        for (String param : parsed) {
            if (param.startsWith("r")) {
                var registerIndex = Integer.parseInt(param.substring(1));
                parameterList.add(CPU.getDataRegisters().get(registerIndex));
            }

            if (param.startsWith("#")) {
                var value = (char) Integer.parseInt(param.substring(1));
                parameterList.add(new Constant(value));
            }

            if (param.startsWith("@")) {
                parameterList.add(new Label(param));
            }
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
