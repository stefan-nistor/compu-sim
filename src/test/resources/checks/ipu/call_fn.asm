// sim-test
// expected: success

jmp @main;


@sum:
mov r0 [sp - 6];
add r0 [sp - 4];
ret;

@square:
mov r0 [sp - 4];
umul r0 r0;
ret;

@cube:
push [sp - 4];
call @square;
pop r7;
umul r0 [sp - 4];
ret;

@main:

push 2;
push 4;
call @sum;
pop r7;
pop r7; // expect-true {r0 == 6}

push 4;
call @square;
pop r7; // expect-true {r0 == 16}

push 3;
call @cube;
pop r7; // expect-true {r0 == 27}
