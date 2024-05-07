# Computing System Simulator

## Glossary

- CPU - Central Processing Unit
- RAM - Random Access Memory
- ROM - Read Access Memory
- I/O - Peripherals, Input/Output
- DR = Data Register
- FR = Flag Register
- SR = Special Purpose Register
- PC = Program Counter
- SP = Stack Pointer

## Components

- CPU
- RAM/ROM
- I/O

## CPU

### Requirements:
- [Project requirements webpage](https://edu.info.uaic.ro/calitatea-sistemelor-software/lab/proiect-cs.html)
- 8 DR - d0...d7, 1 DR = 16b
- 1 FR - operational result flags
- n SR - PC, SP, ...

### Assembly:

#### Type 1 - Actual Reality:

jne d0 15 - 2B, 2B, 2B
add d0 d1 - 2B, 2B, xB

```
Each instruction is composed of:

- Command Data - 2B
- Param1       - 2B
- Param2       - 2B

- Command Data:
  0b 0000 0000           - param info
             ^
             If bit is set, param 1 is register, otherwise constant
            ^
            Same, but param2  
               0000 0000 - command type, otherwise constant
                      ^^ SubCommand type
                     ^ Comparison op
                    ^ ALU Op
                  ^ Jmp Op
                 ^ Stack Op / Func Op
                ^ RAM Op
               ^ I/O Op
               
- Add d0 d1:
  | 0000 0011 0000 1000 | 0000 0000 0000 0000 | 0000 0000 0000 0001 |
- Sub d5 100
  | 0000 0001 0000 1001 | 0000 0000 0000 0005 | 0000 0000 0110 0100 |



mov d1 50
label1:
sub d1 1
cmp d1 0
je label1
```

#### Type 2 - Emulated:

```
mov d1 50
label1:
sub d1 1
cmp d1 0
je label1

Preprocess into List<Instruction>, where
Instruction is { Command, Param, Param }
where
Command is enum { Add, Sub, JumpNotEq, ... }
Param is { ParamType, ParamData }
ParamType is { Constant, Register, Instruction, Label }
ParamData is { int, String, Instruction } 


1 : Parse the file to obtain 
  - Parse instructions and keep a Map<Label, Instruction>
  - Link Referenced Labels:
       - Jump Commands referncing labels will have their parameter replaced 
            from String to Instruction (PC)
```

For instructions that do not use all params, the param will be ignored



### Architecture:

- 8 Data registers named d0, d1.... d7
- Special purpose registers defined on demand
  - Such as - FP (flags), SP (stack ptr), PC (prog ctr), ...
- Command modules 
  - ALU
  - JMP
  - RAM
  - Stack
  - I/O

Practical Example (more present in tests)
```
// add d1 50
CPU.setD1(20) // Equivalent to mov d1 20
CPU.getALU().add(CPU.getD1(), 50);
AssertEqauls(70, CPU.getD1());
AssertEqauls(70, (CPU.getFP() & Registry::FlagBits::Overflow) == 0);
```


### Graphical interface:

- Text input panel for writing ASM code
  - May include buttons for loading local ASM input files
- Schema for peripheral devices with links between them
- Panel for code output (text)