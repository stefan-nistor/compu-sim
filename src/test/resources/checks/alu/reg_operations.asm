// sim-test
// expected: success

mov r0 #10; // expect-true {r0==10}
add r0 #5;
mov r1 #20; // expect-true {r0==15; r1==20}
