package ro.uaic.swqual;

import ro.uaic.swqual.exception.ParserException;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.operands.Constant;
import ro.uaic.swqual.model.operands.Parameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Processor processor;

    public Parser(Processor processor) {
        this.processor = processor;
    }

    public List<Instruction> parse(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            List<Instruction> instructions = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().charAt(0) == '@') continue;

                instructions.add(parseInstruction(line));
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
                int registerIndex = Integer.parseInt(param.substring(1));
                parameterList.add(processor.getDataRegisters().get(registerIndex));
            }

            if (param.startsWith("#")) {
                short value = (short)Int.parseInt(param.substring(1));
                parameterList.add(new Constant(value));
            }
        }

        instruction.setParameters(parameterList);
        return instruction;
    }


}
