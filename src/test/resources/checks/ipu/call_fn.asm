// sim-test
// expected: success

// Explanation:
//
// 1. Stack Pointer:
// sp will always point two bytes PAST the head of the stack.
// If stack contains 0xABCD 0xDEFA 0x1234
//                                        ^ SP points here
// Reason: if stack is empty, we need to avoid an overflow, as it would not make sense
// So when we want to read from stack, we first pop, then we read.
// Or, we offset negatively by two (or more).
//
// 2. Function calls:
// When we call, we want to remember where to return. Therefore, the mechanism works as follows:
// We push the desired call parameters (explicitly)
// We push the address to call (implicitly done by call instruction)
//   Now stack contains param0 param1 param2 ... paramN returnAddr
// ILU will jump to call addr (implicitly)
// <Execute function code>
//   In function code, parameters can be accessed, in reverse order, from sp - 4 onwards
//   Reason:
//     memory: ... param0 param1 ... paramN-1 paramN returnAddr ...
//     SP:                           [SP-6]   [SP-4] [SP-2]     ^ SP is here
// Call ret
// ILU will jump to the returnAddr (implicitly, stored at SP - 2)
// Stack is popped (implicitly, to remove returnAddr)
// Used parameters are popped (explicitly)

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
pop;
umul r0 [sp - 4];
ret;


@main:
push 2;
push 4;
call @sum;
pop;
pop; // expect-true {r0 == 6}

push 4;
call @square;
pop; // expect-true {r0 == 16}

push 3;
call @cube;
pop; // expect-true {r0 == 27}
