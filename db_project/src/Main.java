import alias.AliasSampler;
import binarysearchtree.BinarySearchTreeSampler;
import bucketsampling.BucketSampler;
import common.WeightedSampler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class Main {

    private static final int SAMPLE_COUNT = 1_000_000;
    private static final int[] ITEM_COUNTS = {1_000, 10_000, 100_000, 1_000_000};
    private static final int MAX_WEIGHT = 1_000;
    private static final long SPEED_TEST_SEED = 42;
    private static final int MEASUREMENT_ROUNDS = 5;
    private static final long MIN_NANOS_PER_MEASUREMENT = 100_000_000;

    public static void main(String[] args) {
        Random random = new Random(42);

        showThatSamplingFollowsWeights("Alias", AliasSampler::new, random);
        showThatSamplingFollowsWeights("Bináris keresőfa", BinarySearchTreeSampler::new, random);
        showThatSamplingFollowsWeights("Vödrös (BUS)", BucketSampler::new, random);

        measureSpeed("Alias", AliasSampler::new);
        measureSpeed("Bináris keresőfa", BinarySearchTreeSampler::new);
        measureSpeed("Vödrös (BUS)", BucketSampler::new);
    }

    private static void showThatSamplingFollowsWeights(String algorithmName,
                                                       Function<Map<String, Integer>, WeightedSampler> createSampler,
                                                       Random random) {
        System.out.println("=== " + algorithmName + ": a sorsolás követi-e a súlyokat ===");
        Map<String, Integer> weightByItem = new LinkedHashMap<>();
        weightByItem.put("A", 1);
        weightByItem.put("B", 2);
        weightByItem.put("C", 3);
        weightByItem.put("D", 4);
        weightByItem.put("E", 10);

        WeightedSampler sampler = createSampler.apply(weightByItem);
        printExpectedAndObservedShares(sampler, weightByItem, random);

        System.out.println("E törlése, F beszúrása 5-ös súllyal:");
        sampler.delete("E");
        weightByItem.remove("E");
        sampler.insert("F", 5);
        weightByItem.put("F", 5);
        printExpectedAndObservedShares(sampler, weightByItem, random);
    }

    private static void printExpectedAndObservedShares(WeightedSampler sampler, Map<String, Integer> weightByItem,
                                                       Random random) {
        Map<String, Integer> countByItem = new LinkedHashMap<>();
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            countByItem.merge(sampler.sample(random), 1, Integer::sum);
        }
        int totalWeight = weightByItem.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("elem  súly   várt     kapott");
        weightByItem.forEach((item, weight) -> System.out.printf("%-4s %5d %6.2f%% %8.2f%%%n", item, weight,
                100.0 * weight / totalWeight, 100.0 * countByItem.getOrDefault(item, 0) / SAMPLE_COUNT));
        System.out.println();
    }

    private static void measureSpeed(String algorithmName,
                                     Function<Map<String, Integer>, WeightedSampler> createSampler) {
        System.out.println("=== " + algorithmName + ": sebesség (" + MEASUREMENT_ROUNDS + " mérés mediánja) ===");
        System.out.println("        n     1 sorsolás     1 insert + 1 delete");
        for (int itemCount : ITEM_COUNTS) {
            Random random = new Random(SPEED_TEST_SEED);
            WeightedSampler sampler = createSampler.apply(randomWeights(itemCount, random));
            Runnable sampleOnce = () -> sampler.sample(random);
            Runnable insertAndDeleteOnce = () -> insertAndDeleteOneItem(sampler, random);
            warmUpBeforeMeasuring(sampleOnce, insertAndDeleteOnce);
            System.out.printf("%,9d %12.0f ns %,20.0f ns%n", itemCount,
                    medianNanosPerOperation(sampleOnce), medianNanosPerOperation(insertAndDeleteOnce));
        }
        System.out.println();
    }

    private static Map<String, Integer> randomWeights(int itemCount, Random random) {
        Map<String, Integer> weightByItem = new LinkedHashMap<>();
        for (int i = 0; i < itemCount; i++) {
            weightByItem.put("item" + i, random.nextInt(1, MAX_WEIGHT + 1));
        }
        return weightByItem;
    }

    private static void insertAndDeleteOneItem(WeightedSampler sampler, Random random) {
        sampler.insert("new", random.nextInt(1, MAX_WEIGHT + 1));
        sampler.delete("new");
    }

    private static void warmUpBeforeMeasuring(Runnable sampleOnce, Runnable insertAndDeleteOnce) {
        nanosPerOperation(sampleOnce);
        nanosPerOperation(insertAndDeleteOnce);
    }

    private static double medianNanosPerOperation(Runnable operation) {
        double[] results = new double[MEASUREMENT_ROUNDS];
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            results[round] = nanosPerOperation(operation);
        }
        Arrays.sort(results);
        return results[MEASUREMENT_ROUNDS / 2];
    }

    private static double nanosPerOperation(Runnable operation) {
        for (int repetitions = 1; ; repetitions *= 2) {
            long start = System.nanoTime();
            for (int i = 0; i < repetitions; i++) {
                operation.run();
            }
            long elapsed = System.nanoTime() - start;
            if (elapsed >= MIN_NANOS_PER_MEASUREMENT) {
                return (double) elapsed / repetitions;
            }
        }
    }
}
