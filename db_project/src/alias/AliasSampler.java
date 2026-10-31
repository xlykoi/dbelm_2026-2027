package alias;

import common.WeightedSampler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AliasSampler implements WeightedSampler {

    private final List<String> items = new ArrayList<>();
    private final List<Integer> weights = new ArrayList<>();

    private double boxCapacity;
    private double[] ownWeight;
    private int[] partner;

    public AliasSampler(Map<String, Integer> weightByItem) {
        weightByItem.forEach((item, weight) -> {
            items.add(item);
            weights.add(weight);
        });
        rebuildBoxes();
    }

    @Override
    public void insert(String item, int weight) {
        items.add(item);
        weights.add(weight);
        rebuildBoxes();
    }

    @Override
    public void delete(String item) {
        int index = items.indexOf(item);
        items.remove(index);
        weights.remove(index);
        rebuildBoxes();
    }

    private void rebuildBoxes() {
        if (items.isEmpty()) {
            boxCapacity = 0;
            ownWeight = new double[0];
            partner = new int[0];
            return;
        }
        boxCapacity = computeBoxCapacity();
        fillBoxesUpToCapacity();
    }

    private double computeBoxCapacity() {
        long totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        return (double) totalWeight / items.size();
    }

    private void fillBoxesUpToCapacity() {
        int boxCount = items.size();
        ownWeight = new double[boxCount];
        partner = new int[boxCount];
        double[] remaining = new double[boxCount];
        Deque<Integer> underfilled = new ArrayDeque<>();
        Deque<Integer> overfilled = new ArrayDeque<>();

        for (int box = 0; box < boxCount; box++) {
            remaining[box] = weights.get(box);
            putIntoUnderfilledOrOverfilled(box, remaining, underfilled, overfilled);
        }

        while (!underfilled.isEmpty() && !overfilled.isEmpty()) {
            int small = underfilled.pop();
            int large = overfilled.pop();
            ownWeight[small] = remaining[small];
            partner[small] = large;
            remaining[large] -= boxCapacity - remaining[small];
            putIntoUnderfilledOrOverfilled(large, remaining, underfilled, overfilled);
        }

        for (int full : underfilled) {
            ownWeight[full] = boxCapacity;
        }
        for (int full : overfilled) {
            ownWeight[full] = boxCapacity;
        }
    }

    private void putIntoUnderfilledOrOverfilled(int box, double[] remaining, Deque<Integer> underfilled,
                                                Deque<Integer> overfilled) {
        if (remaining[box] < boxCapacity) {
            underfilled.push(box);
        } else {
            overfilled.push(box);
        }
    }

    @Override
    public String sample(Random random) {
        int box = random.nextInt(items.size());
        double pointInBox = random.nextDouble() * boxCapacity;
        if (pointInBox < ownWeight[box]) {
            return items.get(box);
        }
        return items.get(partner[box]);
    }
}
