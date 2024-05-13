// sim-test
// expected: success

jmp @main;


@isPrime:
mov r3 [sp - 4];

// if < 2, is not prime
mov r0 0;
cmp r3 2;
jlt @isPrimeEnd;

// if < 4, implicitly prime
mov r0 1;

mov r2 2;
@isPrimeLoop:
mov r4 r2;
umul r4 r4;
cmp r4 r3;
jgt @isPrimeEnd;

// compute remainder
mov r1 r3;
udiv r1 r2;
cmp r7 0;
// if not zero, keep going
jne @isPrimeContinue;
// if zero, not prime.
mov r0 0;
jmp @isPrimeEnd;

@isPrimeContinue:
add r2 1;
jmp @isPrimeLoop;

@isPrimeEnd:
ret;


// Input[kb]: n, then n numbers
// Output[r0]: number of prime numbers in given sequence of n numbers


// kb-preload {6, 11, 19, 25, 14, 97, 13}
@expectResult:
mov r0 [sp - 4]; // expect-true {r0 == 4}
ret;


@main:

mov r5 [0x10];
umul r5 2;
mov r6 0;
@readNumsLoopStart:
mov [0x600 + r6] [0x10];
add r6 2;
cmp r6 r5;
jlt @readNumsLoopStart;

// parse nums
mov r6 0;
mov r4 r5;
mov r5 0;
@computeHowManyPrimeLoopStart:
push r4;
push [0x600 + r6];
call @isPrime;
pop;
pop r4;
cmp r0 1;
jne @computeHowManyPrimeLoopContinue;
add r5 1;

@computeHowManyPrimeLoopContinue:
add r6 2;
cmp r6 r4;
jlt @computeHowManyPrimeLoopStart;

push r5;
call @expectResult;
pop;
