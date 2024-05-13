// sim-test
// expected: success

// tester display will be at 0x20.
// tester display will have a length of 48 bytes (0x30) -> 48 chars (operating in char = byte).
// any characters not written will be 0. Null terminated string rules apply.

jmp @main;

@main:

mov r1 0; // expect-true {r1 == 0}
mov r2 97; // character 'a'

@disp-write-loop:
mov [0x20 + r1] r2;
add r2 1;
cmp r2 20;
jlt @disp-write-loop;

// dummy hook for expect
mov r3 0; // expect-display {abcdefghijklmnopqrst}
mov [0x20 + 2] 97; // expect-display {abadefghijklmnopqrst}
