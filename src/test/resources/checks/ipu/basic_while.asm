// sim-test
// expected: success

mov r0 #10 // expect-true {r0==10}
@StartLoop:
sub r0 #1
cmp r0 #0
jne @StartLoop
mov r0 r0 // expect-true {r0==0}
