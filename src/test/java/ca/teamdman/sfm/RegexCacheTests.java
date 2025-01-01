package ca.teamdman.sfm;

import ca.teamdman.sfm.common.program.RegexCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexCacheTests {

    private static final int ITERATIONS = 1_000_000;
    private static final int ALTERNATIONS = 25;

    private long measureTime(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - start;
    }

    @BeforeAll
    public static void setup() {
        // Warm-up the JVM
        RegexCache.buildPredicate(".*seeds.*").test("warmup");
        Pattern.compile(".*seeds.*").asMatchPredicate().test("warmup");
    }

    @Test
    public void measureContains() {
        String pattern = ".*seeds.*";
        String testString = "wheat_seeds";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = 0;
        long standardTime = 0;

        for (int i = 0; i < ALTERNATIONS; i++) {
            optimizedTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    optimizedPredicate.test(testString);
                }
            });

            standardTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    standardPredicate.test(testString);
                }
            });
        }

        System.out.println("Contains optimization - Optimized time: " + optimizedTime + " ms");
        System.out.println("Contains optimization - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
        assertTrue(optimizedTime < standardTime, "Optimized=" + optimizedTime + ", Standard=" + standardTime);
    }

    @Test
    public void measureStartsWith() {
        String pattern = "seeds.*";
        String testString = "beetroot_seeds";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = 0;
        long standardTime = 0;

        for (int i = 0; i < ALTERNATIONS; i++) {
            optimizedTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    optimizedPredicate.test(testString);
                }
            });

            standardTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    standardPredicate.test(testString);
                }
            });
        }

        System.out.println("StartsWith optimization - Optimized time: " + optimizedTime + " ms");
        System.out.println("StartsWith optimization - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
        assertTrue(optimizedTime < standardTime, "Optimized=" + optimizedTime + ", Standard=" + standardTime);
    }

    @Test
    public void measureEndsWith() {
        String pattern = ".*seeds";
        String testString = "wheat_seeds";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = 0;
        long standardTime = 0;

        for (int i = 0; i < ALTERNATIONS; i++) {
            optimizedTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    optimizedPredicate.test(testString);
                }
            });

            standardTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    standardPredicate.test(testString);
                }
            });
        }

        System.out.println("EndsWith optimization - Optimized time: " + optimizedTime + " ms");
        System.out.println("EndsWith optimization - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
        assertTrue(optimizedTime < standardTime, "Optimized=" + optimizedTime + ", Standard=" + standardTime);
    }


    @Test
    public void measureStartsWithEndsWith() {
        String pattern = "printed.*processor";
        String testString = "printed_advanced_processor";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = 0;
        long standardTime = 0;

        for (int i = 0; i < ALTERNATIONS; i++) {
            optimizedTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    optimizedPredicate.test(testString);
                }
            });

            standardTime += measureTime(() -> {
                for (int j = 0; j < ITERATIONS / ALTERNATIONS; j++) {
                    standardPredicate.test(testString);
                }
            });
        }

        System.out.println("EndsWith optimization - Optimized time: " + optimizedTime + " ms");
        System.out.println("EndsWith optimization - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
        assertTrue(optimizedTime < standardTime, "Optimized=" + optimizedTime + ", Standard=" + standardTime);
    }

    @Test
    public void testDoubleWildcardOptimization() {
        String pattern = ".*.*";
        String testString = "anything";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                optimizedPredicate.test(testString);
            }
        });

        long standardTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                standardPredicate.test(testString);
            }
        });

        System.out.println("Double wildcard optimization - Optimized time: " + optimizedTime + " ms");
        System.out.println("Double wildcard optimization - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
    }

    @Test
    public void testSpecialCharacterPattern() {
        String pattern = ".*\\d+.*";
        String testString = "number123";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                optimizedPredicate.test(testString);
            }
        });

        long standardTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                standardPredicate.test(testString);
            }
        });

        System.out.println("Special character pattern - Optimized time: " + optimizedTime + " ms");
        System.out.println("Special character pattern - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
    }

    @Test
    public void testNoWildcardPattern() {
        String pattern = "exactmatch";
        String testString = "exactmatch";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                optimizedPredicate.test(testString);
            }
        });

        long standardTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                standardPredicate.test(testString);
            }
        });

        System.out.println("No wildcard pattern - Optimized time: " + optimizedTime + " ms");
        System.out.println("No wildcard pattern - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
    }

    @Test
    public void testComplexRegexPattern() {
        String pattern = ".*[a-z]{3}\\d+.*";
        String testString = "abc123";

        Predicate<String> optimizedPredicate = RegexCache.buildPredicate(pattern);
        Predicate<String> standardPredicate = Pattern.compile(pattern).asMatchPredicate();

        long optimizedTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                optimizedPredicate.test(testString);
            }
        });

        long standardTime = measureTime(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                standardPredicate.test(testString);
            }
        });

        System.out.println("Complex regex pattern - Optimized time: " + optimizedTime + " ms");
        System.out.println("Complex regex pattern - Standard time: " + standardTime + " ms");

        assertEquals(optimizedPredicate.test(testString), standardPredicate.test(testString));
    }

}
