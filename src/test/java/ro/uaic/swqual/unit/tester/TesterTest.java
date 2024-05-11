package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.tester.Tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesterTest {
    interface TesterTestConsumer {
        void accept(boolean outcome, String out, String err);
    }

    void testerTest(String path, TesterTestConsumer testerConsumer) {
        var outStream = new StringBuilder();
        var errStream = new StringBuilder();
        var tester = new Tester(path, outStream::append, errStream::append);
        tester.run();
        testerConsumer.accept(tester.getOutcome(), outStream.toString(), errStream.toString());
    }

    @Test
    void successOutputShouldBeSuccessful() {
        testerTest("src/test/resources/unit/tester-success-expected-success.txt", (outcome, out, err) -> {
            assertTrue(outcome);
            assertEquals("Test 'src/test/resources/unit/tester-success-expected-success.txt' was successful", out);
            assertEquals("", err);
        });
    }

    @Test
    void noExpectationsResultInFailure() {
        testerTest("src/test/resources/unit/tester-failure-no-expectations.txt", (outcome, out, err) -> {
            assertFalse(outcome);
            assertEquals("", out);
            assertEquals(
                    "Error: no expectations found in 'src/test/resources/unit/tester-failure-no-expectations.txt'",
                    err
            );
        });
    }

    @Test
    void expectedFailureButAllExpectationsSucceedResultsInFailure() {
        testerTest("src/test/resources/unit/tester-failure-expected-failure.txt", (outcome, out, err) -> {
            assertFalse(outcome);
            assertEquals("", out);
            assertEquals("Outcome of test 'src/test/resources/unit/tester-failure-expected-failure.txt'" +
                    " invalid. Expected failure, but all expectations succeeded", err);
        });
    }

    @Test
    void expectedFailureAndFailsResultsInSuccess() {
        testerTest("src/test/resources/unit/tester-success-expected-failure.txt", (outcome, out, err) -> {
            assertTrue(outcome);
            assertEquals("Test 'src/test/resources/unit/tester-success-expected-failure.txt' was successful", out);
            assertEquals("", err);
        });
    }

    @Test
    void expectedSuccessAndFailsResultsInFailureBecauseOfRegistryState() {
        testerTest("src/test/resources/unit/tester-failure-expected-success-because-reg.txt", (outcome, out, err) -> {
            assertFalse(outcome);
            assertEquals("", out);
            assertEquals(
                    """
                    Expectation 'expect-true {r0==9}' at line 4 did not succeed.\s
                    \tExpression 'r0==9'. Used Registry State -> r0: 10
                    Expectation 'expect-true {r0==14; r1==20; r3==14}' at line 7 did not succeed.\s
                    \tExpression 'r0==14'. Used Registry State -> r0: 15
                    \tExpression 'r1==20'. Correctly evaluated
                    \tExpression 'r3==14'. Used Registry State -> r3: 15
                    """,
                    err
            );
        });
    }

    @Test
    void expectedSuccessAndFailsResultsInFailureBecauseOfMemoryState() {
        testerTest("src/test/resources/unit/tester-failure-expected-success-because-mem.txt", (outcome, out, err) -> {
            assertFalse(outcome);
            assertEquals("", out);
            assertEquals(
                    """
                    Expectation 'expect-true {[0x100]==9}' at line 4 did not succeed.\s
                    \tExpression '[0x100]==9'. Used Memory State -> [0x100]: 10
                    Expectation 'expect-true {[0x100]==14; [0x200]==20; [0x300]==14}' at line 7 did not succeed.\s
                    \tExpression '[0x100]==14'. Used Memory State -> [0x100]: 15
                    \tExpression '[0x200]==20'. Correctly evaluated
                    \tExpression '[0x300]==14'. Used Memory State -> [0x300]: 15
                    """,
                    err
            );
        });
    }

    @Test
    void expectedSuccessAndFailsResultsInFailureBecauseOfMemoryStateAndRegistryState() {
        testerTest("src/test/resources/unit/tester-failure-expected-success-because-reg-mem.txt", (outcome, out, err) -> {
            assertFalse(outcome);
            assertEquals("", out);
            assertEquals(
                    """
                    Expectation 'expect-true {r1==4; [0x100]==99; r0==[0x100]}' at line 6 did not succeed.\s
                    \tExpression 'r1==4'. Used Registry State -> r1: 5
                    \tExpression '[0x100]==99'. Used Memory State -> [0x100]: 5
                    \tExpression 'r0==[0x100]'. Used Registry State -> r0: 256; Used Memory State -> [0x100]: 5
                    Expectation 'expect-true {[0x100]==9}' at line 8 did not succeed.\s
                    \tExpression '[0x100]==9'. Used Memory State -> [0x100]: 10
                    Expectation 'expect-true {[0x300]==15; [0x200]==10; [0x100]==14}' at line 10 did not succeed.\s
                    \tExpression '[0x300]==15'. Correctly evaluated
                    \tExpression '[0x200]==10'. Used Memory State -> [0x200]: 0
                    \tExpression '[0x100]==14'. Used Memory State -> [0x100]: 15
                    Expectation 'expect-true {[0x100]==14; [0x200]==20; [0x300]==14}' at line 11 did not succeed.\s
                    \tExpression '[0x100]==14'. Used Memory State -> [0x100]: 15
                    \tExpression '[0x200]==20'. Correctly evaluated
                    \tExpression '[0x300]==14'. Used Memory State -> [0x300]: 15
                    """,
                    err
            );
        });
    }
}
