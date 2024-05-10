package ro.uaic.swqual.unit.tester;

import org.junit.jupiter.api.Test;
import ro.uaic.swqual.tester.Tester;
import ro.uaic.swqual.util.Tuple3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TesterChecksTest {
    List<String> pathOfAllFilesIn(String root) {
        try (var paths = Files.walk(Paths.get(root))) {
            return paths.filter(Files::isRegularFile).map(Path::toString).toList();
        } catch (IOException exception) {
            fail();
        }
        return List.of();
    }

    @Test
    void runTesterForEachAsmFileWithHeadersInChecks() {
        var checkFiles = pathOfAllFilesIn(Tester.CHECKS_PATH);
        var thCount = Math.min(checkFiles.size(), Runtime.getRuntime().availableProcessors());
        Map<String, Tuple3<Tester, StringBuilder, StringBuilder>> resourceMap = new HashMap<>();
        try (var tpe = Executors.newFixedThreadPool(thCount)) {
            checkFiles.stream().map(
                    file -> {
                        var outSb = new StringBuilder();
                        var errSb = new StringBuilder();
                        var tester = new Tester(
                                file,
                                outSb::append,
                                errSb::append
                        );
                        resourceMap.put(file, new Tuple3<>(tester, outSb, errSb));
                        return tester;
                    }
            ).forEach(tpe::execute);

            tpe.shutdown();
            try {
                assertTrue(tpe.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
            } catch (InterruptedException exception) {
                fail();
            }
        } catch (Exception e) {
            fail();
        }

        var anyFailed = resourceMap.values().stream().anyMatch(e -> !e.getFirst().getOutcome());
        if (anyFailed) {
            for (var entry : resourceMap.entrySet()) {
                var res = entry.getValue();
                if (!res.getFirst().getOutcome()) {
                    var path = entry.getKey();
                    System.out.println("Test of '" + path + "' failed: \n" + res.getThird().toString());
                }
            }
        }

        assertFalse(anyFailed);
    }
}
