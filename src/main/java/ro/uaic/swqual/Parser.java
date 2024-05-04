package ro.uaic.swqual;

import ro.uaic.swqual.model.Instruction;

import java.util.List;

public class Parser {
    private static final String ASM_FILEPATH = "src/main/resources/input.txt";

    /*
    parse asm file and return list of commands
    command = non-empty line without @label
    command = instr reg1 reg2
     command = instr reg1
     */

    public List<Instruction> parse() {

    }

    private Instruction parseInstruction(String line) {
        var parsed = line.split(" ");

        var instruction = new Instruction();
        instruction.setType(InstructionType.valueOf(parsed[0]));

        for (String param : parsed) {
            if(param.startsWith("r")) {
                instruction.setParameters();
            }
        }


        return instruction;
    }




}
