package ca.teamdman.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime) // Measure average execution time
@OutputTimeUnit(TimeUnit.MILLISECONDS) // Use milliseconds as time unit
@State(Scope.Thread) // One instance per thread
public class SampleBenchmark {

    private int[] array;

    @Setup(Level.Iteration)
    public void setup() {
        array = new int[1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
    }

    @Benchmark
    public int sumArray() {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    public static void main(String[] args) throws RunnerException {
//        org.openjdk.jmh.Main.main(args);
        Options options = new OptionsBuilder()
                .include(SampleBenchmark.class.getSimpleName())
//                .include(".*")
                .forks(1)
                .shouldDoGC(false)
                .build();
        new Runner(options).run();
    }
}

