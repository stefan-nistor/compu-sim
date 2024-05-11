// sim-test
// expected: failure

mov r0 #10; // expect-true {r0==10}
add r0 #5;
mov r1 #20; // expect-true {r0==15; r1==20}
mov [0x200] 0x20; // expect-true {[0x200] == 0x20}
mov [0x300] 0x40; // expect-true {[0x300] == 0x40}
add [0x200] [0x300]; // expect-true {[0x200] == 0x60}
mov r3 0x300; // expect-true {r3 == 0x300}
add [0x200] [r3]; // expect-true {[0x200] == 0xA0}
mov r3 0x200; // expect-true {r3 == 0x200}
add [r3] [r3 + 0x100]; // expect-true {[0x200] == 0x0E0}
add [r3] [r3 + 0x150 - 0x50]; // expect-true {[0x200] == 0x119}
