// sim-test
// expected: success

// tester kb will be at 0x10.
// Will use "kb-preload" to set-up keyboard.

// kb-preload {0x60, 0x61, 0x62}

jmp @main;

@main:

mov r0 [0x10]; // expect-true {r0==0x60}
mov r0 [0x10]; // expect-true {r0==0x61}
mov r0 [0x10]; // expect-true {r0==0x62}
